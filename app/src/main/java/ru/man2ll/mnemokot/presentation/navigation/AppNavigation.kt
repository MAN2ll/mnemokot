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
import ru.man2ll.mnemokot.ui.vault.VaultScreen
import ru.man2ll.mnemokot.ui.vault.VaultViewModel
import ru.man2ll.mnemokot.presentation.screens.generator.GeneratorScreen
import ru.man2ll.mnemokot.ui.audit.AuditScreen
import ru.man2ll.mnemokot.ui.audit.AuditViewModel
import ru.man2ll.mnemokot.ui.backup.BackupScreen
import ru.man2ll.mnemokot.ui.backup.BackupViewModel

object Routes {
    const val MASTER_AUTH = "master_auth_route"
    const val AUTH = "auth_route"
    const val VAULT = "vault_route"
    const val GENERATOR = "generator_route"
    const val AUDIT = "audit_route"
    const val BACKUP = "backup_route"
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
                },
                onExitApp = {
                    // Закрытие приложения через finish()
                    android.os.Process.killProcess(android.os.Process.myPid())
                }
            )
        }

        composable(Routes.AUTH) {
            val authViewModel: AuthViewModel = hiltViewModel()
            AuthScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate(Routes.VAULT) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.VAULT) {
            val vaultViewModel: VaultViewModel = hiltViewModel()
            VaultScreen(
                viewModel = vaultViewModel,
                onNavigateToGenerator = {
                    navController.navigate(Routes.GENERATOR)
                },
                onNavigateToAudit = {
                    navController.navigate(Routes.AUDIT)
                },
                onNavigateToBackup = {
                    navController.navigate(Routes.BACKUP)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.GENERATOR) {
            GeneratorScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.AUDIT) {
            val auditViewModel: AuditViewModel = hiltViewModel()
            AuditScreen(
                viewModel = auditViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.BACKUP) {
            val backupViewModel: BackupViewModel = hiltViewModel()
            BackupScreen(
                viewModel = backupViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
