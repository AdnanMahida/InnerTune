package com.zionhuang.music.compose.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.zionhuang.music.R
import com.zionhuang.music.compose.LocalPlayerAwareWindowInsets
import com.zionhuang.music.compose.component.PreferenceEntry
import com.zionhuang.music.constants.Constants.GITHUB_URL

@Composable
fun AboutScreen() {
    val uriHandler = LocalUriHandler.current

    Column(
        Modifier
            .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues())
            .verticalScroll(rememberScrollState())
    ) {
        PreferenceEntry(
            title = stringResource(R.string.pref_app_version_title),
            description = stringResource(R.string.app_version),
            icon = R.drawable.ic_info,
            onClick = { }
        )
        PreferenceEntry(
            title = stringResource(R.string.pref_github_title),
            description = stringResource(R.string.pref_github_summary),
            icon = R.drawable.ic_github,
            onClick = {
                uriHandler.openUri(GITHUB_URL)
            }
        )
    }
}
