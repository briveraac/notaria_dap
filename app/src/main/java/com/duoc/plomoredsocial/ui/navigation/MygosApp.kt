package com.duoc.plomoredsocial.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.duoc.plomoredsocial.R
import com.duoc.plomoredsocial.ui.contact.ContactScreen
import com.duoc.plomoredsocial.viewmodel.ContactViewModel

// Navigation Compose administra la pila; el ViewModel conserva el formulario de MAIN-003.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MygosApp(contactViewModel: ContactViewModel) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val destination = MygosDestination.entries.firstOrNull { it.route == entry?.destination?.route }
        ?: MygosDestination.HOME
    val showBottomBar = destination in MygosDestination.main

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!showBottomBar) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    title = { Text(stringResource(destination.title)) },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.testTag("navigation_back")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                stringResource(R.string.nav_back)
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            // La barra solo aparece en las cuatro secciones: queda oculta en acceso y registro.
            if (showBottomBar) {
                // Superficie blanca y selección lavanda, con las mismas rutas de MAIN-006.
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    NavigationBar(
                        modifier = Modifier.testTag("bottom_navigation"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        MygosDestination.main.forEach { item ->
                            NavigationBarItem(
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                selected = destination == item,
                                modifier = Modifier.testTag("nav_${item.route}"),
                                onClick = {
                                    navController.navigate(item.route) {
                                        // Evita duplicados y recupera el estado al cambiar de sección.
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = when (item) {
                                            MygosDestination.HOME -> Icons.Filled.Home
                                            MygosDestination.SEARCH -> Icons.Filled.Search
                                            MygosDestination.ACTIVITIES -> Icons.Filled.DateRange
                                            else -> Icons.Filled.Person
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = { Text(stringResource(item.title)) }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = MygosDestination.HOME.route,
            modifier = Modifier.padding(padding)
        ) {
            MygosDestination.entries.forEach { item ->
                composable(item.route) {
                    if (item == MygosDestination.CONTACT) {
                        // Reutiliza íntegramente la pantalla, validaciones, modal y guardado existentes.
                        ContactScreen(viewModel = contactViewModel)
                    } else {
                        TemporaryScreen(item) { next ->
                            navController.navigate(next.route) { launchSingleTop = true }
                        }
                    }
                }
            }
        }
    }
}
