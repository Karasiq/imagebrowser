package com.karasiq.imagebrowser.providers

import com.karasiq.imagebrowser.index.imageprocessing.ThumbnailsCache

trait ThumbnailsCacheProvider {
  /**
   * Image thumbnails cache
   */
  def thumbnailsCache: ThumbnailsCache
}
