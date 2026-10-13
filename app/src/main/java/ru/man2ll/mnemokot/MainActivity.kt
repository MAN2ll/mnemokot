package ru.man2ll.mnemokot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.io.PrintWriter
import java.io.StringWriter

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    // Используем lazy-инициализацию, чтобы краш при сборке Hilt-графа 
    // не выбивал саму Activity до запуска блока try-catch
    private val authViewModel: AuthViewModel by lazy { viewModels<AuthViewModel>().value }
    private val vaultViewModel: VaultViewModel by lazy { viewModels<VaultViewModel>().value }
    private val auditViewModel: AuditViewModel by lazy { viewModels<AuditViewModel>().value }
    private val backupViewModel: BackupViewModel by lazy { viewModels<BackupViewModel>().value }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
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
        } catch (t: Throwable) {
            // ЕСЛИ ПРИЛОЖЕНИЕ ПАДАЕТ, ОНО ПОКАЖЕТ КРАСНЫЙ ЭКРАН С ЛОГОМ ОШИБКИ ПРЯМО НА СМАРТФОНЕ
            showCrashScreen(t)
        }
    }

    private fun showCrashScreen(t: Throwable) {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        val exceptionAsString = sw.toString()

        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF8B0000)) // Темно-красный фон краша
                    .padding(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Критический сбой инициализации MnemoKot", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Сделайте скриншот этого экрана для отладки:", color = Color.Yellow, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = exceptionAsString,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
