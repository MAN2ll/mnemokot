package ru.man2ll.mnemokot.ui.backup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val statusMessage by viewModel.statusMessage.collectAsState()
    val backupPayload by viewModel.backupPayload.collectAsState()

    val passBuffer = remember { CharArray(128) { '\u0000' } }
    var passLength by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Резервное копирование", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Резервное копирование v3", fontSize = 20.sp, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = String(passBuffer, 0, passLength),
                onValueChange = { input ->
                    passLength = input.length.coerceAtMost(128)
                    for (i in 0 until passLength) passBuffer[i] = input[i]
                },
                label = { Text("Мастер-пароль резервной копии") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val secretPass = CharArray(passLength) { passBuffer[it] }
                    viewModel.exportData(secretPass)
                    secretPass.fill('\u0000')
                },
                enabled = passLength > 0,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Экспортировать зашифрованный бэкап")
            }

            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(statusMessage!!, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
