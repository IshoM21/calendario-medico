package com.codigomoo.calendariomedico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.codigomoo.calendariomedico.presentation.navigation.AppNavGraph
import com.codigomoo.calendariomedico.ui.theme.CalendarioMedicoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalendarioMedicoTheme {
                AppNavGraph(modifier = Modifier.fillMaxSize())
            }
        }
    }
}