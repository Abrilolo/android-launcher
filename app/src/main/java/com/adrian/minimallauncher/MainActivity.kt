package com.adrian.minimallauncher
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.adrian.minimallauncher.feature.exercise.ExerciseViewModel
import com.adrian.minimallauncher.ui.LauncherScreen
import com.adrian.minimallauncher.viewmodel.LauncherViewModel
class MainActivity : FragmentActivity() {
    private val viewModel: LauncherViewModel by viewModels()
    private val exerciseViewModel: ExerciseViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LauncherScreen(viewModel, exerciseViewModel) }
    }

    // Home button or gesture while the launcher is already showing: go back to the main page.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) viewModel.onHomePressed()
    }
}
