package ru.man2ll.mnemokot.ui.audit

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
fun AuditScreen(
    viewModel: AuditViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val auditResult by viewModel.auditResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val checkBuffer = remember { CharArray(128) { '\u0000' } }
    var checkLength by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Аудит безопасности", fontWeight = FontWeight.Bold) },
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
            Text("Аудит безопасности сейфов", fontSize = 20.sp, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = String(checkBuffer, 0, checkLength),
                onValueChange = { input ->
                    checkLength = input.length.coerceAtMost(128)
                    for (i in 0 until checkLength) checkBuffer[i] = input[i]
                },
                label = { Text("Введите фразу для глубокого аудита") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.runShallowAudit() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Поверхностный")
                }

                Button(
                    onClick = {
                        val finalPhrase = CharArray(checkLength) { checkBuffer[it] }
                        viewModel.runDeepAudit(finalPhrase)
                        finalPhrase.fill('\u0000')
                    },
                    enabled = checkLength > 0,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Глубокий")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else if (auditResult != null) {
                val res = auditResult!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Проверено аккаунтов: ${res.totalAccountsChecked}")
                        Text("Найдено дубликатов: ${res.duplicatePasswordsFound}")
                        Text("Скомпрометировано: ${res.weakPasswordsFound}")
                    }
                }
            }
        }
    }
}
