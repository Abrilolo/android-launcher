package com.adrian.minimallauncher.viewmodel
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adrian.minimallauncher.data.AppRepository
import com.adrian.minimallauncher.data.LauncherApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val _apps = MutableStateFlow<List<LauncherApp>>(emptyList())
    val apps: StateFlow<List<LauncherApp>> = _apps
    init { refreshApps() }
    fun refreshApps() { viewModelScope.launch(Dispatchers.IO) { _apps.value = repository.loadApps() } }
    fun openApp(packageName: String) = repository.openApp(packageName)
}
