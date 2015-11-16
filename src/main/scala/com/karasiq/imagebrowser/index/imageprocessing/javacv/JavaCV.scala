package com.karasiq.imagebrowser.index.imageprocessing.javacv

import java.nio.file.Path

import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgcodecs._

import scala.util.control.Exception

/**
 * JavaCV helper object
 */
private[javacv] object JavaCV {
  def withImage[T](image: IplImage)(f: IplImage ⇒ T): T = {
    Exception.allCatch.andFinally(cvReleaseImage(image)) {
      f(image)
    }
  }

  def withImage[T](path: Path)(f: IplImage ⇒ T): T = {
    val image = cvLoadImage(path.toAbsolutePath.toString)
    Exception.allCatch.andFinally(cvReleaseImage(image)) {
      f(image)
    }
  }

  /**
    * Creates JPEG-encoded image
    * @param image JavaCV image
    * @param quality JPEG quality
    * @return Bytes of JPEG-encoded image
    */
  def asJpeg(image: IplImage, quality: Int = 85): Array[Byte] = {
    val matrix = cvEncodeImage(".jpeg", image.asCvMat(), Array(CV_IMWRITE_JPEG_QUALITY, quality, 0))
    Exception.allCatch.andFinally(cvReleaseMat(matrix)) {
      val ptr = matrix.data_ptr()
      val data = new Array[Byte](matrix.size())
      ptr.get(data, 0, matrix.size())
      data
    }
  }
}
