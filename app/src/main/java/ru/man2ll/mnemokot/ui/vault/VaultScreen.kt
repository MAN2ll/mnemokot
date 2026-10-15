package ru.man2ll.mnemokot.ui.vault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.man2ll.mnemokot.core.database.domain.model.Account

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(viewModel: VaultViewModel) {
    val accounts by viewModel.accounts.collectAsState()
    val calculatedPassword by viewModel.calculatedPassword.collectAsState()
    val vaultError by viewModel.vaultError.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showPinDialogForAccountId by remember { mutableStateOf<Long?>(null) }
    var pinVerificationInput by remember { mutableStateOf("") }
    var showPhraseDialogForAccountId by remember { mutableStateOf<Long?>(null) }
    var mnemonicPhraseInput by remember { mutableStateOf("") }
    var isCurrentAccountDualMode by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.Black)
            }
        }
    ) { p ->
        Box(modifier = Modifier.padding(p).fillMaxSize()) {
            if (accounts.isEmpty()) {
                Text("Журнал пуст. Нажмите +, чтобы добавить запись.", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(accounts) { acc ->
                        AccountItem(acc, onShowPasswordClick = { viewModel.clearVaultError(); pinVerificationInput = ""; showPinDialogForAccountId = acc.id }, onDeleteClick = { viewModel.onDeleteAccount(acc.id) })
                    }
                }
            }
            showPinDialogForAccountId?.let { id ->
                AlertDialog(
                    onDismissRequest = { showPinDialogForAccountId = null; pinVerificationInput = ""; viewModel.clearVaultError() },
                    title = { Text("Подтверждение операции") },
                    text = {
                        Column {
                            Text("Введите PIN-код текущего профиля:", fontSize = 14.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = pinVerificationInput, onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 6) pinVerificationInput = it }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = vaultError != null)
                            if (vaultError != null) Text(vaultError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val target = accounts.firstOrNull { it.id == id }
                            viewModel.verifyProfilePinForAction(pinVerificationInput) {
                                showPinDialogForAccountId = null; pinVerificationInput = ""
                                if (target != null) {
                                    if (target.isMnemonic) { isCurrentAccountDualMode = target.isDualUserMode; showPhraseDialogForAccountId = target.id }
                                    else { viewModel.onCalculatePassword(id, null) }
                                }
                            }
                        }) { Text("ОК") }
                    }
                )
            }
            calculatedPassword?.let { pass ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearCalculatedPassword() },
                    title = { Text(if (isCurrentAccountDualMode) "Разделенный секрет" else "Ваш пароль") },
                    text = {
                        Column {
                            if (isCurrentAccountDualMode && pass.length >= 16) {
                                val half = pass.length / 2
                                Text("Часть 1:", fontSize = 12.sp, color = Color.Gray)
                                Text(pass.substring(0, half), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Часть 2:", fontSize = 12.sp, color = Color.Gray)
                                Text(pass.substring(half), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            } else {
                                Text(pass, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    confirmButton = { Button(onClick = { viewModel.clearCalculatedPassword() }) { Text("Закрыть") } }
                )
            }
            showPhraseDialogForAccountId?.let { id ->
                AlertDialog(
                    onDismissRequest = { showPhraseDialogForAccountId = null; mnemonicPhraseInput = "" },
                    title = { Text("Ввод мнемосхемы") },
                    text = { OutlinedTextField(value = mnemonicPhraseInput, onValueChange = { mnemonicPhraseInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Мнемофраза") }) },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.onCalculatePassword(id, mnemonicPhraseInput.takeIf { it.isNotEmpty() })
                            showPhraseDialogForAccountId = null; mnemonicPhraseInput = ""
                        }) { Text("Расчет") }
                    }
                )
            }
            if (showAddDialog) {
                AddAccountDialog(onDismiss = { showAddDialog = false }, onSave = { s, l, m, len, sl, d, ex, dl -> viewModel.onAddAccount(s, l, m, len, sl, d, ex, dl); showAddDialog = false })
            }
        }
    }
}

@Composable
fun AccountItem(account: Account, onShowPasswordClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(account.serviceName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(account.login, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip(onClick = {}, label = { Text(if (account.isMnemonic) "AMPG v2" else "Явный") })
                    if (account.isDualUserMode) SuggestionChip(onClick = {}, label = { Text("2 Игрока") })
                }
            }
            Row {
                IconButton(onClick = onShowPasswordClick) { Icon(Icons.Default.Visibility, contentDescription = "Показать", tint = Color.White) }
                IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
@Composable
fun AddAccountDialog(onDismiss: () -> Unit, onSave: (String, String, Boolean, Int, String, String, String?, Boolean) -> Unit) {
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
        title = { Text("Новый аккаунт") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(value = service, onValueChange = { service = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Сервис") }) }
                item { OutlinedTextField(value = login, onValueChange = { login = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Логин") }) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isMnemonic, onCheckedChange = { isMnemonic = it })
                        Text("Генератор AMPG v2")
                    }
                }
                if (isMnemonic) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = isDualUserMode, onCheckedChange = { isDualUserMode = it })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Режим двух пользователей", fontSize = 14.sp)
                        }
                    }
                    item { OutlinedTextField(value = length, onValueChange = { length = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Длина пароля") }) }
                    item { OutlinedTextField(value = salt, onValueChange = { salt = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Соль") }) }
                    item { OutlinedTextField(value = rotationDate, onValueChange = { rotationDate = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Ротация") }) }
                } else {
                    item { OutlinedTextField(value = explicitPass, onValueChange = { explicitPass = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Пароль вручную") }) }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(service, login, isMnemonic, length.toIntOrNull() ?: 16, salt, rotationDate, explicitPass.takeIf { it.isNotEmpty() }, isDualUserMode) }) { Text("Сохранить") } }
    )
}
