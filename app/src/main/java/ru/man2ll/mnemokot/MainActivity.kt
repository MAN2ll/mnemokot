package ru.man2ll.mnemokot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import ru.man2ll.mnemokot.ui.auth.AuthScreen
import ru.man2ll.mnemokot.ui.auth.AuthViewModel
import ru.man2ll.mnemokot.ui.theme.MnemoKotTheme
import ru.man2ll.mnemokot.ui.vault.VaultScreen
import ru.man2ll.mnemokot.ui.vault.VaultViewModel
import ru.man2ll.mnemokot.ui.audit.AuditScreen
import ru.man2ll.mnemokot.ui.audit.AuditViewModel
import ru.man2ll.mnemokot.ui.backup.BackupScreen
import ru.man2ll.mnemokot.ui.backup.BackupViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()
    private val vaultViewModel: VaultViewModel by viewModels()
    private val auditViewModel: AuditViewModel by viewModels()
    private val backupViewModel: BackupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MnemoKotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isAuthenticated by remember { mutableStateOf(false) }

                    if (!isAuthenticated) {
                        AuthScreen(
                            viewModel = authViewModel,
                            onAuthSuccess = { isAuthenticated = true }
                        )
                    } else {
                        MainNavigationContainer(
                            vaultViewModel = vaultViewModel,
                            auditViewModel = auditViewModel,
                            backupViewModel = backupViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainNavigationContainer(
    vaultViewModel: VaultViewModel,
    auditViewModel: AuditViewModel,
    backupViewModel: BackupViewModel
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Журнал") },
                    label = { Text("Журнал") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Shield, contentDescription = "Аудит") },
                    label = { Text("Аудит") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Save, contentDescription = "Резерв") },
                    label = { Text("Резерв") }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (selectedTab) {
                0 -> VaultScreen(viewModel = vaultViewModel)
                1 -> AuditScreen(viewModel = auditViewModel)
                2 -> BackupScreen(viewModel = backupViewModel)
            }
        }
    }
}
