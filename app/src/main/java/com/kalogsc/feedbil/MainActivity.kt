package com.kalogsc.feedbil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.kalogsc.feedbil.ui.FeedbilNavHost
import com.kalogsc.feedbil.ui.theme.FeedbilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as FeedbilApplication).container

        setContent {
            FeedbilTheme {
                FeedbilNavHost(
                    container = container,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
