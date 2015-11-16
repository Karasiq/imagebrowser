package com.karasiq.imagebrowser.index.imageprocessing

import java.nio.charset.Charset
import java.nio.file.Path

import com.karasiq.imagebrowser.index.imageprocessing.imageio.ImageIOThumbnailCreator
import com.karasiq.imagebrowser.index.imageprocessing.javacv.{JavaCVVideoThumbnailCreator, JavaCVThumbnailCreator}
import com.karasiq.imagebrowser.providers.ThumbnailCreatorProvider
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FilenameUtils

trait DefaultThumbnailCreatorProvider extends ThumbnailCreatorProvider {
  override final val thumbnailCreator: ThumbnailCreator = new DefaultThumbnailCreator

  private final class DefaultThumbnailCreator extends ThumbnailCreator {
    private val (imageFormats, videoFormats) = {
      import scala.collection.JavaConversions._
      val config = ConfigFactory.load().getConfig("imageBrowser")
      (config.getStringList("image-formats").toSet, config.getStringList("video-formats").toSet)
    }

    private val (forImage, forVideo) = {
      (new JavaCVThumbnailCreator(), new JavaCVVideoThumbnailCreator())
    }

    private val fallback = new ImageIOThumbnailCreator

    private val ascii = Charset.forName("ASCII").newEncoder()

    @inline
    private def isAscii(s: String): Boolean = {
      ascii.canEncode(s)
    }

    @inline
    private def implFor(image: Path): ThumbnailCreator = {
      val ext = FilenameUtils.getExtension(image.getFileName.toString)
      if (imageFormats.contains(ext)) {
        if (ext == "gif" || !isAscii(image.toAbsolutePath.toString)) { // Cannot be processed with OpenCV
          fallback
        } else {
          forImage
        }
      } else if (videoFormats.contains(ext)) {
        forVideo
      } else {
        throw new IllegalArgumentException("Unknown media format: " + ext)
      }
    }

    override def createThumbnail(image: Path, size: Int): Array[Byte] = {
      implFor(image).createThumbnail(image, size)
    }
  }
}
