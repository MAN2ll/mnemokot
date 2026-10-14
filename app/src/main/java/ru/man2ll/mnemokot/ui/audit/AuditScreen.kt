package ru.man2ll.mnemokot.ui.audit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.man2ll.mnemokot.core.security.audit.model.WarningType

@Composable
fun AuditScreen(viewModel: AuditViewModel) {
    val auditResult by viewModel.auditResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showDeepAuditDialog by remember { mutableStateOf(false) }
    var mnemonicInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Аудит безопасности базы", 
            fontSize = 22.sp, 
            fontWeight = FontWeight.Bold, 
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { viewModel.runShallowAudit() }, modifier = Modifier.weight(1f)) {
                Text("Поверхностный")
            }
            Button(
                onClick = { showDeepAuditDialog = true }, 
                modifier = Modifier.weight(1f), 
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Глубокий (С фразой)")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            auditResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(), 
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Результаты проверки профиля:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Всего аккаунтов проверено: ${result.totalAccountsChecked}")
                        Text(
                            text = "Повторно используемых паролей: ${result.duplicatePasswordsFound}", 
                            color = if (result.duplicatePasswordsFound > 0) Color(0xFFEF5350) else Color.Unspecified
                        )
                        Text(
                            text = "Слабых паролей: ${result.weakPasswordsFound}", 
                            color = if (result.weakPasswordsFound > 0) Color(0xFFFFEE58) else Color.Unspecified
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Предупреждения системы безопасности:", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(result.warnings) { warning ->
                        // ИСПРАВЛЕНИЕ: Переменная цвета теперь честно применяется к контейнеру Card
                        val cardColor = when (warning.type) {
                            WarningType.REUSED_PASSWORD -> Color(0xFF3A1F22) // Глубокий красный для дубликатов
                            WarningType.SHORT_PASSWORD -> Color(0xFF38341F)  // Горчично-желтый для коротких
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(), 
                            colors = CardDefaults.cardColors(containerColor = cardColor)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${warning.serviceName} (${warning.login})", 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(warning.message, fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
        // Диалог запроса мнемофразы для глубокого аудита
        if (showDeepAuditDialog) {
            AlertDialog(
                onDismissRequest = { showDeepAuditDialog = false; mnemonicInput = "" },
                title = { Text("Запрос секретной фразы") },
                text = {
                    OutlinedTextField(
                        value = mnemonicInput, 
                        onValueChange = { mnemonicInput = it }, 
                        label = { Text("Введите мастер-фразу для анализа") }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.runDeepAudit(mnemonicInput)
                            showDeepAuditDialog = false
                            mnemonicInput = ""
                        }
                    ) { Text("Запустить аудит") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeepAuditDialog = false; mnemonicInput = "" }) { 
                        Text("Отмена") 
                    }
                }
            )
        }
    }
}
