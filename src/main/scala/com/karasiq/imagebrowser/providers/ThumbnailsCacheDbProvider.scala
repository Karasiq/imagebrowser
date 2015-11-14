package com.karasiq.imagebrowser.providers

import com.karasiq.mapdb.MapDbFile

trait ThumbnailsCacheDbProvider {
  /**
   * Thumbnails cache database
   */
  def thumbnailsCacheDb: MapDbFile
}
