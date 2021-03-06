package com.karasiq.imagebrowser.index.mapdb

import java.io.IOException
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant

import com.karasiq.common.Lazy
import com.karasiq.fileutils.PathUtils._
import com.karasiq.fileutils.pathtree.PathTreeUtils._
import com.karasiq.imagebrowser.index._
import com.karasiq.imagebrowser.providers.{IndexRegistryDbProvider, IndexRegistryProvider, MetadataReaderProvider, ThumbnailsCacheProvider}
import com.karasiq.mapdb.MapDbWrapper
import com.karasiq.mapdb.MapDbWrapper.{MapDbHashMap, MapDbTreeMap}
import org.mapdb.Serializer

import scala.collection.GenTraversableOnce
import scala.collection.JavaConversions._

trait MapDbIndexRegistryProvider extends IndexRegistryProvider { self: IndexRegistryDbProvider with MetadataReaderProvider with ThumbnailsCacheProvider ⇒
  import Serializers._

  override final val indexRegistry: IndexRegistry = new MapDbIndexRegistry

  private final class MapDbIndexRegistry extends IndexRegistry {
    // File visitor object
    private final class DirectoryScanner(readMetadata: Boolean) extends SimpleFileVisitor[Path] {
      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
        if (!getDirectory(dir).exists(_.lastModified.toEpochMilli == attrs.lastModifiedTime().toMillis)) {
          putDirectory(dir, readMetadata)
        }
        FileVisitResult.CONTINUE
      }

      override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = {
        FileVisitResult.CONTINUE
      }
    }

    private final class WrappedImageEntry(image: Path, info: IndexEntry) extends ImageCursor {
      require(!info.isDirectory, "Not image")

      override def lastModified: Instant = info.lastModified

      override def path: Path = image

      override def thumbnail: Array[Byte] = {
        getThumbnail(image)
      }

      private val metadata_ = Lazy(MapDbIndexRegistry.this.metadata.get(asString(path)))

      override def metadata: Option[ImageMetadata] = metadata_()
    }

    private final class WrappedDirectoryEntry(directory: Path, info: IndexEntry) extends DirectoryCursor {
      require(info.isDirectory, "Not directory")

      override def lastModified: Instant = info.lastModified

      override def path: Path = directory

      private val subTree = Lazy {
        fileTree.underlying()
          .subMap(Array(directory.toString), Array(directory.toString, null))
          .toList
      }

      override def images: GenTraversableOnce[ImageCursor] = subTree().collect {
        case StoredIndexEntry(path, entry) if !entry.isDirectory ⇒
          new WrappedImageEntry(path, entry)
      }

      override def subDirs: GenTraversableOnce[DirectoryCursor] = subTree().collect {
        case StoredIndexEntry(path, entry) if entry.isDirectory ⇒
          new WrappedDirectoryEntry(path, entry)
      }
    }

    private def needRescanImage(image: Path): Boolean = {
      val entry = this.getImage(image)
      val changed = !entry.exists(_.lastModified.toEpochMilli == image.lastModified.toMillis)
      // val metadataRescan = readMetadata && entry.exists(_.metadata.isEmpty)
      changed // || metadataRescan
    }

    @inline
    private def getThumbnail(img: Path): Array[Byte] = {
      thumbnailsCache.getOrCreateThumbnail(img, 150)
    }
  
    private val fileTree: MapDbTreeMap[Array[Any], IndexEntry] = MapDbWrapper(indexRegistryDb).createTreeMap("files") { _
        .nodeSize(32)
        .keySerializer(INDEX_KEY)
        .valueSerializer(INDEX_ENTRY)
    }

    private val metadata: MapDbHashMap[String, ImageMetadata] = MapDbWrapper(indexRegistryDb).createHashMap("metadata") { _
        .keySerializer(Serializer.STRING_XXHASH)
        .valueSerializer(INDEX_METADATA)
    }

    override def images: GenTraversableOnce[ImageCursor] = fileTree.iterator.collect {
      case StoredIndexEntry(path, entry) if !entry.isDirectory ⇒
        new WrappedImageEntry(path, entry)
  }

    override def directories: GenTraversableOnce[DirectoryCursor] = fileTree.iterator.collect {
      case StoredIndexEntry(path, entry) if entry.isDirectory ⇒
        new WrappedDirectoryEntry(path, entry)
    }

    override def removeImage(image: Path): Unit =  {
      thumbnailsCache.evictThumbnail(image)
      fileTree -= asIndexKey(image)
    }

    override def putImage(image: Path, readMetadata: Boolean = false): Unit =  {
      if (needRescanImage(image)) {
        thumbnailsCache.evictThumbnail(image)

        // Add file to tree
        fileTree += asIndexKey(image) → IndexEntry(false, image.lastModified.toInstant)

        // Read metadata
        if (readMetadata) metadataReader.readMetadata(image).foreach { data ⇒
          metadata += asString(image) → data
        }
      }
    }

    override def getImage(image: Path): Option[ImageCursor] = {
      fileTree.get(asIndexKey(image))
        .map(new WrappedImageEntry(image, _))
    }

    override def removeDirectory(directory: Path): Unit =  {
      // Remove all sub nodes
      fileTree.underlying()
        .subMap(Array(asString(directory)), Array(null, null))
        .clear()

      // Remove directory entry
      fileTree -= asIndexKey(directory)
    }

    override def putDirectory(directory: Path, readMetadata: Boolean): Unit =  {
      // Remove phantom nodes
      val subTree = fileTree.underlying()
          .subMap(Array(asString(directory)), Array(asString(directory), null))

      val directoryListing = directory.subFilesAndDirs.toStream
      val directoryNames = directoryListing.map(_.getFileName.toString).toSet
      subTree.keysIterator.foreach {
        case key ⇒
          val fileName = key(0).asInstanceOf[String]
          if (!directoryNames.contains(fileName)) {
            // Remove non-existing entry
            val entry = fileTree.get(key)
            val path = directory.resolve(fileName)
            if (entry.exists(_.isDirectory)) {
              removeDirectory(path)
            } else {
              removeImage(path)
            }
          }
      }

      // Put images
      for (image <- directoryListing.filter(_.isRegularFile) if needIndexing(image)) {
        putImage(image, readMetadata)
      }

      // Put directory entry
      fileTree += asIndexKey(directory) → IndexEntry(true, directory.lastModified.toInstant)
    }

    override def getDirectory(directory: Path): Option[DirectoryCursor] = {
      fileTree.get(asIndexKey(directory))
        .map(new WrappedDirectoryEntry(directory, _))
    }

    override def hasImage(image: Path): Boolean = {
      fileTree.contains(asIndexKey(image))
    }

    override def hasDirectory(directory: Path): Boolean = {
      fileTree.contains(asIndexKey(directory))
    }

    override def scanDirectory(directory: Path, readMetadata: Boolean): Unit =  {
      Files.walkFileTree(directory, new DirectoryScanner(readMetadata))
    }

    override def clean(): Unit =  {
      fileTree.keysIterator.foreach { key ⇒
        val path = Paths.get(key(0).asInstanceOf[String], key(1).asInstanceOf[String])
        if (!Files.exists(path)) {
          fileTree -= key
        }
      }
    }
  }
}