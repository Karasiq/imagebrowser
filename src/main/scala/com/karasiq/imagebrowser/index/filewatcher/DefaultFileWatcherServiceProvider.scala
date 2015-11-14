package com.karasiq.imagebrowser.index.filewatcher

import java.nio.file.{Files, Path}

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

  override final val fileWatcherService: ActorRef = actorSystem.actorOf(Props(new IndexRegistryFileWatcherService(fileSystemEventBus)), "fileWatcherService")

  final val indexRegistryWatcher: ActorRef = actorSystem.actorOf(Props(new IndexRegistryWatcher), "indexRegistryWatcher")

  private final class IndexRegistryFileWatcherService(eventBus: FileWatcherService.FsEventBus) extends FileWatcherService(eventBus) {
    override protected def pollInterval(): FiniteDuration = {
      ConfigFactory.load().getInt("fileWatcherService.interval").seconds
    }
  }

  private final class IndexRegistryWatcher extends FileWatcherServiceWrapper(fileSystemEventBus) with ActorLogging {
    private val rescanQueue = new mutable.Queue[Path]()

    private implicit def ec: ExecutionContext = actorSystem.dispatcher

    // Scheduling updates
    private val rescanTask = actorSystem.scheduler.schedule(10 seconds, 10 seconds) {
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

      // Only rescan directories
      if (Files.isDirectory(file))
        scheduleRescan(file)
    }

    override def onDelete(event: WatchedFileEvent): Unit = {
      val path = event.absolutePath

      rescanQueue.dequeueAll(_ == path)

      if (indexRegistry.hasDirectory(path))
        indexRegistry.removeDirectory(path)
      else if (indexRegistry.hasImage(path))
        indexRegistry.removeImage(path)

      actorSystem.eventStream.publish(IndexRegistryChange(event.path))
    }

    override def onCreate(event: WatchedFileEvent): Unit = {
      val file = event.absolutePath

      // Only register directories
      if (Files.isDirectory(file)) {
        fileWatcherService ! RegisterFile(file.toString)
        scheduleRescan(file)
      }

      scheduleRescan(file.getParent)
    }

    @throws[Exception](classOf[Exception]) override
    def postStop(): Unit = {
      rescanTask.cancel()
      super.postStop()
    }
  }
}