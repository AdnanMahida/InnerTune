package com.zionhuang.music.compose.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zionhuang.music.R
import com.zionhuang.music.compose.LocalPlayerAwareWindowInsets
import com.zionhuang.music.compose.component.EditTextPreference
import com.zionhuang.music.compose.component.ListPreference
import com.zionhuang.music.compose.component.PreferenceGroupTitle
import com.zionhuang.music.compose.component.SwitchPreference
import com.zionhuang.music.constants.*
import com.zionhuang.music.extensions.mutablePreferenceState
import java.net.Proxy

@Composable
fun ContentSettings() {
    val (contentLanguage, onContentLanguageChange) = mutablePreferenceState(key = CONTENT_LANGUAGE, defaultValue = "system")
    val (contentCountry, onContentCountryChange) = mutablePreferenceState(key = CONTENT_COUNTRY, defaultValue = "system")
    val (proxyEnabled, onProxyEnabledChange) = mutablePreferenceState(key = PROXY_ENABLED, defaultValue = false)
    val (proxyType, onProxyTypeChange) = mutablePreferenceState(key = PROXY_TYPE, defaultValue = Proxy.Type.HTTP)
    val (proxyUrl, onProxyUrlChange) = mutablePreferenceState(key = PROXY_URL, defaultValue = "host:port")


    Column(
        Modifier
            .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues())
            .verticalScroll(rememberScrollState())
    ) {
        ListPreference(
            title = stringResource(R.string.pref_content_language_title),
            icon = R.drawable.ic_language,
            selectedValue = contentLanguage,
            values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
            valueText = {
                LanguageCodeToName.getOrElse(it) {
                    stringResource(R.string.system_default)
                }
            },
            onValueSelected = onContentLanguageChange
        )
        ListPreference(
            title = stringResource(R.string.pref_default_content_country_title),
            icon = R.drawable.ic_place,
            selectedValue = contentCountry,
            values = listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList(),
            valueText = {
                CountryCodeToName.getOrElse(it) {
                    stringResource(R.string.system_default)
                }
            },
            onValueSelected = onContentCountryChange
        )

        PreferenceGroupTitle(
            title = "PROXY"
        )

        SwitchPreference(
            title = stringResource(R.string.pref_enable_proxy_title),
            checked = proxyEnabled,
            onCheckedChange = onProxyEnabledChange
        )

        if (proxyEnabled) {
            ListPreference(
                title = stringResource(R.string.pref_proxy_type_title),
                selectedValue = proxyType,
                values = listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS),
                valueText = { it.name },
                onValueSelected = onProxyTypeChange
            )
            EditTextPreference(
                title = stringResource(R.string.pref_proxy_url_title),
                value = proxyUrl,
                onValueChange = onProxyUrlChange
            )
        }
    }
}
