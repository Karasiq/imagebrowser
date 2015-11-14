import com.karasiq.imagebrowser.providers.{ThumbnailsCacheDbProvider, IndexRegistryDbProvider}
import com.karasiq.mapdb.MapDbFile
import org.apache.commons.io.IOUtils
import org.mapdb.DBMaker
import org.scalatest.{BeforeAndAfterAll, Suite}

trait TestMapDbProvider extends IndexRegistryDbProvider with ThumbnailsCacheDbProvider with BeforeAndAfterAll { self: Suite ⇒
  override final val indexRegistryDb: MapDbFile = MapDbFile(DBMaker.memoryDirectDB().transactionDisable().make())

  override final val thumbnailsCacheDb: MapDbFile = MapDbFile(DBMaker.memoryDirectDB().transactionDisable().make())

  abstract override protected def afterAll(): Unit = {
    IOUtils.closeQuietly(indexRegistryDb)
    IOUtils.closeQuietly(thumbnailsCacheDb)
    super.afterAll()
  }
}
