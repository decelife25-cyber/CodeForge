package com.cartadigital.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cartadigital.app.ui.AppNavHost
import com.cartadigital.app.ui.theme.CartaDigitalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CartaDigitalTheme {
                AppNavHost()
            }
        }
    }
}
