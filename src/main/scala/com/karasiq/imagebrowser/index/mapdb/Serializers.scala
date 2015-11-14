package com.karasiq.imagebrowser.index.mapdb

import java.io.{DataInput, DataOutput}
import java.nio.file.{Path, Paths}
import java.time.Instant

import com.karasiq.imagebrowser.index.ImageMetadata
import org.mapdb.BTreeKeySerializer.ArrayKeySerializer
import org.mapdb.Serializer.CompressionWrapper
import org.mapdb.{DataIO, Fun, Serializer}

/**
  * MapDB serialization helper object
  */
private[mapdb] object Serializers {
  /**
   * File tree key serializer
   */
  val INDEX_KEY = new ArrayKeySerializer(
    Array(Fun.COMPARATOR, Fun.COMPARATOR),
    Array(Serializer.STRING_XXHASH, Serializer.STRING_XXHASH)
  )

  /**
   * File tree value serializer
   */
  val INDEX_ENTRY = new Serializer[IndexEntry] {
    override def serialize(dataOutput: DataOutput, a: IndexEntry): Unit = {
      dataOutput.writeBoolean(a.isDirectory)
      DataIO.packLong(dataOutput, a.lastModified.getEpochSecond)
      DataIO.packInt(dataOutput, a.lastModified.getNano)
    }

    override def deserialize(dataInput: DataInput, i: Int): IndexEntry = {
      val directory = dataInput.readBoolean()
      val lastModified = Instant.ofEpochSecond(DataIO.unpackLong(dataInput), DataIO.unpackInt(dataInput))
      IndexEntry(directory, lastModified)
    }
  }

  /**
   * File metadata value serializer
   */
  val INDEX_METADATA = new CompressionWrapper[ImageMetadata](new Serializer[ImageMetadata] {
    override def serialize(dataOutput: DataOutput, a: ImageMetadata): Unit = {
      DataIO.packInt(dataOutput, a.metadata.size)
      a.metadata.foreach { case (key, value) ⇒
        dataOutput.writeUTF(key)
        dataOutput.writeUTF(value)
      }
    }

    override def deserialize(dataInput: DataInput, i: Int): ImageMetadata = {
      val length = DataIO.unpackInt(dataInput)
      if (length > 0) {
        val data = (1 to length).map { _ ⇒
          val key = dataInput.readUTF()
          val value = dataInput.readUTF()
          key → value
        }

        ImageMetadata(data.toMap)
      } else {
        ImageMetadata.empty
      }
    }
  })

  /**
   * Valid path string
   * @param p File path
   * @return Path string
   */
  def asString(p: Path): String = {
    p.toAbsolutePath.toString
  }

  /**
   * Path as file tree key
   * @param path File path
   * @return File tree key
   */
  def asIndexKey(path: Path): Array[Any] = {
    Array(asString(path.toAbsolutePath.getParent), path.getFileName.toString)
  }

  /**
   * Path -> value extractor
   */
  object StoredIndexEntry {
    import scala.util.Try

    def unapply(kv: (Array[Any], IndexEntry)): Option[(Path, IndexEntry)] = {
      Try(Paths.get(kv._1(0).asInstanceOf[String], kv._1(1).asInstanceOf[String]) → kv._2).toOption
    }
  }
}