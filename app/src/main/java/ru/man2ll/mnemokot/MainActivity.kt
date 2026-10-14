package ru.man2ll.mnemokot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dagger.hilt.android.AndroidEntryPoint
import ru.man2ll.mnemokot.core.database.domain.session.SessionManager
import ru.man2ll.mnemokot.ui.auth.AuthScreen
import ru.man2ll.mnemokot.ui.auth.AuthViewModel
import ru.man2ll.mnemokot.ui.theme.MnemoKotTheme
import ru.man2ll.mnemokot.ui.vault.VaultScreen
import ru.man2ll.mnemokot.ui.vault.VaultViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val vaultViewModel: VaultViewModel by viewModels()

    @Inject
    late val sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // МЕХАНИЗМ АВТОБЛОКИРОВКИ ПРИ СВЕРНУТОМ ПРИЛОЖЕНИИ
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                sessionManager.lockSession() // Сворачивание приложения мгновенно закрывает сейф
            }
        })

        setContent {
            MnemoKotTheme {
                val isAuthorized by sessionManager.isAuthorized.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isAuthorized) {
                        AuthScreen(viewModel = authViewModel, onAuthSuccess = {})
                    } else {
                        MainNavigationDrawerContainer(vaultViewModel = vaultViewModel, sessionManager = sessionManager)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationDrawerContainer(
    vaultViewModel: VaultViewModel,
    sessionManager: SessionManager
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Управление сейфом", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                Divider()
                NavigationDrawerItem(
                    label = { Text("Журнал паролей") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; androidx.compose.runtime.rememberCoroutineScope().launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Lock, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Профили", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelLarge)
                NavigationDrawerItem(
                    label = { Text("Сменить профиль / Выйти") },
                    selected = false,
                    onClick = { sessionManager.logout() },
                    icon = { Icon(Icons.Default.Menu, contentDescription = null) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("MnemoKot") },
                    navigationIcon = {
                        IconButton(onClick = { kotlinx.coroutines.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                when (selectedTab) {
                    0 -> VaultScreen(viewModel = vaultViewModel)
                }
            }
        }
    }
}
