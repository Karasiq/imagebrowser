package com.karasiq.imagebrowser.providers

import akka.actor.ActorRef

trait ImageBrowserServiceProvider {
  /**
   * Image share web service
   */
  def imageBrowserService: ActorRef
}
