package com.karasiq.imagebrowser.index.imageprocessing.javacv

import java.nio.file.Path

import com.karasiq.imagebrowser.index.imageprocessing.ThumbnailCreator
import org.bytedeco.javacv.{FFmpegFrameGrabber, OpenCVFrameConverter}
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgcodecs._

import scala.util.control.Exception

class JavaCVVideoThumbnailCreator extends ThumbnailCreator {
  private val bitrate: Int = 100000 // 100 kbps

  override def createThumbnail(image: Path, size: Int): Array[Byte] = {
    val grabber = new FFmpegFrameGrabber(image.toFile)

    Exception.allCatch.andFinally(grabber.stop()) {
      grabber.setImageHeight(size)
      grabber.setImageWidth(size)
      grabber.setVideoBitrate(bitrate)
      grabber.start()

      val converter = new OpenCVFrameConverter.ToIplImage()
      val image = converter.convert(grabber.grabImage())
      val jpeg = JavaCV.asJpeg(image)
      cvReleaseImage(image)
      jpeg
    }
  }
}
