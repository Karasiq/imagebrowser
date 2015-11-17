package com.karasiq.imagebrowser.providers

import java.io.IOException
import java.nio.file.Paths

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, AllForOneStrategy, SupervisorStrategy}
import com.karasiq.fileutils.watcher.{RegisterFile, UnregisterFile}
import com.karasiq.imagebrowser.dirmanager.{RemoveDirectory, ScanDirectory}
import com.karasiq.imagebrowser.index.filewatcher.IndexRegistryChange

trait DirectoryManagerProvider { self: IndexRegistryProvider with FileWatcherServiceProvider ⇒
  /**
   * Tracked directories manager
   */
  def directoryManager: DirectoryManager

  abstract class DirectoryManager {
    def trackedDirectories: Set[String]

    final def rescan(): Unit = {
      trackedDirectories.foreach(dir ⇒ addDirectory(dir, false))
    }

    def addDirectory(dir: String, readMetadata: Boolean): Unit = {
      fileWatcherService ! RegisterFile(dir, true)
      indexRegistry.scanDirectory(Paths.get(dir), readMetadata)
    }

    def removeDirectory(dir: String): Unit = {
      indexRegistry.removeDirectory(Paths.get(dir))
      fileWatcherService ! UnregisterFile(dir)
    }
  }

  final class DirectoryManagerActor extends Actor with ActorLogging {
    override def receive: Actor.Receive = {
      case ScanDirectory(path, readMetadata) ⇒
        log.info("Scanning directory: {}", path)
        directoryManager.addDirectory(path, readMetadata)
        log.info("Scan finished")
        context.system.eventStream.publish(IndexRegistryChange(path))

      case RemoveDirectory(path) ⇒
        directoryManager.removeDirectory(path)
        log.info("Directory removed: {}", path)
        context.system.eventStream.publish(IndexRegistryChange(path))
    }

    override def supervisorStrategy: SupervisorStrategy = AllForOneStrategy() {
      case e: IOException ⇒ Resume
    }
  }
}
