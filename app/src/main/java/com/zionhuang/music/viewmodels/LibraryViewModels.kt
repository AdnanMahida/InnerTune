@file:OptIn(ExperimentalCoroutinesApi::class)

package com.zionhuang.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zionhuang.innertube.YouTube
import com.zionhuang.music.constants.*
import com.zionhuang.music.db.MusicDatabase
import com.zionhuang.music.extensions.toEnum
import com.zionhuang.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class LibrarySongsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
) : ViewModel() {
    val allSongs = context.dataStore.data
        .map {
            it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE) to (it[SongSortDescendingKey] ?: true)
        }
        .distinctUntilChanged()
        .flatMapLatest { (sortType, descending) ->
            database.songs(sortType, descending)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

@HiltViewModel
class LibraryArtistsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
) : ViewModel() {
    val allArtists = context.dataStore.data
        .map {
            it[ArtistSortTypeKey].toEnum(ArtistSortType.CREATE_DATE) to (it[ArtistSortDescendingKey] ?: true)
        }
        .distinctUntilChanged()
        .flatMapLatest { (sortType, descending) ->
            database.artists(sortType, descending)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            allArtists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter {
                        it.thumbnailUrl == null || Duration.between(it.lastUpdateTime, LocalDateTime.now()) > Duration.ofDays(10)
                    }
                    .forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryAlbumsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
) : ViewModel() {
    val allAlbums = context.dataStore.data
        .map {
            it[AlbumSortTypeKey].toEnum(AlbumSortType.CREATE_DATE) to (it[AlbumSortDescendingKey] ?: true)
        }
        .distinctUntilChanged()
        .flatMapLatest { (sortType, descending) ->
            database.albums(sortType, descending)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

@HiltViewModel
class LibraryPlaylistsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
) : ViewModel() {
    val likedSongCount = database.likedSongsCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val downloadedSongCount = database.downloadedSongsCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val allPlaylists = context.dataStore.data
        .map {
            it[PlaylistSortTypeKey].toEnum(PlaylistSortType.CREATE_DATE) to (it[PlaylistSortDescendingKey] ?: true)
        }
        .distinctUntilChanged()
        .flatMapLatest { (sortType, descending) ->
            database.playlists(sortType, descending)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

@HiltViewModel
class ArtistSongsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val artistId = savedStateHandle.get<String>("artistId")!!
    val artist = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val songs = context.dataStore.data
        .map {
            it[ArtistSongSortTypeKey].toEnum(ArtistSongSortType.CREATE_DATE) to (it[ArtistSongSortDescendingKey] ?: true)
        }
        .distinctUntilChanged()
        .flatMapLatest { (sortType, descending) ->
            database.artistSongs(artistId, sortType, descending)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
