package com.karasiq.imagebrowser.index.filewatcher

import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit

import akka.actor.{ActorLogging, ActorRef, Props}
import com.karasiq.fileutils.watcher.FileWatcherService.FsEventBus
import com.karasiq.fileutils.watcher.{FileWatcherService, FileWatcherServiceWrapper, RegisterFile, WatchedFileEvent}
import com.karasiq.imagebrowser.providers._
import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

final case class IndexRegistryChange(path: String)

trait DefaultFileWatcherServiceProvider extends FileWatcherServiceProvider { self: ActorSystemProvider with IndexRegistryProvider ⇒
  final val fileSystemEventBus: FsEventBus = new FsEventBus

  override final val fileWatcherService: ActorRef = actorSystem.actorOf(Props(new FileWatcherService(fileSystemEventBus)), "fileWatcherService")

  final val indexRegistryWatcher: ActorRef = actorSystem.actorOf(Props(new IndexRegistryWatcher), "indexRegistryWatcher")

  private final class IndexRegistryWatcher extends FileWatcherServiceWrapper(fileSystemEventBus) with ActorLogging {
    private val rescanQueue = new mutable.Queue[Path]()

    private implicit def ec: ExecutionContext = actorSystem.dispatcher

    private val rescanTaskInterval: FiniteDuration = {
      val millis = ConfigFactory.load().getDuration("imageBrowser.rescan-queue-interval", TimeUnit.MILLISECONDS)
      FiniteDuration(millis, TimeUnit.MILLISECONDS)
    }

    // Scheduling updates
    private val rescanTask = actorSystem.scheduler.schedule(rescanTaskInterval, rescanTaskInterval) {
      rescanQueue.dequeueAll(_ ⇒ true).foreach { dir ⇒
        log.debug("Rescan started: {}", dir)
        indexRegistry.putDirectory(dir)
        actorSystem.eventStream.publish(IndexRegistryChange(dir.toString))
      }
    }

    def scheduleRescan(dir: Path): Unit = {
      if (!rescanQueue.contains(dir)) {
        rescanQueue.enqueue(dir)
        log.debug("Rescan scheduled: {}", dir)
      }
    }

    override def onModify(event: WatchedFileEvent): Unit = {
      val file = event.absolutePath

      if (Files.isDirectory(file)) {
        scheduleRescan(file)
      } else {
        scheduleRescan(file.getParent)
      }
    }

    override def onDelete(event: WatchedFileEvent): Unit = {
      val path = event.absolutePath

      path match {
        case dir if indexRegistry.hasDirectory(dir) ⇒
          rescanQueue.dequeueAll(_ == path)
          indexRegistry.removeDirectory(path)
          actorSystem.eventStream.publish(IndexRegistryChange(dir.getParent.toString))

        case img if indexRegistry.hasImage(img) ⇒
          indexRegistry.removeImage(img)
          actorSystem.eventStream.publish(IndexRegistryChange(img.getParent.toString))

        case _ ⇒
          // Pass
      }
    }

    override def onCreate(event: WatchedFileEvent): Unit = {
      val file = event.absolutePath

      if (Files.isDirectory(file)) {
        fileWatcherService ! RegisterFile(file.toString)
      } else {
        scheduleRescan(file.getParent)
      }
    }

    @throws[Exception](classOf[Exception]) override
    def postStop(): Unit = {
      rescanTask.cancel()
      super.postStop()
    }
  }
}