package com.karasiq.imagebrowser.index

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant

import com.karasiq.fileutils.PathUtils._
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FilenameUtils

import scala.collection.GenTraversableOnce

abstract class ImageCursor {
  def path: Path
  def thumbnail: Array[Byte]
  def lastModified: Instant
  def metadata: Option[ImageMetadata]

  override def toString: String = path.toString

  override def equals(obj: scala.Any): Boolean = obj match {
    case ic: ImageCursor ⇒
      path.equals(ic.path)
    case _ ⇒
      false
  }

  override def hashCode(): Int = path.hashCode
}

abstract class DirectoryCursor {
  def path: Path
  def lastModified: Instant
  def images: GenTraversableOnce[ImageCursor]
  def subDirs: GenTraversableOnce[DirectoryCursor]

  override def toString: String = path.toString

  override def equals(obj: scala.Any): Boolean = obj match {
    case dc: DirectoryCursor ⇒
      path.equals(dc.path)
    case _ ⇒
      false
  }

  override def hashCode(): Int = path.hashCode
}

private[index] object IndexRegistry {
  // Configured formats
  val allowedExtensions: Set[String] = {
    import scala.collection.JavaConversions._
    val config = ConfigFactory.load().getConfig("imageBrowser")
    config.getStringList("image-formats").toSet ++ config.getStringList("video-formats").toSet
  }
}

abstract class IndexRegistry {
  def needIndexing(file: Path): Boolean = {
    IndexRegistry.allowedExtensions.contains(FilenameUtils.getExtension(file.getFileName.toString))
  }

  def images: GenTraversableOnce[ImageCursor]
  def directories: GenTraversableOnce[DirectoryCursor]

  def putImage(image: Path, readMetadata: Boolean = false): Unit
  def removeImage(image: Path): Unit
  def getImage(image: Path): Option[ImageCursor]

  def putDirectory(directory: Path, readMetadata: Boolean = false): Unit
  def removeDirectory(directory: Path): Unit
  def getDirectory(directory: Path): Option[DirectoryCursor]

  def hasImage(image: Path) = image.ne(null) && getImage(image).nonEmpty

  def hasDirectory(directory: Path) = directory.ne(null) && getDirectory(directory).nonEmpty

  def scanDirectory(directory: Path, readMetadata: Boolean = false): Unit = {
    Files.walkFileTree(directory, new SimpleFileVisitor[Path] {
      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
        putDirectory(dir, readMetadata)
        FileVisitResult.CONTINUE
      }
    })
  }

  def clean(): Unit = {
    directories.foreach { dir ⇒
      if (!dir.path.exists)
        removeDirectory(dir.path)
    }

    images.foreach { img ⇒
      if (!img.path.exists)
        removeImage(img.path)
    }
  }
}

