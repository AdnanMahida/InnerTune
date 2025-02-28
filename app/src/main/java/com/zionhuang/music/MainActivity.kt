package com.zionhuang.music

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.imageLoader
import coil.request.ImageRequest
import com.google.common.util.concurrent.MoreExecutors
import com.valentinilk.shimmer.LocalShimmerTheme
import com.zionhuang.music.constants.*
import com.zionhuang.music.db.MusicDatabase
import com.zionhuang.music.db.entities.PlaylistEntity.Companion.DOWNLOADED_PLAYLIST_ID
import com.zionhuang.music.db.entities.PlaylistEntity.Companion.LIKED_PLAYLIST_ID
import com.zionhuang.music.db.entities.SearchHistory
import com.zionhuang.music.extensions.*
import com.zionhuang.music.playback.MusicService
import com.zionhuang.music.playback.MusicService.MusicBinder
import com.zionhuang.music.playback.PlayerConnection
import com.zionhuang.music.ui.component.*
import com.zionhuang.music.ui.component.shimmer.ShimmerTheme
import com.zionhuang.music.ui.player.BottomSheetPlayer
import com.zionhuang.music.ui.screens.*
import com.zionhuang.music.ui.screens.artist.ArtistItemsScreen
import com.zionhuang.music.ui.screens.artist.ArtistScreen
import com.zionhuang.music.ui.screens.artist.ArtistSongsScreen
import com.zionhuang.music.ui.screens.library.LibraryAlbumsScreen
import com.zionhuang.music.ui.screens.library.LibraryArtistsScreen
import com.zionhuang.music.ui.screens.library.LibraryPlaylistsScreen
import com.zionhuang.music.ui.screens.library.LibrarySongsScreen
import com.zionhuang.music.ui.screens.playlist.BuiltInPlaylistScreen
import com.zionhuang.music.ui.screens.playlist.LocalPlaylistScreen
import com.zionhuang.music.ui.screens.playlist.OnlinePlaylistScreen
import com.zionhuang.music.ui.screens.search.LocalSearchScreen
import com.zionhuang.music.ui.screens.search.OnlineSearchResult
import com.zionhuang.music.ui.screens.search.OnlineSearchScreen
import com.zionhuang.music.ui.screens.settings.*
import com.zionhuang.music.ui.theme.*
import com.zionhuang.music.ui.utils.appBarScrollBehavior
import com.zionhuang.music.ui.utils.canNavigateUp
import com.zionhuang.music.ui.utils.resetHeightOffset
import com.zionhuang.music.utils.dataStore
import com.zionhuang.music.utils.get
import com.zionhuang.music.utils.rememberEnumPreference
import com.zionhuang.music.utils.rememberPreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase
    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicBinder) {
                playerConnection = PlayerConnection(database, service)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerConnection?.dispose()
            playerConnection = null
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Connect to service so that notification and background playing will work
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener(
            { controllerFuture.get() },
            MoreExecutors.directExecutor()
        )

        setContent {
            val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
            val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
            val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            }
            LaunchedEffect(useDarkTheme) {
                setSystemBarAppearance(useDarkTheme)
            }
            var themeColor by rememberSaveable(stateSaver = ColorSaver) {
                mutableStateOf(DefaultThemeColor)
            }

            LaunchedEffect(playerConnection, enableDynamicTheme, isSystemInDarkTheme) {
                val playerConnection = playerConnection
                if (!enableDynamicTheme || playerConnection == null) {
                    themeColor = DefaultThemeColor
                    return@LaunchedEffect
                }
                playerConnection.service.currentMediaMetadata.collectLatest { song ->
                    themeColor = if (song != null) {
                        withContext(Dispatchers.IO) {
                            val result = imageLoader.execute(
                                ImageRequest.Builder(this@MainActivity)
                                    .data(song.thumbnailUrl)
                                    .allowHardware(false) // pixel access is not supported on Config#HARDWARE bitmaps
                                    .build()
                            )
                            (result.drawable as BitmapDrawable).bitmap.extractThemeColor()
                        }
                    } else DefaultThemeColor
                }
            }

            InnerTuneTheme(
                darkTheme = useDarkTheme,
                pureBlack = pureBlack,
                themeColor = themeColor
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val focusManager = LocalFocusManager.current
                    val density = LocalDensity.current
                    val windowsInsets = WindowInsets.systemBars
                    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }

                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()

                    val navigationItems = remember {
                        listOf(Screens.Home, Screens.Songs, Screens.Artists, Screens.Albums, Screens.Playlists)
                    }
                    val defaultOpenTab = remember {
                        dataStore[DefaultOpenTabKey].toEnum(defaultValue = NavigationTab.HOME)
                    }

                    val (query, onQueryChange) = rememberSaveable(stateSaver = TextFieldValue.Saver) {
                        mutableStateOf(TextFieldValue())
                    }
                    var active by rememberSaveable {
                        mutableStateOf(false)
                    }
                    val onActiveChange: (Boolean) -> Unit = { newActive ->
                        active = newActive
                        if (!newActive) {
                            focusManager.clearFocus()
                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                onQueryChange(TextFieldValue())
                            }
                        }
                    }
                    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)

                    val onSearch: (String) -> Unit = {
                        onActiveChange(false)
                        navController.navigate("search/$it")
                        if (dataStore[PauseSearchHistoryKey] != true) {
                            database.query {
                                insert(SearchHistory(query = it))
                            }
                        }
                    }

                    val shouldShowSearchBar = remember(active, navBackStackEntry) {
                        active || navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                navBackStackEntry?.destination?.route?.startsWith("search/") == true
                    }
                    val shouldShowNavigationBar = remember(navBackStackEntry, active) {
                        navBackStackEntry?.destination?.route == null ||
                                navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } && !active
                    }
                    val navigationBarHeight by animateDpAsState(
                        targetValue = if (shouldShowNavigationBar) NavigationBarHeight else 0.dp,
                        animationSpec = NavigationBarAnimationSpec,
                        label = ""
                    )

                    val playerBottomSheetState = rememberBottomSheetState(
                        dismissedBound = 0.dp,
                        collapsedBound = bottomInset + (if (shouldShowNavigationBar) NavigationBarHeight else 0.dp) + MiniPlayerHeight,
                        expandedBound = maxHeight,
                    )

                    val playerAwareWindowInsets = remember(bottomInset, shouldShowNavigationBar, playerBottomSheetState.isDismissed) {
                        var bottom = bottomInset
                        if (shouldShowNavigationBar) bottom += NavigationBarHeight
                        if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
                        windowsInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                    }

                    val scrollBehavior = appBarScrollBehavior(
                        canScroll = {
                            navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        }
                    )

                    LaunchedEffect(navBackStackEntry) {
                        if (navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                            val searchQuery = navBackStackEntry?.arguments?.getString("query")!!
                            onQueryChange(TextFieldValue(searchQuery, TextRange(searchQuery.length)))
                        } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                            onQueryChange(TextFieldValue())
                        }
                        scrollBehavior.state.resetHeightOffset()
                    }
                    LaunchedEffect(active) {
                        if (active) {
                            scrollBehavior.state.resetHeightOffset()
                        }
                    }

                    LaunchedEffect(playerConnection) {
                        val player = playerConnection?.player ?: return@LaunchedEffect
                        if (player.currentMediaItem == null) {
                            if (!playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.dismiss()
                            }
                        } else {
                            if (playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.collapseSoft()
                            }
                        }
                    }

                    val expandOnPlay by rememberPreference(ExpandOnPlayKey, defaultValue = false)

                    DisposableEffect(playerConnection, playerBottomSheetState) {
                        val player = playerConnection?.player ?: return@DisposableEffect onDispose { }
                        val listener = object : Player.Listener {
                            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED && mediaItem != null && playerBottomSheetState.isDismissed) {
                                    if (expandOnPlay) {
                                        playerBottomSheetState.expandSoft()
                                    } else {
                                        playerBottomSheetState.collapseSoft()
                                    }
                                }
                            }
                        }
                        player.addListener(listener)
                        onDispose {
                            player.removeListener(listener)
                        }
                    }

                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.background),
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalShimmerTheme provides ShimmerTheme
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = when (defaultOpenTab) {
                                NavigationTab.HOME -> Screens.Home
                                NavigationTab.SONG -> Screens.Songs
                                NavigationTab.ARTIST -> Screens.Artists
                                NavigationTab.ALBUM -> Screens.Albums
                                NavigationTab.PLAYLIST -> Screens.Playlists
                            }.route,
                            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                        ) {
                            composable(Screens.Home.route) {
                                HomeScreen(navController)
                            }
                            composable(Screens.Songs.route) {
                                LibrarySongsScreen(navController)
                            }
                            composable(Screens.Artists.route) {
                                LibraryArtistsScreen(navController)
                            }
                            composable(Screens.Albums.route) {
                                LibraryAlbumsScreen(navController)
                            }
                            composable(Screens.Playlists.route) {
                                LibraryPlaylistsScreen(navController)
                            }
                            composable("history") {
                                HistoryScreen(navController)
                            }
                            composable("new_release") {
                                NewReleaseScreen(navController, scrollBehavior)
                            }
                            composable(
                                route = "search/{query}",
                                arguments = listOf(
                                    navArgument("query") {
                                        type = NavType.StringType
                                    }
                                )
                            ) {
                                OnlineSearchResult(navController)
                            }
                            composable(
                                route = "album/{albumId}?playlistId={playlistId}",
                                arguments = listOf(
                                    navArgument("albumId") {
                                        type = NavType.StringType
                                    },
                                    navArgument("playlistId") {
                                        type = NavType.StringType
                                        nullable = true
                                    }
                                )
                            ) {
                                AlbumScreen(navController, scrollBehavior)
                            }
                            composable(
                                route = "artist/{artistId}",
                                arguments = listOf(
                                    navArgument("artistId") {
                                        type = NavType.StringType
                                    }
                                )
                            ) { backStackEntry ->
                                val artistId = backStackEntry.arguments?.getString("artistId")!!
                                if (artistId.startsWith("LA")) {
                                    ArtistSongsScreen(navController, scrollBehavior)
                                } else {
                                    ArtistScreen(navController, scrollBehavior)
                                }
                            }
                            composable(
                                route = "artist/{artistId}/songs",
                                arguments = listOf(
                                    navArgument("artistId") {
                                        type = NavType.StringType
                                    }
                                )
                            ) {
                                ArtistSongsScreen(navController, scrollBehavior)
                            }
                            composable(
                                route = "artist/{artistId}/items?browseId={browseId}?params={params}",
                                arguments = listOf(
                                    navArgument("artistId") {
                                        type = NavType.StringType
                                    },
                                    navArgument("browseId") {
                                        type = NavType.StringType
                                        nullable = true
                                    },
                                    navArgument("params") {
                                        type = NavType.StringType
                                        nullable = true
                                    }
                                )
                            ) {
                                ArtistItemsScreen(navController, scrollBehavior)
                            }
                            composable(
                                route = "online_playlist/{playlistId}",
                                arguments = listOf(
                                    navArgument("playlistId") {
                                        type = NavType.StringType
                                    }
                                )
                            ) {
                                OnlinePlaylistScreen(navController, scrollBehavior)
                            }
                            composable(
                                route = "local_playlist/{playlistId}",
                                arguments = listOf(
                                    navArgument("playlistId") {
                                        type = NavType.StringType
                                    }
                                )
                            ) { backStackEntry ->
                                val playlistId = backStackEntry.arguments?.getString("playlistId")!!
                                if (playlistId == LIKED_PLAYLIST_ID || playlistId == DOWNLOADED_PLAYLIST_ID) {
                                    BuiltInPlaylistScreen(navController, scrollBehavior)
                                } else {
                                    LocalPlaylistScreen(navController, scrollBehavior)
                                }
                            }
                            composable("settings") {
                                SettingsScreen(navController, scrollBehavior)
                            }
                            composable("settings/appearance") {
                                AppearanceSettings(navController, scrollBehavior)
                            }
                            composable("settings/content") {
                                ContentSettings(navController, scrollBehavior)
                            }
                            composable("settings/player") {
                                PlayerSettings(navController, scrollBehavior)
                            }
                            composable("settings/storage") {
                                StorageSettings(navController, scrollBehavior)
                            }
                            composable("settings/general") {
                                GeneralSettings(navController, scrollBehavior)
                            }
                            composable("settings/privacy") {
                                PrivacySettings(navController, scrollBehavior)
                            }
                            composable("settings/backup_restore") {
                                BackupAndRestore(navController, scrollBehavior)
                            }
                            composable("settings/about") {
                                AboutScreen(navController, scrollBehavior)
                            }
                        }

                        AnimatedVisibility(
                            visible = shouldShowSearchBar,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            SearchBar(
                                query = query,
                                onQueryChange = onQueryChange,
                                onSearch = onSearch,
                                active = active,
                                onActiveChange = onActiveChange,
                                scrollBehavior = scrollBehavior,
                                placeholder = {
                                    Text(
                                        text = stringResource(
                                            if (!active) R.string.search
                                            else when (searchSource) {
                                                SearchSource.LOCAL -> R.string.search_library
                                                SearchSource.ONLINE -> R.string.search_yt_music
                                            }
                                        )
                                    )
                                },
                                leadingIcon = {
                                    IconButton(onClick = {
                                        when {
                                            active -> onActiveChange(false)
                                            navController.canNavigateUp && !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                navController.navigateUp()
                                            }

                                            else -> onActiveChange(true)
                                        }
                                    }) {
                                        Icon(
                                            painterResource(
                                                if (active || (navController.canNavigateUp && !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route })) {
                                                    R.drawable.ic_arrow_back
                                                } else {
                                                    R.drawable.ic_search
                                                }
                                            ),
                                            contentDescription = null
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (active) {
                                        if (query.text.isNotEmpty()) {
                                            IconButton(
                                                onClick = { onQueryChange(TextFieldValue("")) }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_close),
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                searchSource = if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(
                                                    when (searchSource) {
                                                        SearchSource.LOCAL -> R.drawable.ic_library_music
                                                        SearchSource.ONLINE -> R.drawable.ic_language
                                                    }
                                                ),
                                                contentDescription = null
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.align(Alignment.TopCenter)
                            ) {
                                Crossfade(
                                    targetState = searchSource,
                                    label = "",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = if (!playerBottomSheetState.isDismissed) MiniPlayerHeight else 0.dp)
                                        .navigationBarsPadding()
                                ) { searchSource ->
                                    when (searchSource) {
                                        SearchSource.LOCAL -> LocalSearchScreen(
                                            query = query.text,
                                            navController = navController,
                                            onDismiss = { onActiveChange(false) }
                                        )

                                        SearchSource.ONLINE -> OnlineSearchScreen(
                                            query = query.text,
                                            onQueryChange = onQueryChange,
                                            navController = navController,
                                            onSearch = {
                                                navController.navigate("search/$it")
                                                if (dataStore[PauseSearchHistoryKey] != true) {
                                                    database.query {
                                                        insert(SearchHistory(query = it))
                                                    }
                                                }
                                            },
                                            onDismiss = { onActiveChange(false) }
                                        )
                                    }
                                }
                            }
                        }

                        BottomSheetPlayer(
                            state = playerBottomSheetState,
                            navController = navController
                        )

                        NavigationBar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset {
                                    if (navigationBarHeight == 0.dp) {
                                        IntOffset(x = 0, y = (bottomInset + NavigationBarHeight).roundToPx())
                                    } else {
                                        val slideOffset = (bottomInset + NavigationBarHeight) * playerBottomSheetState.progress.coerceIn(0f, 1f)
                                        val hideOffset = (bottomInset + NavigationBarHeight) * (1 - navigationBarHeight / NavigationBarHeight)
                                        IntOffset(
                                            x = 0,
                                            y = (slideOffset + hideOffset).roundToPx()
                                        )
                                    }
                                }
                        ) {
                            navigationItems.fastForEach { screen ->
                                NavigationBarItem(
                                    selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true,
                                    icon = {
                                        Icon(
                                            painter = painterResource(screen.iconId),
                                            contentDescription = null
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(screen.titleId),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }

                        BottomSheetMenu(
                            state = LocalMenuState.current,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }
}

const val ACTION_SHOW_BOTTOM_SHEET = "show_bottom_sheet"

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
