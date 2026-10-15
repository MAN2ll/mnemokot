package ru.man2ll.mnemokot.ui.vault

import androidx.compose.foundation.clickable
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
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedAccountIds by viewModel.selectedAccountIds.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showPinDialogForAccountId by remember { mutableStateOf<Long?>(null) }
    var pinVerificationInput by remember { mutableStateOf("") }
    var showPhraseDialogForAccountId by remember { mutableStateOf<Long?>(null) }
    var mnemonicPhraseInput by remember { mutableStateOf("") }
    var isCurrentAccountDualMode by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = { showAddDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.Black)
                }
            }
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).fillMaxSize()) {
            if (accounts.isNotEmpty()) {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer, tonalElevation = 2.dp) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        if (!isSelectionMode) {
                            Text("Паролей в сейфе: ${accounts.size}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Button(onClick = { viewModel.startSelectionMode() }) { Text("Выбрать для ротации", fontSize = 12.sp) }
                        } else {
                            Text("Выбрано: ${selectedAccountIds.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { viewModel.cancelSelectionMode() }) { Text("Отмена") }
                                Button(onClick = { viewModel.runSelectedPasswordRotation() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Перемешать", fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (accounts.isEmpty()) {
                    Text("Журнал пуст. Нажмите +, чтобы добавить запись.", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(accounts) { acc ->
                            AccountItem(acc, isSelectionMode, selectedAccountIds.contains(acc.id), { viewModel.toggleAccountSelection(acc.id) }, { viewModel.clearVaultError(); pinVerificationInput = ""; showPinDialogForAccountId = acc.id }, { viewModel.onDeleteAccount(acc.id) })
                        }
                    }
                }
                vaultError?.let { report ->
                    AlertDialog(onDismissRequest = { viewModel.clearVaultError() }, title = { Text("Аудит безопасности ротации", fontWeight = FontWeight.Bold) }, text = { Text(report, fontSize = 14.sp) }, confirmButton = { Button(onClick = { viewModel.clearVaultError() }) { Text("Принять риски и закрыть") } })
                }
                showPinDialogForAccountId?.let { id ->
                    AlertDialog(
                        onDismissRequest = { showPinDialogForAccountId = null; pinVerificationInput = ""; viewModel.clearVaultError() },
                        title = { Text("Подтверждение операции") },
                        text = {
                            Column {
                                Text("Введите PIN-код текущего профиля:", fontSize = 14.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = pinVerificationInput, onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 6) pinVerificationInput = it }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
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
                    AlertDialog(onDismissRequest = { showPhraseDialogForAccountId = null; mnemonicPhraseInput = "" }, title = { Text("Ввод мнемосхемы") }, text = { OutlinedTextField(value = mnemonicPhraseInput, onValueChange = { mnemonicPhraseInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Мнемофраза") }) }, confirmButton = { Button(onClick = { viewModel.onCalculatePassword(id, mnemonicPhraseInput.takeIf { it.isNotEmpty() }); showPhraseDialogForAccountId = null; mnemonicPhraseInput = "" }) { Text("Расчет") } })
                }
                if (showAddDialog) {
                    AddAccountDialog(onDismiss = { showAddDialog = false }, onSave = { s, l, m, len, sl, d, ex, dl -> viewModel.onAddAccount(s, l, m, len, sl, d, ex, dl); showAddDialog = false })
                }
            }
        }
    }
}
@Composable
fun AccountItem(account: Account, isSelectionMode: Boolean, isSelected: Boolean, onSelectToggle: () -> Unit, onShowPasswordClick: () -> Unit, onDeleteClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(modifier = Modifier.fillMaxWidth().clickable(enabled = isSelectionMode) { onSelectToggle() }, colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(account.serviceName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(account.login, fontSize = 14.sp, color = Color.Gray)
                if (account.expiresAt > 0L) {
                    val daysLeft = ((account.expiresAt - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).coerceAtLeast(0)
                    Text(text = if (daysLeft > 0) "Истекает через: $daysLeft дн." else "Пароль устарел! Требуется смена", fontSize = 12.sp, color = if (daysLeft <= 7) Color(0xFFEF5350) else Color(0xFFFFEE58), fontWeight = FontWeight.Medium)
                } else { Text("Срок действия: Вечный", fontSize = 12.sp, color = Color.LightGray) }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(onClick = {}, label = { Text(if (account.isMnemonic) "AMPG v2" else "Явный") })
                    if (account.isDualUserMode) SuggestionChip(onClick = {}, label = { Text("2 Игрока") })
                    if (account.expiresAt > 0L && !isSelectionMode) {
                        TextButton(onClick = { addPasswordRotationReminder(context, account.serviceName, account.expiresAt) }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)) { Text("⏰ В календарь", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary) }
                    }
                }
            }
            if (isSelectionMode) { Checkbox(checked = isSelected, onCheckedChange = { onSelectToggle() }) }
            else { Row { IconButton(onClick = onShowPasswordClick) { Icon(Icons.Default.Visibility, contentDescription = "Показать", tint = Color.White) }; IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error) } } }
        }
    }
}

@Composable
fun AddAccountDialog(onDismiss: () -> Unit, onSave: (String, String, Boolean, Int, String, Int, String?, Boolean) -> Unit) {
    var service by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var explicitPass by remember { mutableStateOf("") }
    var randomLength by remember { mutableStateOf("16") }
    var useUpper by remember { mutableStateOf(true) }
    var useDigits by remember { mutableStateOf(true) }
    var useSpecial by remember { mutableStateOf(false) }
    var length by remember { mutableStateOf("16") }
    var salt by remember { mutableStateOf("") }
    var isDualUserMode by remember { mutableStateOf(false) }
    var daysMenuExpanded by remember { mutableStateOf(false) }
    val daysOptions = listOf(0 to "Вечный пароль", 30 to "30 дней", 90 to "90 дней", 180 to "180 дней")
    var selectedDaysOption by remember { mutableStateOf(daysOptions[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая учетная запись", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item { OutlinedTextField(value = service, onValueChange = { service = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Название сервиса") }) }
                item { OutlinedTextField(value = login, onValueChange = { login = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Логин / Email") }) }
                item { TabRow(selectedTabIndex = selectedTab, modifier = Modifier.padding(vertical = 4.dp)) { Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Вручную", fontSize = 12.sp) }); Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Кубики", fontSize = 12.sp) }); Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Мнемо", fontSize = 12.sp) }) } }
                when (selectedTab) {
                    0 -> {
                        item { OutlinedTextField(value = explicitPass, onValueChange = { explicitPass = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Введите ваш пароль") }, visualTransformation = PasswordVisualTransformation()) }
                        item { PasswordStrengthMeter(password = explicitPass) }
                    }
                    1 -> {
                        item { OutlinedTextField(value = randomLength, onValueChange = { randomLength = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Длина случайного пароля") }) }
                        item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = useUpper, onCheckedChange = { useUpper = it }); Text("ЗАГЛАВНЫЕ БУКВЫ", fontSize = 14.sp) } }
                        item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = useDigits, onCheckedChange = { useDigits = it }); Text("Цифры (0-9)", fontSize = 14.sp) } }
                        item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = useSpecial, onCheckedChange = { useSpecial = it }); Text("Спецсимволы (%$#@)", fontSize = 14.sp) } }
                        item { Button(onClick = { explicitPass = generateRandomPassword(randomLength.toIntOrNull() ?: 16, useUpper, useDigits, useSpecial) }, modifier = Modifier.fillMaxWidth()) { Text("Сгенерировать случайный пароль") } }
                        if (explicitPass.isNotEmpty()) { item { OutlinedTextField(value = explicitPass, onValueChange = {}, modifier = Modifier.fillMaxWidth(), label = { Text("Результат генерации") }, readOnly = true) } }
                    }
                    2 -> {
                        item { Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = isDualUserMode, onCheckedChange = { isDualUserMode = it }); Spacer(modifier = Modifier.width(8.dp)); Column { Text("Режим 2-х пользователей", fontSize = 14.sp, fontWeight = FontWeight.Medium); Text("Пароль разделится пополам", fontSize = 11.sp, color = Color.Gray) } } }
                        item { OutlinedTextField(value = length, onValueChange = { length = it }, modifier = Modifier.fillMaxWidth(), label = { Text(if (isDualUserMode) "Длина (16, 18 или 20)" else "Длина пароля") }) }
                        item { OutlinedTextField(value = salt, onValueChange = { salt = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Своя соль") }) }
                    }
                }
                item { Box(modifier = Modifier.fillMaxWidth()) { OutlinedButton(onClick = { daysMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Срок действия: ${selectedDaysOption.second}") }; DropdownMenu(expanded = daysMenuExpanded, onDismissRequest = { daysMenuExpanded = false }) { daysOptions.forEach { option -> DropdownMenuItem(text = { Text(option.second) }, onClick = { selectedDaysOption = option; daysMenuExpanded = false }) } } } }
            }
        },
        confirmButton = { Button(onClick = { val isMnemonicType = (selectedTab == 2); val finalLen = if (selectedTab == 1) randomLength.toIntOrNull() ?: 16 else length.toIntOrNull() ?: 16; val finalExplicit = if (selectedTab == 0 || selectedTab == 1) explicitPass.takeIf { it.isNotEmpty() } else null; onSave(service, login, isMnemonicType, finalLen, salt, selectedDaysOption.first, finalExplicit, isDualUserMode) }) { Text("Сохранить") } }
    )
}

@Composable
fun PasswordStrengthMeter(password: String) {
    if (password.isEmpty()) return
    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    val (text, color) = when {
        score <= 2 -> "Слабый пароль" to Color(0xFFEF5350)
        score <= 4 -> "Средний пароль" to Color(0xFFFFEE58)
        else -> "Надежный пароль" to Color(0xFF66BB6A)
    }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color)
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(progress = { (score / 5f).coerceIn(0.1f, 1f) }, modifier = Modifier.fillMaxWidth().height(6.dp), color = color, trackColor = Color.DarkGray)
    }
}

fun generateRandomPassword(length: Int, useUpper: Boolean, useDigits: Boolean, useSpecial: Boolean): String {
    val lowerChars = "abcdefghijklmnopqrstuvwxyz"
    val upperChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val digitChars = "0123456789"
    val specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"
    val allowedChars = StringBuilder(lowerChars)
    if (useUpper) allowedChars.append(upperChars)
    if (useDigits) allowedChars.append(digitChars)
    if (useSpecial) allowedChars.append(specialChars)
    val secureRandom = java.security.SecureRandom()
    return (1..length).map { allowedChars[secureRandom.nextInt(allowedChars.length)] }.joinToString("")
}

fun addPasswordRotationReminder(context: android.content.Context, serviceName: String, expiresAt: Long) {
    if (expiresAt <= 0L) return
    val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
        data = android.provider.CalendarContract.Events.CONTENT_URI
        putExtra(android.provider.CalendarContract.Events.TITLE, "Ротация пароля MnemoKot: $serviceName")
        putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Необходимо сменить пароль для сервиса $serviceName.")
        putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, expiresAt)
        putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, expiresAt + 60 * 60 * 1000)
        putExtra(android.provider.CalendarContract.Events.ACCESS_LEVEL, android.provider.CalendarContract.Events.ACCESS_PRIVATE)
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
