package com.karasiq.imagebrowser.providers

import akka.actor.ActorRef

trait FileWatcherServiceProvider {
  /**
   * File watcher service
   */
  def fileWatcherService: ActorRef
}
