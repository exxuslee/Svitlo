package org.exxlexxlee.svitlo.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.exxlexxlee.svitlo.R
import org.exxlexxlee.svitlo.di.appVersion
import org.exxlexxlee.svitlo.ui.common.LocalNavController
import org.exxlexxlee.svitlo.ui.theme.AppColors
import org.exxlexxlee.svitlo.ui.theme.SvitloTheme
import org.koin.compose.koinInject

@Composable
fun AboutScreen(
    navController: NavController = LocalNavController.current,
) {
    val appVersion: String = koinInject(qualifier = appVersion)
    AboutContent(
        appVersion = appVersion,
        onBack = { navController.popBackStack() },
    )
}

@Composable
fun AboutContent(
    appVersion: String,
    onBack: () -> Unit,
) {
    val backgroundColor = AppColors.Background
    val titleColor = AppColors.Title
    val subtitleColor = AppColors.Subtitle

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
            IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = stringResource(R.string.back),
                    tint = titleColor,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = stringResource(R.string.settings_about),
                modifier = Modifier.weight(1f),
                color = titleColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val sections = listOf(
            stringResource(R.string.about_application_title) to stringResource(R.string.about_description),
            stringResource(R.string.about_how_it_works_title) to stringResource(R.string.about_how_it_works_body),
            stringResource(R.string.about_reminders_title) to stringResource(R.string.about_reminders_body),
            stringResource(R.string.about_widget_title) to stringResource(R.string.about_widget_body),
            stringResource(R.string.about_sources_title) to stringResource(R.string.about_sources_body),
            stringResource(R.string.about_privacy_title) to stringResource(R.string.about_privacy_body),
            stringResource(R.string.about_version_title) to appVersion,
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        ) {
            sections.forEach { (heading, body) ->
                stickyHeader {
                    Text(
                        text = heading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backgroundColor)
                            .padding(top = 8.dp, bottom = 8.dp),
                        color = titleColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 26.sp,
                    )
                }
                item {
                    Text(
                        text = body,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        color = subtitleColor,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutContent_Preview() {
    SvitloTheme {
        AboutContent(
            appVersion = "1.0",
            onBack = {},
        )
    }
}
