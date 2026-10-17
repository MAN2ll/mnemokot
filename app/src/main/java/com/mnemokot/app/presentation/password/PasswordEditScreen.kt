package com.mnemokot.app.presentation.password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mnemokot.app.presentation.mnemonic.MnemonicGeneratorDialog
import com.mnemokot.app.presentation.mnemonic.MnemonicGeneratorViewModel
import com.mnemokot.app.presentation.random.RandomGeneratorDialog
import com.mnemokot.app.presentation.random.RandomGeneratorViewModel

@Composable
fun PasswordEditScreen(
    mnemonicViewModel: MnemonicGeneratorViewModel,
    randomViewModel: RandomGeneratorViewModel,
    modifier: Modifier = Modifier
) {
    var passwordInput by remember { mutableStateOf("") }
    var showRandomDialog by remember { mutableStateOf(false) }
    var showMnemonicDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            label = { Text("Пароль *") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    IconButton(onClick = { showRandomDialog = true }) {
                        Text("🎲")
                    }
                    IconButton(onClick = { showMnemonicDialog = true }) {
                        Text("🐈")
                    }
                }
            }
        )
    }

    if (showRandomDialog) {
        RandomGeneratorDialog(
            viewModel = randomViewModel,
            onDismissRequest = { showRandomDialog = false },
            onPasswordApplied = { generatedPassword ->
                passwordInput = generatedPassword
            }
        )
    }

    if (showMnemonicDialog) {
        MnemonicGeneratorDialog(
            viewModel = mnemonicViewModel,
            onDismissRequest = { showMnemonicDialog = false },
            onPasswordApplied = { generatedPassword ->
                passwordInput = generatedPassword
            }
        )
    }
}
