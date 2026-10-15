package ru.man2ll.mnemokot

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.activity.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.man2ll.mnemokot.core.database.domain.session.SessionManager
import ru.man2ll.mnemokot.ui.auth.AuthScreen
import ru.man2ll.mnemokot.ui.auth.AuthViewModel
import ru.man2ll.mnemokot.ui.auth.MasterAuthScreen
import ru.man2ll.mnemokot.ui.auth.MasterAuthViewModel
import ru.man2ll.mnemokot.ui.theme.MnemoKotTheme
import ru.man2ll.mnemokot.ui.vault.VaultScreen
import ru.man2ll.mnemokot.ui.vault.VaultViewModel
import ru.man2ll.mnemokot.ui.audit.AuditScreen
import ru.man2ll.mnemokot.ui.audit.AuditViewModel
import ru.man2ll.mnemokot.ui.backup.BackupScreen
import ru.man2ll.mnemokot.ui.backup.BackupViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() { // Изменили для аппаратной биометрии

    private val masterViewModel: MasterAuthViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val vaultViewModel: VaultViewModel by viewModels()
    private val auditViewModel: AuditViewModel by viewModels()
    private val backupViewModel: BackupViewModel by viewModels()

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                sessionManager.lockSession()
            }
        })

        setContent {
            MnemoKotTheme {
                var isMasterAuthorized by remember { mutableStateOf(false) }
                val isAuthorized by sessionManager.isAuthorized.collectAsState()

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (!isMasterAuthorized) {
                        // Рубеж №1: Глобальный Мастер-пароль / Отпечаток
                        MasterAuthScreen(viewModel = masterViewModel, onAccessGranted = { isMasterAuthorized = true })
                    } else if (!isAuthorized) {
                        // Рубеж №2: Внутрисейфовые изолированные профили
                        AuthScreen(viewModel = authViewModel, onAuthSuccess = {})
                    } else {
                        MainNavigationDrawerContainer(vaultViewModel, auditViewModel, backupViewModel, sessionManager)
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
    auditViewModel: AuditViewModel,
    backupViewModel: BackupViewModel,
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
                Text(
                    text = "Управление сейфом", 
                    modifier = Modifier.padding(16.dp), 
                    style = MaterialTheme.typography.titleMedium
                )
                HorizontalDivider()
                
                NavigationDrawerItem(
                    label = { Text("Журнал паролей") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Аудит безопасности") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Shield, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Резервное копирование") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Save, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider()
                
                NavigationDrawerItem(
                    label = { Text("Сменить профиль / Выйти") },
                    selected = false,
                    onClick = { sessionManager.logout() },
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    ) {
        val titleText = when (selectedTab) {
            0 -> "Журнал паролей"
            1 -> "Аудит безопасности"
            2 -> "Резервное копирование"
            else -> "MnemoKot"
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(titleText, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
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
}
