package com.adrian.minimallauncher
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.adrian.minimallauncher.ui.LauncherScreen
import com.adrian.minimallauncher.viewmodel.LauncherViewModel
class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LauncherScreen(viewModel) }
    }
}
