package ru.man2ll.mnemokot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import ru.man2ll.mnemokot.ui.auth.AuthScreen
import ru.man2ll.mnemokot.ui.auth.AuthViewModel
import ru.man2ll.mnemokot.ui.theme.MnemoKotTheme
import ru.man2ll.mnemokot.ui.vault.VaultScreen
import ru.man2ll.mnemokot.ui.vault.VaultViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()
    private val vaultViewModel: VaultViewModel by viewModels()

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
                        // После успешного ввода PIN-кода открывается наш журнал паролей
                        VaultScreen(viewModel = vaultViewModel)
                    }
                }
            }
        }
    }
}
