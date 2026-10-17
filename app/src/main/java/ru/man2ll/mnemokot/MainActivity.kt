package ru.man2ll.mnemokot

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import ru.man2ll.mnemokot.presentation.password.PasswordEditScreen
import ru.man2ll.mnemokot.presentation.mnemonic.MnemonicGeneratorViewModel
import ru.man2ll.mnemokot.presentation.random.RandomGeneratorViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.man2ll.mnemokot.ui.theme.MnemoKotTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            MnemoKotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val mnemonicVm: MnemonicGeneratorViewModel = viewModel()
                    val randomVm: RandomGeneratorViewModel = viewModel()
                    
                    PasswordEditScreen(
                        mnemonicViewModel = mnemonicVm,
                        randomViewModel = randomVm
                    )
                }
            }
        }
    }
}
