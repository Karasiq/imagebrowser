package com.karasiq.imagebrowser.providers

import com.karasiq.imagebrowser.index.imageprocessing.ThumbnailCreator

trait ThumbnailCreatorProvider {
  /**
   * Image thumbnail creator
   */
  def thumbnailCreator: ThumbnailCreator
}
