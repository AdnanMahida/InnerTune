package com.zionhuang.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.zionhuang.music.LocalPlayerAwareWindowInsets
import com.zionhuang.music.LocalPlayerConnection
import com.zionhuang.music.R
import com.zionhuang.music.constants.MaxImageCacheSizeKey
import com.zionhuang.music.constants.MaxSongCacheSizeKey
import com.zionhuang.music.ui.component.ListPreference
import com.zionhuang.music.ui.component.PreferenceEntry
import com.zionhuang.music.ui.component.PreferenceGroupTitle
import com.zionhuang.music.ui.utils.formatFileSize
import com.zionhuang.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StorageSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val imageDiskCache = context.imageLoader.diskCache ?: return
    val playerCache = LocalPlayerConnection.current?.service?.cache ?: return

    val coroutineScope = rememberCoroutineScope()

    var imageCacheSize by remember {
        mutableStateOf(imageDiskCache.size)
    }
    var playerCacheSize by remember {
        mutableStateOf(playerCache.cacheSpace)
    }

    LaunchedEffect(imageDiskCache) {
        while (isActive) {
            delay(500)
            imageCacheSize = imageDiskCache.size
        }
    }
    LaunchedEffect(playerCache) {
        while (isActive) {
            delay(500)
            playerCacheSize = playerCache.cacheSpace
        }
    }

    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(key = MaxImageCacheSizeKey, defaultValue = 512)
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(key = MaxSongCacheSizeKey, defaultValue = 1024)

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
    ) {
        PreferenceGroupTitle(
            title = stringResource(R.string.image_cache)
        )

        LinearProgressIndicator(
            progress = (imageCacheSize.toFloat() / imageDiskCache.maxSize).coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Text(
            text = stringResource(R.string.size_used, "${formatFileSize(imageCacheSize)} / ${formatFileSize(imageDiskCache.maxSize)}"),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        ListPreference(
            title = stringResource(R.string.max_cache_size),
            selectedValue = maxImageCacheSize,
            values = listOf(128, 256, 512, 1024, 2048, 4096, 8192),
            valueText = { formatFileSize(it * 1024 * 1024L) },
            onValueSelected = onMaxImageCacheSizeChange
        )

        PreferenceEntry(
            title = stringResource(R.string.clear_image_cache),
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    imageDiskCache.clear()
                }
            },
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.song_cache)
        )

        if (maxSongCacheSize == -1) {
            Text(
                text = stringResource(R.string.size_used, formatFileSize(playerCacheSize)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        } else {
            LinearProgressIndicator(
                progress = (playerCacheSize.toFloat() / (maxSongCacheSize * 1024 * 1024L)).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Text(
                text = stringResource(R.string.size_used, "${formatFileSize(playerCacheSize)} / ${formatFileSize(maxSongCacheSize * 1024 * 1024L)}"),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        ListPreference(
            title = stringResource(R.string.max_cache_size),
            selectedValue = maxSongCacheSize,
            values = listOf(128, 256, 512, 1024, 2048, 4096, 8192, -1),
            valueText = {
                if (it == -1) stringResource(R.string.unlimited) else formatFileSize(it * 1024 * 1024L)
            },
            onValueSelected = onMaxSongCacheSizeChange
        )

        PreferenceEntry(
            title = stringResource(R.string.clear_song_cache),
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    playerCache.keys.forEach { key ->
                        playerCache.removeResource(key)
                    }
                }
            },
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.storage)) },
        navigationIcon = {
            IconButton(onClick = navController::navigateUp) {
                Icon(
                    painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}
