package com.karasiq.imagebrowser.index.imageprocessing.cache

import java.nio.file.Path

import com.karasiq.imagebrowser.providers.ThumbnailCreatorProvider

import scala.concurrent.duration._
import scala.collection.JavaConversions._

trait ThumbnailsCache {
  def getOrCreateThumbnail(image: Path, size: Int): Array[Byte]
  def evictThumbnail(image: Path): Unit
}


