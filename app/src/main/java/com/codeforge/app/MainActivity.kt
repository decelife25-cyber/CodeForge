package com.codeforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.codeforge.app.ui.AppNavHost
import com.codeforge.app.ui.theme.CodeForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CodeForgeTheme {
                AppNavHost()
            }
        }
    }
}
