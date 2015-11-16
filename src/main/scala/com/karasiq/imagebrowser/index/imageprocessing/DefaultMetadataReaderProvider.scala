package com.karasiq.imagebrowser.index.imageprocessing

import java.nio.file.Path

import com.drew.imaging.{ImageMetadataReader, ImageProcessingException}
import com.drew.metadata.Metadata
import com.drew.metadata.iptc.IptcDirectory
import com.karasiq.imagebrowser.index.ImageMetadata
import com.karasiq.imagebrowser.providers.MetadataReaderProvider
import org.apache.commons.io.FilenameUtils

import scala.collection.JavaConversions._
import scala.util.control.Exception._

final class DefaultMetadataReader extends MetadataReader {
  private def convert(mt: Metadata): ImageMetadata = {
    val data = Option(mt.getFirstDirectoryOfType(classOf[IptcDirectory])).map { tags ⇒
      tags.getTags.map(tag ⇒ tag.getTagName → tag.getDescription)
    }

    data.filter(_.nonEmpty).fold(ImageMetadata.empty)(data ⇒ ImageMetadata(data.toMap))
  }

  private val imageFormats = Set("png", "bmp", "jpg", "jpeg", "gif")

  def readMetadata(image: Path): Option[ImageMetadata] = {
    if (imageFormats.contains(FilenameUtils.getExtension(image.getFileName.toString))) {
      catching(classOf[ImageProcessingException])
        .opt(convert(ImageMetadataReader.readMetadata(image.toFile)))
        .filter(_.metadata.nonEmpty)
    } else {
      None
    }
  }
}

trait DefaultMetadataReaderProvider extends MetadataReaderProvider {
  override final val metadataReader: MetadataReader = new DefaultMetadataReader
}


