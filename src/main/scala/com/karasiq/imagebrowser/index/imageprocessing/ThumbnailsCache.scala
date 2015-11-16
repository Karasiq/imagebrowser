package com.karasiq.imagebrowser.index.imageprocessing

import java.nio.file.Path

trait ThumbnailsCache {
  def getOrCreateThumbnail(image: Path, size: Int): Array[Byte]
  def evictThumbnail(image: Path): Unit
}


