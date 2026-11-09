package org.exxlexxlee.svitlo.features.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import org.exxlexxlee.svitlo.ui.common.LocalNavController
import org.exxlexxlee.svitlo.ui.theme.LocalDarkTheme
import org.exxlexxlee.svitlo.ui.theme.SvitloTheme

@Composable
fun AboutScreen(
    navController: NavController = LocalNavController.current,
) {
    val context = LocalContext.current
    val appVersion = remember(context) { resolveAppVersion(context) }
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
    val isDark = LocalDarkTheme.current
    val backgroundColor = if (isDark) Color(0xFF121418) else AddressPalette.Background
    val cardColor = if (isDark) Color(0xFF1E2128) else Color.White
    val titleColor = if (isDark) Color(0xFFF0F2F5) else AddressPalette.Title
    val subtitleColor = if (isDark) Color(0xFF8B95A7) else AddressPalette.Subtitle

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

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDark) Color(0xFF2B2F3A) else AddressPalette.AccentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = stringResource(R.string.app_name),
                    tint = AddressPalette.Accent,
                    modifier = Modifier.size(56.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.app_name),
                color = titleColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.settings_version, appVersion),
                color = subtitleColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.about_description),
                    modifier = Modifier.padding(20.dp),
                    color = titleColor,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun resolveAppVersion(context: Context?): String {
    return try {
        val pInfo = context?.packageManager?.getPackageInfo(context.packageName, 0)
        pInfo?.versionName ?: "1.0"
    } catch (_: Throwable) {
        "1.0"
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
