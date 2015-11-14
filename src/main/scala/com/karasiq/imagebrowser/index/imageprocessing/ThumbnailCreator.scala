package com.karasiq.imagebrowser.index.imageprocessing

import java.nio.file.Path

trait ThumbnailCreator {
  def createThumbnail(image: Path, size: Int): Array[Byte]
}

