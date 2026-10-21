package ru.man2ll.mnemokot.presentation.screens.generator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.man2ll.mnemokot.presentation.mnemonic.MnemonicGeneratorDialog
import ru.man2ll.mnemokot.presentation.mnemonic.MnemonicGeneratorViewModel
import ru.man2ll.mnemokot.presentation.random.RandomGeneratorDialog
import ru.man2ll.mnemokot.presentation.random.RandomGeneratorViewModel

@Composable
fun GeneratorScreen(
    modifier: Modifier = Modifier
) {
    val mnemonicViewModel: MnemonicGeneratorViewModel = viewModel()
    val randomViewModel: RandomGeneratorViewModel = viewModel()

    val displayBuffer = remember { CharArray(128) { '\u0000' } }
    var displayLength by remember { mutableStateOf(0) }

    var showRandomDialog by remember { mutableStateOf(false) }
    var showMnemonicDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Создание пароля",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = String(displayBuffer, 0, displayLength),
                onValueChange = { input ->
                    displayLength = input.length.coerceAtMost(128)
                    for (i in 0 until displayLength) displayBuffer[i] = input[i]
                },
                label = { Text("Пароль *") },
                visualTransformation = PasswordVisualTransformation(),
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

            if (showRandomDialog) {
                RandomGeneratorDialog(
                    viewModel = randomViewModel,
                    onDismissRequest = { showRandomDialog = false },
                    onPasswordApplied = { approvedPassword ->
                        displayLength = approvedPassword.size.coerceAtMost(128)
                        for (i in 0 until displayLength) displayBuffer[i] = approvedPassword[i]
                    }
                )
            }

            if (showMnemonicDialog) {
                MnemonicGeneratorDialog(
                    viewModel = mnemonicViewModel,
                    onDismissRequest = { showMnemonicDialog = false },
                    onPasswordApplied = { approvedPassword ->
                        displayLength = approvedPassword.size.coerceAtMost(128)
                        for (i in 0 until displayLength) displayBuffer[i] = approvedPassword[i]
                    }
                )
            }
        }
    }
}
