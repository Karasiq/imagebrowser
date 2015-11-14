package com.karasiq.imagebrowser.dirmanager

import com.karasiq.imagebrowser.index.filewatcher.DefaultFileWatcherServiceProvider
import com.karasiq.imagebrowser.providers.{DirectoryManagerProvider, IndexRegistryProvider, IndexRegistryDbProvider}
import com.karasiq.mapdb.MapDbWrapper
import com.karasiq.mapdb.MapDbWrapper.MapDbHashSet

trait MapDbDirectoryManagerProvider extends DirectoryManagerProvider { self: IndexRegistryDbProvider with IndexRegistryProvider with DefaultFileWatcherServiceProvider ⇒
  override final val directoryManager: DirectoryManager = new DirectoryManager

  final class DirectoryManager extends super.DirectoryManager {
    private val dirs: MapDbHashSet[String] = MapDbWrapper(indexRegistryDb).hashSet("tracked_directories")

    override def trackedDirectories: Set[String] = dirs.toSet

    override def addDirectory(dir: String, readMetadata: Boolean): Unit = {
      dirs.add(dir)
      super.addDirectory(dir, readMetadata)
    }

    override def removeDirectory(dir: String): Unit = {
      assert(dirs.contains(dir), "Directory isn't tracked")
      dirs.remove(dir)
      super.removeDirectory(dir)
    }
  }
}
