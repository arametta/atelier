package com.aistudio.atelier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.aistudio.atelier.ui.screens.MainScreen
import com.aistudio.atelier.ui.theme.MyApplicationTheme
import com.aistudio.atelier.ui.viewmodel.FragranceViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FragranceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
