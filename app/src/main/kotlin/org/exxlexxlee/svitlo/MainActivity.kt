package org.exxlexxlee.svitlo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.exxlexxlee.svitlo.ui.SvitloNav
import org.exxlexxlee.svitlo.ui.theme.SvitloTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SvitloTheme {
                SvitloNav()
            }
        }
    }
}
