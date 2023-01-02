package com.zionhuang.music.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.zionhuang.music.R
import com.zionhuang.music.compose.LocalPlayerAwareWindowInsets
import com.zionhuang.music.compose.component.PreferenceEntry

@Composable
fun SettingsScreen(
    navController: NavController,
) {
    Column(
        modifier = Modifier
            .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues())
            .verticalScroll(rememberScrollState())
    ) {
        PreferenceEntry(
            title = stringResource(R.string.pref_appearance_title),
            icon = R.drawable.ic_palette,
            onClick = { navController.navigate("settings/appearance") }
        )
        PreferenceEntry(
            title = stringResource(R.string.pref_content_title),
            icon = R.drawable.ic_language,
            onClick = { navController.navigate("settings/content") }
        )
        PreferenceEntry(
            title = stringResource(R.string.pref_player_audio_title),
            icon = R.drawable.ic_play,
            onClick = { navController.navigate("settings/player") }
        )
        PreferenceEntry(
            title = stringResource(R.string.pref_storage_title),
            icon = R.drawable.ic_storage,
            onClick = { navController.navigate("settings/storage") }
        )
        PreferenceEntry(
            title = stringResource(R.string.pref_general_title),
            icon = R.drawable.ic_testing,
            onClick = { navController.navigate("settings/general") }
        )
        PreferenceEntry(
            title = stringResource(R.string.pref_privacy_title),
            icon = R.drawable.ic_security,
            onClick = { navController.navigate("settings/privacy") }
        )
        PreferenceEntry(
            title = stringResource(R.string.pref_backup_restore_title),
            icon = R.drawable.ic_settings_backup_restore,
            onClick = { navController.navigate("settings/backup_restore") }
        )
        PreferenceEntry(
            title = stringResource(R.string.pref_about_title),
            icon = R.drawable.ic_info,
            onClick = { navController.navigate("settings/about") }
        )
    }
}
