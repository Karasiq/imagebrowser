package com.karasiq.imagebrowser.index

import scala.language.implicitConversions

object ImageMetadata {
  implicit def imageMetadataValueGet(mt: ImageMetadata): Map[String, String] = mt.metadata

  val empty: ImageMetadata = ImageMetadata(Map.empty)
}

final case class ImageMetadata(metadata: Map[String, String]) {
  def apply() = metadata
}