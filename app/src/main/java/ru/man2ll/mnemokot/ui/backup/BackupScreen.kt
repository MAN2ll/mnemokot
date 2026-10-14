package ru.man2ll.mnemokot.ui.backup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BackupScreen(viewModel: BackupViewModel) {
    val backupPayload by viewModel.backupPayload.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var passwordInput by remember { mutableStateOf("") }
    var importPayloadInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Резервное копирование v3", 
            fontSize = 22.sp, 
            fontWeight = FontWeight.Bold, 
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Пароль для шифрования/дешифрования бэкапа") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.exportData(passwordInput) }, 
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Экспорт данных активного профиля")
        }

        statusMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
        }

        backupPayload?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Код вашей зашифрованной копии (AES-256-GCM):", 
                fontSize = 12.sp, 
                color = MaterialTheme.colorScheme.secondary
            )
            OutlinedTextField(
                value = it, 
                onValueChange = {}, 
                modifier = Modifier.fillMaxWidth().height(100.dp), 
                readOnly = true
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Импорт резервной копии", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = importPayloadInput,
            onValueChange = { importPayloadInput = it },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            label = { Text("Вставьте зашифрованный код бэкапа") }
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = { viewModel.importData(importPayloadInput, passwordInput) }, 
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Запустить импорт в текущий профиль")
        }
    }
}
