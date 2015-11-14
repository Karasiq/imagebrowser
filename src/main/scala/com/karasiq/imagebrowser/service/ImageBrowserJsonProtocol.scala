package com.karasiq.imagebrowser.service

import com.karasiq.imagebrowser.index.{DirectoryCursor, ImageCursor, ImageMetadata}
import spray.json._

object ImageBrowserJsonProtocol extends DefaultJsonProtocol {
  implicit val metadataJsonFormat: JsonFormat[Option[ImageMetadata]] = new JsonFormat[Option[ImageMetadata]] {
    override def write(obj: Option[ImageMetadata]): JsValue = {
      obj.fold[JsValue](JsString(""))(_.metadata.toJson)
    }

    override def read(json: JsValue): Option[ImageMetadata] = ???
  }

  implicit val imageJsonFormat: JsonFormat[ImageCursor] = new JsonFormat[ImageCursor] {
    override def write(obj: ImageCursor): JsValue = {
      JsObject(
        "path" → JsString(obj.path.toString),
        "lastModified" → JsString(obj.lastModified.toString),
        "iptc" → obj.metadata.toJson
      )
    }

    override def read(json: JsValue): ImageCursor = ???
  }

  implicit val directoryJsonWriter: JsonFormat[DirectoryCursor] = new JsonFormat[DirectoryCursor] {
    override def write(obj: DirectoryCursor): JsValue = {
      JsObject(
        "path" → JsString(obj.path.toString),
        "lastModified" → JsString(obj.lastModified.toString),
        "subDirs" → obj.subDirs.toVector.map(_.path.toString).sorted.toJson,
        "images" → obj.images.toVector.sortBy(_.lastModified).toJson
      )
    }

    override def read(json: JsValue): DirectoryCursor = ???
  }
}
