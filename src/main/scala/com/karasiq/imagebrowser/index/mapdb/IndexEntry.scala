package com.karasiq.imagebrowser.index.mapdb

import java.time.Instant

private[mapdb] final case class IndexEntry(isDirectory: Boolean, lastModified: Instant)
