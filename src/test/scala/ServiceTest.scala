import java.nio.file.Paths

import akka.actor.ActorRefFactory
import akka.event.LoggingAdapter
import com.karasiq.imagebrowser.DefaultServicesProvider
import com.karasiq.imagebrowser.service.DefaultImageBrowserServiceProvider
import org.scalatest.{FlatSpec, Matchers}
import spray.http.HttpEncodings._
import spray.http.HttpHeaders.`Accept-Encoding`
import spray.http.Uri.Query
import spray.http.{StatusCodes, Uri}
import spray.json.DefaultJsonProtocol._
import spray.json._
import spray.testkit.ScalatestRouteTest

import scala.concurrent.ExecutionContext

class ServiceTest extends FlatSpec with ScalatestRouteTest with Matchers with TestMapDbProvider with TestActorSystemProvider with DefaultServicesProvider with DefaultImageBrowserServiceProvider {
  val service = new ImageBrowserService {
    override implicit def executionContext: ExecutionContext = actorRefFactory.dispatcher

    override val log: LoggingAdapter = actorSystem.log

    override implicit def actorRefFactory: ActorRefFactory = actorSystem
  }

  val route = service.route()

  val testDir = Paths.get(System.getProperty("java.io.tmpdir")).normalize().toString

  "Http service" should "add directory" in {
    Put(Uri("/directory").copy(query = Query("path" → testDir, "readMetadata" → "false"))) ~> route ~> check {
      status should be (StatusCodes.OK)
      Thread.sleep(1000) // Wait for scan
    }
  }

  it should "get root directories" in {
    Get("/directories") ~> `Accept-Encoding`(identity) ~> route ~> check {
      val response = responseAs[String]
      response.parseJson.convertTo[Vector[String]] should contain(testDir)
    }
  }

  it should "get all directories" in {
    Get("/directories/all") ~> `Accept-Encoding`(identity) ~> route ~> check {
      val response = responseAs[String]
      response.parseJson.convertTo[Vector[String]] should contain(testDir)
    }
  }

  it should "get directory" in {
    Get(Uri("/directory").copy(query = Query("path" → testDir))) ~> `Accept-Encoding`(identity) ~> route ~> check {
      val response = responseAs[String]
      response.parseJson.asJsObject.getFields("path") match {
        case Seq(JsString(path)) ⇒
          path should be(testDir)
      }
    }
  }

  it should "remove directory" in {
    Delete(Uri("/directory").copy(query = Query("path" → testDir))) ~> route ~> check {
      status should be (StatusCodes.OK)
    }
  }

  it should "respond with 404 for images" in {
    Get(Uri("/thumbnail").copy(query = Query("path" → "not existing image"))) ~> route ~> check {
      status should be (StatusCodes.NotFound)
    }

    Get(Uri("/image").copy(query = Query("path" → "not existing image"))) ~> route ~> check {
      status should be (StatusCodes.NotFound)
    }
  }
}
