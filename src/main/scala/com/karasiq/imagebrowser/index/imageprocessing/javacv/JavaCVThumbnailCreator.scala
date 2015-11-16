package com.karasiq.imagebrowser.index.imageprocessing.javacv

import java.nio.file.Path

import com.karasiq.imagebrowser.index.imageprocessing.ThumbnailCreator
import com.karasiq.imagebrowser.index.imageprocessing.javacv.JavaCV._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_imgproc._

final class JavaCVThumbnailCreator extends ThumbnailCreator {
  override def createThumbnail(image: Path, size: Int): Array[Byte] = {
    val img = cvLoadImage(image.toAbsolutePath.toString)
    val thumb = cvCreateImage(cvSize(size, size), img.depth(), img.nChannels())
    cvResize(img, thumb)

    val jpeg = asJpeg(thumb)
    cvRelease(img)
    cvRelease(thumb)
    jpeg
  }
}
