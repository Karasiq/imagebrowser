package com.karasiq.imagebrowser.index.mapdb

import java.nio.file.Path

import com.karasiq.imagebrowser.index.imageprocessing.{ThumbnailCreator, ThumbnailsCache}
import com.karasiq.imagebrowser.providers.{ThumbnailCreatorProvider, ThumbnailsCacheDbProvider, ThumbnailsCacheProvider}
import com.karasiq.mapdb.{MapDbWrapper, MapDbFile}
import com.karasiq.mapdb.MapDbWrapper._
import org.mapdb.Serializer

trait MapDbThumbnailsCacheProvider extends ThumbnailsCacheProvider { self: ThumbnailsCacheDbProvider with ThumbnailCreatorProvider ⇒
  override final val thumbnailsCache: ThumbnailsCache = new MapDbThumbnailsCache(thumbnailsCacheDb, thumbnailCreator)

  private final class MapDbThumbnailsCache(db: MapDbFile, impl: ThumbnailCreator) extends ThumbnailsCache {
    private val thumbnails: MapDbTreeMap[String, Array[Byte]] = MapDbWrapper(db).createTreeMap("thumbs") { _
      .keySerializer(Serializer.STRING_XXHASH)
      .valueSerializer(Serializer.BYTE_ARRAY)
      .nodeSize(32)
      .valuesOutsideNodesEnable()
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
}