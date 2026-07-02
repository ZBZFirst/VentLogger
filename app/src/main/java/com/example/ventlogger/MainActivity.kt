package com.example.ventlogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ventlogger.data.AppRepository
import com.example.ventlogger.data.local.AppDatabase
import com.example.ventlogger.ui.VentLoggerViewModel
import com.example.ventlogger.ui.screens.AddEditScreen
import com.example.ventlogger.ui.screens.LogScreen
import com.example.ventlogger.ui.screens.ReviewScreen
import com.example.ventlogger.ui.screens.SettingsScreen
import com.example.ventlogger.ui.theme.VentLoggerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val database = AppDatabase.getDatabase(context)
            val repository = AppRepository(context, database.encounterDao())
            val viewModel: VentLoggerViewModel = viewModel(
                factory = VentLoggerViewModel.Factory(repository)
            )
            var themeMode by rememberSaveable { mutableStateOf(AppThemeMode.SYSTEM) }
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            VentLoggerTheme(darkTheme = darkTheme) {
                VentLoggerApp(
                    viewModel = viewModel,
                    themeMode = themeMode,
                    onThemeModeChange = { themeMode = it }
                )
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun VentLoggerApp(
    viewModel: VentLoggerViewModel = viewModel(),
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onThemeModeChange: (AppThemeMode) -> Unit = {}
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.ADD_EDIT) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = {
                        if (it == AppDestinations.LOG) {
                            viewModel.startNewLogFromCurrentTemplate()
                        }
                        currentDestination = it
                    }
                )
            }
        }
    ) {
        val modifier = Modifier.fillMaxSize()
        when (currentDestination) {
            AppDestinations.ADD_EDIT -> AddEditScreen(
                viewModel = viewModel,
                onStartLogging = {
                    viewModel.startNewLogFromCurrentTemplate()
                    currentDestination = AppDestinations.LOG
                },
                modifier = modifier
            )
            AppDestinations.LOG -> LogScreen(viewModel, modifier)
            AppDestinations.REVIEW -> ReviewScreen(
                viewModel = viewModel,
                onEditEncounter = { currentDestination = AppDestinations.LOG },
                modifier = modifier
            )
            AppDestinations.SETTINGS -> SettingsScreen(
                viewModel = viewModel,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                modifier = modifier
            )
        }
    }
}

enum class AppThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    ADD_EDIT("Add/Edit", R.drawable.ic_account_box),
    LOG("Log", R.drawable.ic_home),
    REVIEW("Review", R.drawable.ic_favorite),
    SETTINGS("Settings", R.drawable.ic_account_box),
}
