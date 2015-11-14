package com.karasiq.imagebrowser.index.imageprocessing

import java.nio.file.Path

import com.karasiq.imagebrowser.index.ImageMetadata

trait MetadataReader {
  def readMetadata(image: Path): Option[ImageMetadata]
}
