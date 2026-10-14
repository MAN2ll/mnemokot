package ru.man2ll.mnemokot.ui.vault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.man2ll.mnemokot.core.database.domain.model.Account

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(viewModel: VaultViewModel) {
    val accounts by viewModel.accounts.collectAsState()
    val calculatedPassword by viewModel.calculatedPassword.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showPhraseDialogForAccountId by remember { mutableStateOf<Long?>(null) }
    var mnemonicPhraseInput by remember { mutableStateOf("") }

    // Переменная для отслеживания, является ли текущий просматриваемый аккаунт двухпользовательским
    var isCurrentAccountDualMode by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }, 
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить аккаунт", tint = Color.Black)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (accounts.isEmpty()) {
                Text(
                    text = "Журнал пуст. Нажмите +, чтобы добавить первую запись.",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp), 
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(accounts) { account ->
                        AccountItem(
                            account = account,
                            onShowPasswordClick = { 
                                isCurrentAccountDualMode = account.isDualUserMode
                                showPhraseDialogForAccountId = account.id 
                            },
                            onDeleteClick = { viewModel.onDeleteAccount(account.id) }
                        )
                    }
                }
            }

            // Диалог отображения вычисленного пароля (С поддержкой Dual-User разделения)
            calculatedPassword?.let { password ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearCalculatedPassword() },
                    title = { Text(if (isCurrentAccountDualMode) "Разделенный секрет (Режим 2-х пользователей)" else "Ваш безопасный пароль") },
                    text = {
                        Column {
                            if (isCurrentAccountDualMode && password.length >= 16) {
                                val halfLength = password.length / 2
                                val part1 = password.substring(0, halfLength)
                                val part2 = password.substring(halfLength)
                                
                                Text("Часть 1 (Пользователь А):", fontSize = 12.sp, color = Color.Gray)
                                Text(part1, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Часть 2 (Пользователь Б):", fontSize = 12.sp, color = Color.Gray)
                                Text(part2, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            } else {
                                Text(password, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.clearCalculatedPassword() }) { Text("Закрыть") }
                    }
                )
            }

            // Диалог запроса мнемофразы для расчета пароля
            showPhraseDialogForAccountId?.let { accountId ->
                AlertDialog(
                    onDismissRequest = { showPhraseDialogForAccountId = null; mnemonicPhraseInput = "" },
                    title = { Text("Ввод мнемонической фразы") },
                    text = {
                        OutlinedTextField(
                            value = mnemonicPhraseInput,
                            onValueChange = { mnemonicPhraseInput = it },
                            label = { Text("Введите секретную фразу") }
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.onCalculatePassword(accountId, mnemonicPhraseInput.takeIf { it.isNotEmpty() })
                            showPhraseDialogForAccountId = null
                            mnemonicPhraseInput = ""
                        }) { Text("Вычислить") }
                    }
                )
            }

            // Диалог добавления новой записи
            if (showAddDialog) {
                AddAccountDialog(
                    onDismiss = { showAddDialog = false },
                    onSave = { service, login, isMnemonic, len, salt, date, explicit, isDual ->
                        viewModel.onAddAccount(service, login, isMnemonic, len, salt, date, explicit, isDual)
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit, 
    onSave: (String, String, Boolean, Int, String, String, String?, Boolean) -> Unit
) {
    var service by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var isMnemonic by remember { mutableStateOf(true) }
    var length by remember { mutableStateOf("16") }
    var salt by remember { mutableStateOf("") }
    var rotationDate by remember { mutableStateOf("") }
    var explicitPass by remember { mutableStateOf("") }
    var isDualUserMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая учетная запись") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { 
                    OutlinedTextField(
                        value = service, 
                        onValueChange = { service = it }, 
                        modifier = Modifier.fillMaxWidth(), 
                        label = { Text("Название сервиса (например, Gmail)") }
                    ) 
                }
                item { 
                    OutlinedTextField(
                        value = login, 
                        onValueChange = { login = it }, 
                        modifier = Modifier.fillMaxWidth(), 
                        label = { Text("Логин / Email") }
                    ) 
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isMnemonic, onCheckedChange = { isMnemonic = it })
                        Text("Использовать мнемокод AMPG v2")
                    }
                }
                if (isMnemonic) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Switch(checked = isDualUserMode, onCheckedChange = { isDualUserMode = it })
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Режим 2-х пользователей", fontWeight = FontWeight.Medium)
                                Text("Пароль разделится на две равные части", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                    item { 
                        OutlinedTextField(
                            value = length, 
                            onValueChange = { length = it }, 
                            modifier = Modifier.fillMaxWidth(), 
                            label = { 
                                Text(
                                    text = if (isDualUserMode) {
                                        "Длина пароля (16, 18 или 20)"
                                    } else {
                                        "Длина пароля"
                                    }
                                ) 
                            }
                        ) 
                    }
                    item { 
                        OutlinedTextField(
                            value = salt, 
                            onValueChange = { salt = it }, 
                            modifier = Modifier.fillMaxWidth(), 
                            label = { Text("Своя соль (необязательно)") }
                        ) 
                    }
                    item { 
                        OutlinedTextField(
                            value = rotationDate, 
                            onValueChange = { rotationDate = it }, 
                            modifier = Modifier.fillMaxWidth(), 
                            label = { Text("Дата ротации ММГГ (например, 1026)") }
                        ) 
                    }
                } else {
                    item { 
                        OutlinedTextField(
                            value = explicitPass, 
                            onValueChange = { explicitPass = it }, 
                            modifier = Modifier.fillMaxWidth(), 
                            label = { Text("Введите явный пароль") }
                        ) 
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        service, 
                        login, 
                        isMnemonic, 
                        length.toIntOrNull() ?: 16, 
                        salt, 
                        rotationDate, 
                        explicitPass.takeIf { it.isNotEmpty() }, 
                        isDualUserMode
                    )
                }
            ) { 
                Text("Сохранить") 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Отмена") 
            }
        }
    )
}
