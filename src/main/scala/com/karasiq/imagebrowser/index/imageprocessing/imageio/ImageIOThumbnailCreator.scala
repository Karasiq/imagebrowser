package com.karasiq.imagebrowser.index.imageprocessing.imageio

import java.awt.RenderingHints._
import java.awt.image._
import java.awt.{Image, Toolkit}
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import javax.imageio.ImageIO

import com.karasiq.imagebrowser.index.imageprocessing.ThumbnailCreator
import org.apache.commons.io.IOUtils

import scala.collection.JavaConversions._
import scala.util.control.Exception

final class ImageIOThumbnailCreator extends ThumbnailCreator {
   private def redrawImage(img: Image, size: Int): BufferedImage = {
    val bufferedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
    val graphics = bufferedImage.createGraphics()
    try {
      graphics.setRenderingHints(Map(
        KEY_RENDERING → VALUE_RENDER_SPEED,
        KEY_COLOR_RENDERING → VALUE_COLOR_RENDER_SPEED,
//        KEY_ANTIALIASING → VALUE_ANTIALIAS_ON,
        KEY_INTERPOLATION → VALUE_INTERPOLATION_NEAREST_NEIGHBOR
      ))
      graphics.drawImage(img, 0, 0, size, size, null)
      bufferedImage
    } finally {
      graphics.dispose()
    }
  }

  // JPG colors fix
  private def loadImage(image: Path): BufferedImage = {
    val img = Toolkit.getDefaultToolkit.createImage(image.toString)

    val RGB_MASKS: Array[Int] = Array(0xFF0000, 0xFF00, 0xFF)
    val RGB_OPAQUE: ColorModel = new DirectColorModel(32, RGB_MASKS(0), RGB_MASKS(1), RGB_MASKS(2))

    val pg = new PixelGrabber(img, 0, 0, -1, -1, true)
    pg.grabPixels()

    (pg.getWidth, pg.getHeight, pg.getPixels) match {
      case (width, height, pixels: Array[Int]) ⇒
        val buffer = new DataBufferInt(pixels, width * height)
        val raster = Raster.createPackedRaster(buffer, width, height, width, RGB_MASKS, null)
        new BufferedImage(RGB_OPAQUE, raster, false, null)

      case _ ⇒
        throw new IllegalArgumentException("Invalid image")
    }
  }

  override def createThumbnail(image: Path, size: Int): Array[Byte] = {
    val img = redrawImage(loadImage(image), size)
    val stream = new ByteArrayOutputStream(8000)
    Exception.allCatch.andFinally(IOUtils.closeQuietly(stream)) {
      ImageIO.write(img, "jpeg", stream)
      stream.toByteArray
    }
  }
}
