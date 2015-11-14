package com.karasiq.imagebrowser.app

import java.nio.file.{Path, Paths}

import com.karasiq.imagebrowser.providers.IndexRegistryDbProvider
import com.karasiq.mapdb.{MapDbFile, MapDbSingleFileProducer}
import com.typesafe.config.ConfigFactory
import org.mapdb.DBMaker.Maker

object ImageBrowserMapDb {
  private val config = ConfigFactory.load().getConfig("imageBrowser")

  def initialSize(): Long = config.getLong("db-initial-size") * 1024 * 1024

  @inline
  private def mapDbFilePath: Path = Paths.get(config.getString("db"))

  private object IndexRegistryDbProducer extends MapDbSingleFileProducer(mapDbFilePath) {
    override protected def setSettings(dbMaker: Maker): Maker = {
      dbMaker
        .transactionDisable()
        .allocateStartSize(initialSize())
        .executorEnable()
        .asyncWriteEnable()
        .asyncWriteFlushDelay(3000)
        .cacheSoftRefEnable()
    }
  }

  trait Provider extends IndexRegistryDbProvider {
    override final val indexRegistryDb: MapDbFile = IndexRegistryDbProducer()
  }
}
