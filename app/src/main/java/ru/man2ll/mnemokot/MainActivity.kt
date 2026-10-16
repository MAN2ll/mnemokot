package ru.man2ll.mnemokot

import android.os.Bundle
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
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
class MainActivity : FragmentActivity() {

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
                        MasterAuthScreen(viewModel = masterViewModel, onAccessGranted = { isMasterAuthorized = true })
                    } else if (!isAuthorized) {
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
    var currentScreen by remember { mutableStateOf("vault") }
    
    // Получаем живое имя текущего запущенного профиля сейфа
    val activeProfileId by sessionManager.activeProfileId.collectAsState()
    val accountsState by vaultViewModel.accounts.collectAsState()
    val profileName = remember(activeProfileId, accountsState) {
        if (accountsState.isNotEmpty()) "Профиль: Сейф активен" else "Внутри сейфа"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                Text("MnemoKot 🐾", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                HorizontalDivider()
                
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Shield, null) },
                    label = { Text("Журнал паролей") },
                    selected = currentScreen == "vault",
                    onClick = { currentScreen = "vault"; scope.launch { drawerState.close() } },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Lock, null) },
                    label = { Text("Аудит безопасности") },
                    selected = currentScreen == "audit",
                    onClick = { currentScreen = "audit"; scope.launch { drawerState.close() } },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Save, null) },
                    label = { Text("Резервные копии") },
                    selected = currentScreen == "backup",
                    onClick = { currentScreen = "backup"; scope.launch { drawerState.close() } },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ExitToApp, null, tint = MaterialTheme.colorScheme.error) },
                    label = { Text("Сменить профиль", color = MaterialTheme.colorScheme.error) },
                    selected = false,
                    onClick = { sessionManager.logout() },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (currentScreen) {
                                "vault" -> profileName // ИСПРАВЛЕНИЕ: Вместо "Журнал сейфа" пишем статус профиля
                                "audit" -> "Панель рисков"
                                else -> "Резервное копирование"
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                when (currentScreen) {
                    "vault" -> VaultScreen(viewModel = vaultViewModel)
                    "audit" -> AuditScreen(viewModel = auditViewModel)
                    "backup" -> BackupScreen(viewModel = backupViewModel)
                }
            }
        }
    }
}
