package com.karasiq.imagebrowser.service

import java.nio.file.Paths
import java.time.Instant
import java.util.concurrent.Executors

import akka.actor._
import akka.event.LoggingAdapter
import com.karasiq.fileutils.PathUtils._
import com.karasiq.imagebrowser.dirmanager.{RemoveDirectory, ScanDirectory}
import com.karasiq.imagebrowser.index.filewatcher.IndexRegistryChange
import com.karasiq.imagebrowser.index.DirectoryCursor
import com.karasiq.imagebrowser.providers.{ActorSystemProvider, DirectoryManagerProvider, ImageBrowserServiceProvider, IndexRegistryProvider}
import com.typesafe.config.ConfigFactory
import spray.caching._
import spray.http.CacheDirectives.`max-age`
import spray.http.HttpHeaders.{`Cache-Control`, `Last-Modified`}
import spray.http.{CacheDirectives, HttpData, MediaTypes, StatusCodes}
import spray.json._
import spray.httpx.SprayJsonSupport._
import ImageBrowserJsonProtocol._
import spray.routing
import spray.routing.directives.CachingDirectives
import spray.routing.{Directive1, HttpService}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{implicitConversions, postfixOps, reflectiveCalls}
import scala.util.{Try, Success}

trait DefaultImageBrowserServiceProvider extends ImageBrowserServiceProvider { self: ActorSystemProvider with IndexRegistryProvider with DirectoryManagerProvider ⇒
  /**
   * Image share web service
   */
  override final val imageBrowserService: ActorRef = actorSystem.actorOf(Props(new ImageBrowserServiceActor), "imageBrowserService")

  // Http service abstraction
  abstract class ImageBrowserService extends HttpService {
    def log: LoggingAdapter

    implicit def executionContext: ExecutionContext

    protected val thumbnailContext = ExecutionContext.fromExecutorService(Executors.newWorkStealingPool(48))

    private lazy val dirManagerActor = actorRefFactory.actorOf(Props(new DirectoryManagerActor), "dirManager")

    private val directoriesCache = LruCache[Option[DirectoryCursor]](timeToLive = 3 minutes)

    protected def evictCacheStartingWith(dir: String): Unit = {
      log.debug("Evicting cache: {}*", dir)

      @inline
      def evict(cache: Cache[_]): Unit = {
        val keys = cache.keys.collect {
          case key: String if key.startsWith(dir) ⇒ key
        }
        keys.foreach(cache.remove)
      }
      evict(directoriesCache)
    }

    private def listRootDirectories(): routing.Route = {
      extract(_ ⇒ directoryManager.trackedDirectories) { list ⇒
        complete(list)
      }
    }

    private def listAllDirectories(): routing.Route = {
      extract(_ ⇒ indexRegistry.directories.toIterator.map(_.path.toString).toVector) { list ⇒
        complete(list)
      }
    }

    private def getDirectory(dir: String): routing.Route = {
      val future = directoriesCache(dir, () ⇒ Future(indexRegistry.getDirectory(Paths.get(dir).normalize())))
      onComplete(future) {
        case Success(Some(data)) ⇒
          complete(data.toJson.compactPrint)

        case _ ⇒
          complete(StatusCodes.NotFound, "No such directory")
      }
    }

    private def imgCacheControl = {
      `Cache-Control`(CacheDirectives.public, CacheDirectives.`max-age`(60 * 60 * 24))
    }

    private def getImage(image: String): routing.Route = {
      indexRegistry.getImage(Paths.get(image)) match {
        case Some(img) ⇒
          (respondWithHeader(imgCacheControl) & respondWithLastModifiedHeader(img.lastModified.toEpochMilli)) {
            getFromFile(img.path.toFile)
          }

        case _ ⇒
          complete(StatusCodes.NotFound, "No such image")
      }
    }

    private def getThumbnailImage(image: String): routing.Route = {
      indexRegistry.getImage(Paths.get(image)) match {
        case Some(img) ⇒
          Try(img.thumbnail) match {
            case Success(thumb) if thumb.ne(null) && thumb.length > 0 ⇒
              (respondWithMediaType(MediaTypes.`image/jpeg`) & respondWithHeader(imgCacheControl) & respondWithLastModifiedHeader(img.lastModified.toEpochMilli)) {
                complete(HttpData(thumb))
              }

            case _ ⇒
              log.warning("Thumbnail not created for: {}", image)
              getFromResource("nothumbnail.png")
          }

        case None ⇒
          // Image not found
          complete(StatusCodes.NotFound, "No such image")
      }
    }

    private def absolutePathParameter: Directive1[String] = {
      parameter("path").map { path ⇒
        Paths.get(path).normalize().toAbsolutePath.toString
      }
    }

    // Prohibits modification of tracked directories list
    private val readOnly: Boolean = {
      val config = ConfigFactory.load().getConfig("imageBrowser")
      config.getBoolean("readOnly")
    }

    final def route(): routing.Route = {
      get {
        // JSON APIs
        compressResponse() {
          pathPrefix("directories") {
            // All directories
            path("all") {
              listAllDirectories()
            } ~
            // Root directories
            pathEndOrSingleSlash {
              listRootDirectories()
            }
          } ~
          // Directory
          (path("directory") & absolutePathParameter) { path ⇒
            getDirectory(path)
          }
        } ~
        // Thumbnail
        (path("thumbnail") & absolutePathParameter) { path ⇒
          detach(thumbnailContext) {
            getThumbnailImage(path)
          }
        } ~
        // Full image
        (path("image") & absolutePathParameter) { path ⇒
          getImage(path)
        } ~
        // Static files
        compressResponse() {
          pathSingleSlash(getFromResource("webstatic/index.html")) ~
            getFromResourceDirectory("webstatic")
        }
      } ~
      put {
        (path("directory") & absolutePathParameter & parameter("readMetadata".as[Boolean] ? false)) { (path, readMetadata) ⇒
          validate(!readOnly, "Directory list modification is disabled") {
            val dir = Paths.get(path)
            if (dir.exists) {
              dirManagerActor ! ScanDirectory(path, readMetadata)
              complete(StatusCodes.OK)
            } else complete(StatusCodes.BadRequest, "Directory not exists")
          }
        }
      } ~
      delete {
        (path("directory") & absolutePathParameter) { path ⇒
          validate(!readOnly, "Directory list modification is disabled") {
            if (directoryManager.trackedDirectories.contains(path)) {
              dirManagerActor ! RemoveDirectory(path)
              complete(StatusCodes.OK)
            } else complete(StatusCodes.BadRequest, "Invalid directory")
          }
        }
      }
    }
  }

  // Http service actor
  final class ImageBrowserServiceActor extends ImageBrowserService with Actor with ActorLogging {
    override def executionContext: ExecutionContext = context.dispatcher

    override def actorRefFactory: ActorRefFactory = context

    override def receive: Receive = runRoute(this.route()).orElse {
      case IndexRegistryChange(path) ⇒
        evictCacheStartingWith(path)
    }

    @scala.throws[Exception](classOf[Exception])
    override def preStart(): Unit = {
      super.preStart()
      context.system.eventStream.subscribe(self, classOf[IndexRegistryChange])
    }

    @scala.throws[Exception](classOf[Exception])
    override def postStop(): Unit = {
      thumbnailContext.shutdown()
      context.system.eventStream.unsubscribe(self)
      super.postStop()
    }
  }
}


