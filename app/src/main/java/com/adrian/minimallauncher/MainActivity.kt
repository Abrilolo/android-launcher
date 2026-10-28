package com.adrian.minimallauncher
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.adrian.minimallauncher.data.routine.ActionKind
import com.adrian.minimallauncher.feature.exercise.ExerciseViewModel
import com.adrian.minimallauncher.ui.ForcedEntryScreen
import com.adrian.minimallauncher.ui.LauncherScreen
import com.adrian.minimallauncher.ui.RoutineHost
import com.adrian.minimallauncher.viewmodel.LauncherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
class MainActivity : FragmentActivity() {
    private val viewModel: LauncherViewModel by viewModels()
    private val exerciseViewModel: ExerciseViewModel by viewModels()
    // The routine test panel (placeholder), opened with EXTRA_OPEN = "routine".
    private var showRoutine by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        showRoutine = intent.getStringExtra(EXTRA_OPEN) == OPEN_ROUTINE
        setContent {
            // A pending payment or salida's registration covers the launcher until it is saved.
            val forced by viewModel.forcedState.collectAsState()
            when {
                forced != null -> ForcedEntryScreen(forced!!, viewModel)
                showRoutine -> RoutineHost(viewModel) { showRoutine = false }
                else -> LauncherScreen(viewModel, exerciseViewModel)
            }
        }
        // A phase that starts while the launcher is open runs its screen actions here.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.phaseEntered.collect { phase ->
                    if (phase.actions.any { it.kind.kind == ActionKind.WHATSAPP }) {
                        // Salida first asks to register what is pending; the chat opens once that is saved.
                        delay(500)
                        viewModel.forcedState.first { it == null }
                        viewModel.openWhatsAppFor(phase)
                    }
                }
            }
        }
    }

    // Home button or gesture while the launcher is already showing: go back to the main page.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getStringExtra(EXTRA_OPEN) == OPEN_ROUTINE) showRoutine = true
        else if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            showRoutine = false
            viewModel.onHomePressed()
        }
    }

    companion object {
        const val EXTRA_OPEN = "open"
        const val OPEN_ROUTINE = "routine"
    }
}
