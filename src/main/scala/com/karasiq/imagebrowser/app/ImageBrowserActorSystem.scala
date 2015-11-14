package com.karasiq.imagebrowser.app

import akka.actor.ActorSystem
import com.karasiq.imagebrowser.providers.ActorSystemProvider

object ImageBrowserActorSystem {
  private val actorSystem = ActorSystem("image-browser")

  def apply(): ActorSystem = actorSystem

  trait Provider extends ActorSystemProvider {
    implicit final override val actorSystem = ImageBrowserActorSystem()
  }
}
