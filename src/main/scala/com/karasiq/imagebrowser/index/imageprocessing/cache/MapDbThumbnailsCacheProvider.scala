package com.karasiq.imagebrowser.index.imageprocessing.cache

import java.nio.file.Path

import com.karasiq.imagebrowser.index.imageprocessing.ThumbnailCreator
import com.karasiq.imagebrowser.providers.{ThumbnailCreatorProvider, ThumbnailsCacheDbProvider, ThumbnailsCacheProvider}
import com.karasiq.mapdb.MapDbWrapper.MapDbHashMap
import com.karasiq.mapdb.{MapDbFile, MapDbWrapper}
import org.mapdb.Serializer

final class MapDbThumbnailsCache(db: MapDbFile, impl: ThumbnailCreator) extends ThumbnailsCache {
  private val thumbnails: MapDbHashMap[String, Array[Byte]] = MapDbWrapper(db).createHashMap { _.hashMapCreate("thumbs")
    .expireStoreSize(0.3D)
    .keySerializer(Serializer.STRING_XXHASH)
    .valueSerializer(Serializer.BYTE_ARRAY)
    .makeOrGet()
  }

  override def getOrCreateThumbnail(image: Path, size: Int): Array[Byte] = {
    val key = image.toString

    thumbnails.get(key) match {
      case Some(thumb) ⇒
        thumb

      case _ ⇒
        val thumb = impl.createThumbnail(image, size)
        if (thumb.ne(null) && thumb.length > 0) {
          thumbnails += key → thumb
        }
        thumb
    }
  }

  override def evictThumbnail(image: Path): Unit = {
    thumbnails -= image.toString
  }
}

trait MapDbThumbnailsCacheProvider extends ThumbnailsCacheProvider { self: ThumbnailsCacheDbProvider with ThumbnailCreatorProvider ⇒
  override final val thumbnailsCache: ThumbnailsCache = new MapDbThumbnailsCache(thumbnailsCacheDb, thumbnailCreator)
}