import java.nio.file.Paths

import com.karasiq.imagebrowser.DefaultServicesProvider
import org.scalatest.{FlatSpec, Matchers}

class DirManagerTest extends FlatSpec with Matchers with TestMapDbProvider with TestActorSystemProvider with DefaultServicesProvider {
  @inline
  private def normalizePath(path: String) = Paths.get(path).normalize().toString

  val testDir = normalizePath(System.getProperty("java.io.tmpdir"))

  "A directory manager" should "add directory" in {
    directoryManager.addDirectory(testDir, readMetadata = false)

    directoryManager.trackedDirectories should equal(Set(testDir))
    indexRegistry.directories.toIterable.map(_.path.toString) should contain (testDir)
  }

  it should "remove directory" in {
    directoryManager.removeDirectory(testDir)
    directoryManager.trackedDirectories should be (empty)
    indexRegistry.directories.toIterable should be (empty)
  }
}
