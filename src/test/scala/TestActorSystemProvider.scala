import akka.actor.ActorSystem
import com.karasiq.imagebrowser.providers.ActorSystemProvider
import org.scalatest.{BeforeAndAfterAll, Suite}

trait TestActorSystemProvider extends ActorSystemProvider with BeforeAndAfterAll { self: Suite ⇒
  override final val actorSystem: ActorSystem = ActorSystem("image-browser-test")

  abstract override protected def afterAll(): Unit = {
    actorSystem.shutdown()
    actorSystem.awaitTermination()
    super.afterAll()
  }
}
