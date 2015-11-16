package com.karasiq.imagebrowser

import com.karasiq.imagebrowser.dirmanager.MapDbDirectoryManagerProvider
import com.karasiq.imagebrowser.index.filewatcher.DefaultFileWatcherServiceProvider
import com.karasiq.imagebrowser.index.imageprocessing.DefaultImageProcessingProvider
import com.karasiq.imagebrowser.index.mapdb.{MapDbThumbnailsCacheProvider, MapDbIndexRegistryProvider}
import com.karasiq.imagebrowser.providers.{ActorSystemProvider, IndexRegistryDbProvider, ThumbnailsCacheDbProvider}

trait DefaultServicesProvider extends DefaultImageProcessingProvider with MapDbIndexRegistryProvider with MapDbDirectoryManagerProvider with DefaultFileWatcherServiceProvider with MapDbThumbnailsCacheProvider { self: ActorSystemProvider with IndexRegistryDbProvider with ThumbnailsCacheDbProvider ⇒

  // Default implementations for custom MapDB, ActorSystem and HttpService
}
