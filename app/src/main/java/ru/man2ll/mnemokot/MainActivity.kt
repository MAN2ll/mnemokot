package ru.man2ll.mnemokot

import android.os.Bundle
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dagger.hilt.android.AndroidEntryPoint
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
                        // Рубеж №1: Глобальный Мастер-пароль / Отпечаток
                        MasterAuthScreen(viewModel = masterViewModel, onAccessGranted = { isMasterAuthorized = true })
                    } else if (!isAuthorized) {
                        // Рубеж №2: Внутрисейфовые изолированные профили
                        AuthScreen(viewModel = authViewModel, onAuthSuccess = {})
                    } else {
                        // ИСПРАВЛЕНИЕ: Вместо черного окна запускаем полноценный контейнер журналов паролей
                        MainNavigationDrawerContainer(vaultViewModel, auditViewModel, backupViewModel, sessionManager)
                    }
                }
            }
        }
    }
}
