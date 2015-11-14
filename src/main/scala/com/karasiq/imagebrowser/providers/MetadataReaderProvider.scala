package com.karasiq.imagebrowser.providers

import com.karasiq.imagebrowser.index.imageprocessing.MetadataReader

trait MetadataReaderProvider {
  /**
   * Image metadata reader
   */
  def metadataReader: MetadataReader
}
