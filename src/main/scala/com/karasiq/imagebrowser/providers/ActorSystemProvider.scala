package com.karasiq.imagebrowser.providers

import akka.actor.ActorSystem

trait ActorSystemProvider {
  /**
   * Application primary actor system
   */
  def actorSystem: ActorSystem
}
