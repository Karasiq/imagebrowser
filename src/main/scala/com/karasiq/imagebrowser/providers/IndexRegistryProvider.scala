package com.karasiq.imagebrowser.providers

import com.karasiq.imagebrowser.index.IndexRegistry

trait IndexRegistryProvider {
  /**
   * File index registry
   */
  def indexRegistry: IndexRegistry
}
