import java.nio.file.Paths

import com.karasiq.imagebrowser.index.imageprocessing.DefaultMetadataReader
import com.typesafe.config.ConfigFactory
import org.scalatest.FlatSpec

class MetadataTest extends FlatSpec {
  "Metadata reader" should "read JPG keywords" in {
    val config = ConfigFactory.load().getConfig("imageBrowser")

    val file = config.getString("test.jpg-file")
    val keyword = config.getString("test.jpg-keyword")
    val descriptionWord = config.getString("test.jpg-description-keyword")

    val Some(data) = new DefaultMetadataReader().readMetadata(Paths.get(file))
    println(data.metadata)
    assert(data.metadata("Keywords").contains(keyword), s"Keyword not found: $keyword")
    assert(data.metadata("Caption/Abstract").contains(descriptionWord), s"Description word not found: $descriptionWord")
  }
}
