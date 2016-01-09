package com.karasiq.imagebrowser.app

import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.karasiq.imagebrowser.DefaultServicesProvider
import com.karasiq.imagebrowser.service.DefaultImageBrowserServiceProvider
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.IOUtils
import spray.can.Http

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object ImageBrowserBoot extends App with ImageBrowserActorSystem.Provider with ImageBrowserMapDb.Provider with ImageBrowserThumbCache.Provider with DefaultServicesProvider with DefaultImageBrowserServiceProvider {
  import actorSystem.{dispatcher, log, scheduler}

  def startup(): Unit = {
    val config = ConfigFactory.load().getConfig("imageBrowser")

    implicit val timeout = Timeout(20 seconds)

    IO(Http) ? Http.Bind(imageBrowserService, interface = config.getString("host"), port = config.getInt("port"))

    scheduler.scheduleOnce(5 seconds) {
      log.info("Rescanning all tracked directories")
      directoryManager.rescan()
      log.info("Scan finished")
    }
  }

  def shutdown(): Unit = {
    log.info("Shutting down ImageBrowser")

    actorSystem.registerOnTermination {
      IOUtils.closeQuietly(indexRegistryDb)
      IOUtils.closeQuietly(thumbnailsCacheDb)
    }
    Await.result(actorSystem.terminate(), 3 minutes)
  }

  // Schedule shutdown
  Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
    override def run(): Unit = shutdown()
  }))

  // Start program
  startup()
}
