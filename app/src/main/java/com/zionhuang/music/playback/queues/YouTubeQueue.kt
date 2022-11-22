package com.zionhuang.music.playback.queues

import com.google.android.exoplayer2.MediaItem
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.models.WatchEndpoint
import com.zionhuang.music.extensions.toMediaItem
import com.zionhuang.music.models.MediaMetadata
import com.zionhuang.music.models.toMediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class YouTubeQueue(
    private val endpoint: WatchEndpoint,
    item: SongItem? = null,
) : Queue {
    override val preloadItem: MediaMetadata? = item?.toMediaMetadata()
    private var continuation: String? = null

    override suspend fun getInitialStatus(): Queue.Status {
        val nextResult = withContext(IO) {
            YouTube.next(endpoint, continuation).getOrThrow()
        }
        continuation = nextResult.continuation
        return Queue.Status(
            title = nextResult.title,
            items = nextResult.items.map { it.toMediaItem() },
            index = nextResult.currentIndex ?: 0
        )
    }

    override fun hasNextPage(): Boolean = continuation != null

    override suspend fun nextPage(): List<MediaItem> {
        val nextResult = withContext(IO) {
            YouTube.next(endpoint, continuation).getOrThrow()
        }
        continuation = nextResult.continuation
        return nextResult.items.map { it.toMediaItem() }
    }
}