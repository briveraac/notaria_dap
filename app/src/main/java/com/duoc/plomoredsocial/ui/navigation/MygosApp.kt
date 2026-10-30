package com.duoc.plomoredsocial.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.material3.Surface
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler
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
import com.duoc.plomoredsocial.ui.activities.ActivitiesScreen
import com.duoc.plomoredsocial.viewmodel.ContactViewModel
import com.duoc.plomoredsocial.viewmodel.LoginViewModel
import com.duoc.plomoredsocial.viewmodel.HomeViewModel
import com.duoc.plomoredsocial.ui.auth.LoginScreen

// Navigation Compose administra la pila; el ViewModel conserva el formulario de MAIN-003.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MygosApp(contactViewModel: ContactViewModel, loginViewModel: LoginViewModel, homeViewModel: HomeViewModel,
    registerViewModel: com.duoc.plomoredsocial.viewmodel.RegisterViewModel) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val destination = MygosDestination.entries.firstOrNull { it.route == entry?.destination?.route }
        ?: MygosDestination.HOME
    val showBottomBar = destination in MygosDestination.main
    val loginState = loginViewModel.uiState

    // Actividades es secundaria y vuelve al catálogo que la abrió.
    BackHandler(enabled = destination == MygosDestination.ACTIVITIES) {
        if (!navController.popBackStack(MygosDestination.SEARCH.route, false)) {
            navController.navigate(MygosDestination.SEARCH.route) { launchSingleTop = true }
        }
    }

    BackHandler(enabled = destination == MygosDestination.REGISTER) {
        if (!registerViewModel.loading) {
            registerViewModel.clearForm()
            navController.popBackStack()
        }
    }

    // No abandonamos una autenticación en curso ni dejamos la contraseña al volver.
    BackHandler(enabled = destination == MygosDestination.LOGIN) {
        if (!loginState.loading) {
            loginViewModel.clearForm()
            navController.popBackStack()
        }
    }
    LaunchedEffect(loginState.user, destination) {
        if (loginState.user != null && destination == MygosDestination.LOGIN) {
            navController.navigate(MygosDestination.HOME.route) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    if (loginState.restoringSession) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!showBottomBar) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    title = { Text(stringResource(destination.title)) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (destination == MygosDestination.LOGIN) loginViewModel.clearForm()
                                if (destination == MygosDestination.REGISTER) registerViewModel.clearForm()
                                if (destination == MygosDestination.ACTIVITIES) {
                                    if (!navController.popBackStack(MygosDestination.SEARCH.route, false)) {
                                        navController.navigate(MygosDestination.SEARCH.route) { launchSingleTop = true }
                                    }
                                } else navController.popBackStack()
                            },
                            enabled = (destination != MygosDestination.LOGIN || !loginState.loading) &&
                                (destination != MygosDestination.REGISTER || !registerViewModel.loading),
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
            // La barra solo aparece en las tres secciones principales: queda oculta en acceso y registro.
            if (showBottomBar) {
                // Superficie blanca y selección lavanda.
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
                                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
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
                    when (item) {
                        MygosDestination.ADMIN -> com.duoc.plomoredsocial.ui.admin.AdminRoute(loginState.user)
                        MygosDestination.HOME -> HomeScreen(loginState.user,
                            onLogin = {
                                loginViewModel.clearForm()
                                navController.navigate(MygosDestination.LOGIN.route) { launchSingleTop = true }
                            },
                            onProfile = { navController.navigate(MygosDestination.PROFILE.route) { launchSingleTop = true } },
                            onExplore = {
                                navController.navigate(MygosDestination.SEARCH.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            })
                        MygosDestination.SEARCH -> ExploreScreen(loginState.user, homeViewModel.uiState,
                            onLogin = {
                                loginViewModel.clearForm()
                                navController.navigate(MygosDestination.LOGIN.route) { launchSingleTop = true }
                            },
                            onRegister = {
                                loginViewModel.clearForm()
                                registerViewModel.clearForm()
                                navController.navigate(MygosDestination.REGISTER.route) { launchSingleTop = true }
                            })
                        MygosDestination.LOGIN -> LoginScreen(
                            state = loginState,
                            onEmailChange = loginViewModel::updateEmail,
                            onPasswordChange = loginViewModel::updatePassword,
                            onLogin = loginViewModel::login,
                            onRegister = {
                                loginViewModel.clearForm()
                                registerViewModel.clearForm()
                                navController.navigate(MygosDestination.REGISTER.route) { launchSingleTop = true }
                            }
                        )
                        MygosDestination.CONTACT -> {
                            ContactScreen(viewModel = contactViewModel)
                        }
                        MygosDestination.ACTIVITIES -> ActivitiesScreen(user = loginState.user,
                            onLogin = {
                                loginViewModel.clearForm()
                                navController.navigate(MygosDestination.LOGIN.route) { launchSingleTop = true }
                            })
                        MygosDestination.REGISTER -> {
                            com.duoc.plomoredsocial.ui.auth.RegisterScreen(registerViewModel) {
                                val email = registerViewModel.registeredEmail ?: registerViewModel.email
                                loginViewModel.clearForm()
                                loginViewModel.updateEmail(email)
                                registerViewModel.clearForm()
                                navController.navigate(MygosDestination.LOGIN.route) {
                                    popUpTo(MygosDestination.REGISTER.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                        else -> TemporaryScreen(item, user = loginState.user,
                            logoutLoading = loginState.loading,
                            logoutFailed = loginState.logoutFailed,
                            onLogout = loginViewModel::logout) { next ->
                            if (next == MygosDestination.LOGIN) loginViewModel.clearForm()
                            if (next == MygosDestination.REGISTER) registerViewModel.clearForm()
                            navController.navigate(next.route) { launchSingleTop = true }
                        }
                    }
                }
            }
        }
    }
}
