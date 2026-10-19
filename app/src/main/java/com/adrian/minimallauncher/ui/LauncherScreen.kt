package com.adrian.minimallauncher.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.adrian.minimallauncher.ui.theme.LauncherTheme
import com.adrian.minimallauncher.viewmodel.LauncherViewModel
@Composable
fun LauncherScreen(viewModel: LauncherViewModel) {
    val apps by viewModel.apps.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    LauncherTheme {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding()
        ) { page ->
            when (page) {
                0 -> MainZone(apps.filterNot { it.isEntertainment }, viewModel::openApp)
                1 -> EntertainmentZone(apps.filter { it.isEntertainment }, viewModel::openApp)
            }
        }
    }
}
