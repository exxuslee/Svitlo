package org.exxlexxlee.svitlo.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.exxlexxlee.svitlo.R
import org.exxlexxlee.svitlo.features.address.AddressPalette
import org.exxlexxlee.svitlo.features.settings.models.Action
import org.exxlexxlee.svitlo.features.settings.models.Event
import org.exxlexxlee.svitlo.features.settings.models.ViewState
import org.exxlexxlee.svitlo.navigation.Routes
import org.exxlexxlee.svitlo.ui.common.LocalNavController
import org.exxlexxlee.svitlo.ui.theme.SvitloTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    navController: NavController = LocalNavController.current,
) {
    val viewState by viewModel.viewStates().collectAsState()
    val viewAction by viewModel.viewActions().collectAsState(initial = null)

    when (viewAction) {
        Action.NavigateToAbout -> {
            viewModel.clearAction()
            navController.navigate(Routes.About.route)
        }
        Action.NavigateBack -> {
            viewModel.clearAction()
            navController.popBackStack()
        }
        null -> {}
    }

    SettingsContent(
        viewState = viewState,
        eventHandler = viewModel::obtainEvent,
    )
}

@Composable
fun SettingsContent(
    viewState: ViewState,
    eventHandler: (Event) -> Unit,
) {
    val isDark = viewState.isDarkTheme
    val backgroundColor = if (isDark) Color(0xFF121418) else AddressPalette.Background
    val cardColor = if (isDark) Color(0xFF1E2128) else Color.White
    val titleColor = if (isDark) Color(0xFFF0F2F5) else AddressPalette.Title
    val subtitleColor = if (isDark) Color(0xFF8B95A7) else AddressPalette.Subtitle
    val dividerColor = if (isDark) Color(0xFF2A2E38) else Color(0xFFEEF0F4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { eventHandler(Event.BackClicked) },
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = stringResource(R.string.back),
                    tint = titleColor,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                modifier = Modifier.weight(1f),
                color = titleColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                // Theme toggle row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { eventHandler(Event.DarkThemeChanged(!viewState.isDarkTheme)) }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF2B2F3A) else AddressPalette.AccentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                            contentDescription = null,
                            tint = AddressPalette.Accent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_dark_theme),
                            color = titleColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.settings_dark_theme_desc),
                            color = subtitleColor,
                            fontSize = 13.sp,
                        )
                    }
                    Switch(
                        checked = viewState.isDarkTheme,
                        onCheckedChange = { eventHandler(Event.DarkThemeChanged(it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AddressPalette.Accent,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = AddressPalette.Track,
                        ),
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 1.dp,
                    color = dividerColor,
                )

                // About app row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { eventHandler(Event.AboutClicked) }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF2B2F3A) else AddressPalette.AccentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = AddressPalette.Accent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_about),
                            color = titleColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.about_description),
                            color = subtitleColor,
                            fontSize = 13.sp,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = subtitleColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        // App version centered at bottom
        Text(
            text = stringResource(R.string.settings_version, viewState.appVersion),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            color = subtitleColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContent_Preview() {
    SvitloTheme {
        SettingsContent(
            viewState = ViewState(
                isDarkTheme = false,
                appVersion = "1.0",
            ),
            eventHandler = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContent_Dark_Preview() {
    SvitloTheme(darkTheme = true) {
        SettingsContent(
            viewState = ViewState(
                isDarkTheme = true,
                appVersion = "1.0",
            ),
            eventHandler = {},
        )
    }
}
