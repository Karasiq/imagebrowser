package com.karasiq.imagebrowser.index.imageprocessing.javacv

import java.nio.file.Path

import com.karasiq.imagebrowser.index.imageprocessing.ThumbnailCreator
import org.bytedeco.javacv.{FFmpegFrameGrabber, OpenCVFrameConverter}

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
      JavaCV.withImage(converter.convert(grabber.grabImage())) { frame ⇒
        JavaCV.asJpeg(frame)
      }
    }
  }
}
