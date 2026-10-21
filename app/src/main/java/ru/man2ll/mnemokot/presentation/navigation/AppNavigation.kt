package ru.man2ll.mnemokot.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.man2ll.mnemokot.ui.auth.AuthScreen
import ru.man2ll.mnemokot.ui.auth.AuthViewModel
import ru.man2ll.mnemokot.ui.auth.MasterAuthScreen
import ru.man2ll.mnemokot.ui.auth.MasterAuthViewModel
import ru.man2ll.mnemokot.presentation.screens.generator.GeneratorScreen

object Routes {
    const val MASTER_AUTH = "master_auth_route"
    const val AUTH = "auth_route"
    const val GENERATOR = "generator_route"
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.MASTER_AUTH
    ) {
        composable(Routes.MASTER_AUTH) {
            val masterViewModel: MasterAuthViewModel = hiltViewModel()
            MasterAuthScreen(
                viewModel = masterViewModel,
                onAccessGranted = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.MASTER_AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.AUTH) {
            val authViewModel: AuthViewModel = hiltViewModel()
            AuthScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate(Routes.GENERATOR) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.GENERATOR) {
            GeneratorScreen()
        }
    }
}
