package com.karasiq.imagebrowser.providers

import com.karasiq.mapdb.MapDbFile

trait IndexRegistryDbProvider {
  /**
   * Main application database
   */
  def indexRegistryDb: MapDbFile
}
