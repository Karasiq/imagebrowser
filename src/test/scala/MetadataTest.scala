import java.nio.file.Paths

import com.karasiq.imagebrowser.index.imageprocessing.DefaultMetadataReader
import com.karasiq.imagebrowser.index.imageprocessing.imageio.ImageIOThumbnailCreator
import com.karasiq.imagebrowser.index.imageprocessing.javacv.{JavaCVVideoThumbnailCreator, JavaCVThumbnailCreator}
import com.typesafe.config.ConfigFactory
import org.scalatest.FlatSpec

class MetadataTest extends FlatSpec {
  val config = ConfigFactory.load().getConfig("imageBrowser.test")
  val file = Paths.get(config.getString("jpg-file"))

  "Metadata reader" should "read JPG keywords" in {
    val keyword = config.getString("jpg-keyword")
    val descriptionWord = config.getString("jpg-description-keyword")

    val Some(data) = new DefaultMetadataReader().readMetadata(file)
    println(data.metadata)
    assert(data.metadata("Keywords").contains(keyword), s"Keyword not found: $keyword")
    assert(data.metadata("Caption/Abstract").contains(descriptionWord), s"Description word not found: $descriptionWord")
  }

  "ImageIO" should "create thumbnail" in {
    assert(new ImageIOThumbnailCreator().createThumbnail(file, 150).length > 0)
  }

  "JavaCV" should "create thumbnail" in {
    assert(new JavaCVThumbnailCreator().createThumbnail(file, 150).length > 0)
  }

  it should "create video thumbnail" in {
    val videoFile = Paths.get(config.getString("video"))
    assert(new JavaCVVideoThumbnailCreator().createThumbnail(videoFile, 150).length > 0)
  }
}
