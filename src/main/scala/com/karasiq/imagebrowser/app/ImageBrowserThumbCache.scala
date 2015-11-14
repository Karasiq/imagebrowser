package com.karasiq.imagebrowser.app

import java.nio.file.{Path, Paths}

import com.karasiq.imagebrowser.providers.ThumbnailsCacheDbProvider
import com.karasiq.mapdb.{MapDbFile, MapDbSingleFileProducer}
import com.typesafe.config.ConfigFactory
import org.mapdb.DBMaker.Maker

object ImageBrowserThumbCache {
  private def thumbsDbPath: Path = Paths.get(ConfigFactory.load().getString("imageBrowser.thumbnails-db"))

  private object ThumbCacheFile extends MapDbSingleFileProducer(thumbsDbPath) {
    override protected def setSettings(dbMaker: Maker): Maker = {
      dbMaker
        .transactionDisable()
        .allocateStartSize(ImageBrowserMapDb.initialSize())
        .executorEnable()
        .asyncWriteEnable()
        .asyncWriteFlushDelay(3000)
        .cacheSoftRefEnable()
    }
  }


  trait Provider extends ThumbnailsCacheDbProvider {
    /**
     * Thumbnails cache database
     */
    override final val thumbnailsCacheDb: MapDbFile = ThumbCacheFile()
  }
}