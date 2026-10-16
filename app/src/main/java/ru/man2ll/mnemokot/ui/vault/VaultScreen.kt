package ru.man2ll.mnemokot.ui.vault

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import ru.man2ll.mnemokot.core.database.domain.model.Account
import ru.man2ll.mnemokot.ui.auth.MasterAuthViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(viewModel: VaultViewModel, masterAuthViewModel: MasterAuthViewModel) {
    val accounts by viewModel.accounts.collectAsState()
    val calculatedPassword by viewModel.calculatedPassword.collectAsState()
    val vaultError by viewModel.vaultError.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedAccountIds by viewModel.selectedAccountIds.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showAddEditDialogForAccount by remember { mutableStateOf<Account?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    // Стейты экспорта физических конвертов безопасности
    var pendingEnvelopeText by remember { mutableStateOf<String?>(null) }
    var showMasterRequestForEnvelope by remember { mutableStateOf(false) }
    var masterInputForEnvelope by remember { mutableStateOf("") }
    var envelopeMasterError by remember { mutableStateOf<String?>(null) }
    
    var showPinDialogForAccountId by remember { mutableStateOf<Long?>(null) }
    var pinVerificationInput by remember { mutableStateOf("") }
    var showPhraseDialogForAccountId by remember { mutableStateOf<Long?>(null) }
    var mnemonicPhraseInput by remember { mutableStateOf("") }
    var isCurrentAccountDualMode by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Аппаратный лаунчер SAF для создания физического файла .txt на диске смартфона
    val fileExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null && pendingEnvelopeText != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(pendingEnvelopeText!!.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Конверт безопасности успешно сохранен в файл!", Toast.LENGTH_LONG).show()
                viewModel.cancelSelectionMode()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка сохранения файла: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                pendingEnvelopeText = null
            }
        }
    }

    val barcodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (contents != null && contents.startsWith("mnemokot://vault/account")) {
            val uri = android.net.Uri.parse(contents)
            val accountId = uri.getQueryParameter("id")?.toLongOrNull()
            if (accountId != null) {
                viewModel.clearVaultError()
                pinVerificationInput = ""
                showPinDialogForAccountId = accountId
            }
        }
    }

    val filteredAccounts = remember(accounts, searchQuery) {
        if (searchQuery.isBlank()) accounts else {
            accounts.filter { 
                it.serviceName.contains(searchQuery, ignoreCase = true) || 
                it.login.contains(searchQuery, ignoreCase = true) ||
                it.salt.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = { isEditMode = false; showAddDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.Black)
                }
            }
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).fillMaxSize()) {
            if (!isSelectionMode) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Поиск по сервису, логину или #тегу...") },
                    trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) } } },
                    singleLine = true
                )
            }
            if (accounts.isNotEmpty()) {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer, tonalElevation = 2.dp) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        if (!isSelectionMode) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { 
                                    val options = ScanOptions().apply {
                                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                        setPrompt("Наведите камеру на физическую метку MnemoKot")
                                        setCaptureActivity(QrScannerActivity::class.java)
                                    }
                                    barcodeLauncher.launch(options)
                                }) { Text("📷 Сканировать", fontSize = 12.sp) }
                            }
                            Button(onClick = { viewModel.startSelectionMode() }) { Text("Режим выбора", fontSize = 12.sp) }
                        } else {
                            Text("Выбрано: ${selectedAccountIds.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { viewModel.cancelSelectionMode() }) { Text("Отмена") }
                                Button(onClick = { viewModel.runSelectedPasswordRotation() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Перемешать", fontSize = 11.sp) }
                                Button(onClick = { 
                                    val ids = selectedAccountIds.toList()
                                    if (ids.isEmpty()) { Toast.makeText(context, "Отметьте записи", Toast.LENGTH_SHORT).show() }
                                    else {
                                        // Активируем двухфакторное подтверждение операции Мастер-паролем
                                        masterInputForEnvelope = ""
                                        envelopeMasterError = null
                                        showMasterRequestForEnvelope = true
                                    }
                                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) { Text("📄 Конверт", fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (filteredAccounts.isEmpty()) {
                    Text("Записей не найдено.", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredAccounts) { acc ->
                            AccountItem(acc, isSelectionMode, selectedAccountIds.contains(acc.id), { viewModel.toggleAccountSelection(acc.id) }, { viewModel.clearVaultError(); pinVerificationInput = ""; showPinDialogForAccountId = acc.id }, { viewModel.onDeleteAccount(acc.id) }, { isEditMode = true; showAddEditDialogForAccount = acc; showAddDialog = true })
                        }
                    }
                }
                
                vaultError?.let { report ->
                    AlertDialog(onDismissRequest = { viewModel.clearVaultError() }, title = { Text("Системный аудит", fontWeight = FontWeight.Bold) }, text = { Text(report, fontSize = 14.sp) }, confirmButton = { Button(onClick = { viewModel.clearVaultError() }) { Text("ОК") } })
                }

                // ДИАЛОГ ВЕРИФИКАЦИИ МАСТЕР-ПАРОЛЯ ПЕРЕД ФОРМИРОВАНИЕМ ФАЙЛА
                if (showMasterRequestForEnvelope) {
                    AlertDialog(
                        onDismissRequest = { showMasterRequestForEnvelope = false },
                        title = { Text("Подтверждение Мастер-пароля") },
                        text = {
                            Column {
                                Text("Операция критической безопасности. Введите Мастер-пароль приложения для выгрузки открытых ключей в файл:", fontSize = 13.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = masterInputForEnvelope, onValueChange = { masterInputForEnvelope = it }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                                if (envelopeMasterError != null) {
                                    Text(envelopeMasterError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                val isMasterValid = masterAuthViewModel.onVerifyMasterPassword(masterInputForEnvelope)
                                if (isMasterValid) {
                                    showMasterRequestForEnvelope = false
                                    // Формируем чистый текст отчета с реальными открытыми паролями из зашифрованной базы
                                    val ids = selectedAccountIds.toList()
                                    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                    val builder = StringBuilder()
                                    builder.append("=========================================\n")
                                    builder.append("      СЛУЖЕБНЫЙ КОНВЕРТ БЕЗОПАСНОСТИ      \n")
                                    builder.append("=========================================\n")
                                    builder.append("Дата выгрузки: ${dateFormat.format(System.currentTimeMillis())}\n\n")
                                    
                                    accounts.filter { ids.contains(it.id) }.forEachIndexed { idx, acc ->
                                        builder.append("${idx + 1}. СЕРВИС: ${acc.serviceName}\n")
                                        builder.append("   ЛОГИН:  ${acc.login}\n")
                                        builder.append("   ПАРОЛЬ: ${acc.explicitPassword ?: "Мнемокод (расчет на лету по PIN)"}\n")
                                        builder.append("-----------------------------------------\n")
                                    }
                                    
                                    pendingEnvelopeText = builder.toString()
                                    // Запускаем оффлайн-сохранение файла на устройство через SAF
                                    fileExportLauncher.launch("mnemokot_envelope_${System.currentTimeMillis()}.txt")
                                } else {
                                    envelopeMasterError = "Неверный глобальный Мастер-пароль!"
                                }
                            }) { Text("Подтвердить") }
                        },
                        dismissButton = { TextButton(onClick = { showMasterRequestForEnvelope = false }) { Text("Отмена") } }
                    )
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
                        title = { Text(if (isCurrentAccountDualMode) "Разделенный секрет" else "Секретный пароль") },
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
                        confirmButton = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { 
                                    clipboardManager.setText(AnnotatedString(pass))
                                    Toast.makeText(context, "Скопировано в буфер!", Toast.LENGTH_SHORT).show()
                                    viewModel.clearCalculatedPassword()
                                }) { Text("Копировать") }
                                TextButton(onClick = { viewModel.clearCalculatedPassword() }) { Text("Закрыть") }
                            }
                        }
                    )
                }

                showPhraseDialogForAccountId?.let { id ->
                    AlertDialog(onDismissRequest = { showPhraseDialogForAccountId = null; mnemonicPhraseInput = "" }, title = { Text("Ввод мнемосхемы") }, text = { OutlinedTextField(value = mnemonicPhraseInput, onValueChange = { mnemonicPhraseInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Мнемофраза") }) }, confirmButton = { Button(onClick = { viewModel.onCalculatePassword(id, mnemonicPhraseInput.takeIf { it.isNotEmpty() }); showPhraseDialogForAccountId = null; mnemonicPhraseInput = "" }) { Text("Расчет") } })
                }

                if (showAddDialog) {
                    AddAccountDialog(accountToEdit = if (isEditMode) showAddEditDialogForAccount else null, onDismiss = { showAddDialog = false; isEditMode = false }, onSave = { s, l, m, len, sl, d, ex, dl -> viewModel.onAddAccount(s, l, m, len, sl, d, ex, dl); showAddDialog = false; isEditMode = false })
                }
            }
        }
    }
}

@Composable
fun AccountItem(
    account: Account, 
    isSelectionMode: Boolean, 
    isSelected: Boolean, 
    onSelectToggle: () -> Unit, 
    onShowPasswordClick: () -> Unit, 
    onDeleteClick: () -> Unit, 
    onEditClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // ФИКС: Локально инициализируем буфер обмена для этой карточки учетной записи
    val clipboardManager = LocalClipboardManager.current
    var showQrDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isSelectionMode) { onSelectToggle() }, 
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(account.serviceName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (account.salt.startsWith("#")) { 
                        Text(account.salt, fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold) 
                    }
                }
                Text(account.login, fontSize = 14.sp, color = Color.Gray)
                if (account.expiresAt > 0L) {
                    val daysLeft = ((account.expiresAt - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).coerceAtLeast(0)
                    Text(text = if (daysLeft > 0) "Истекает через: $daysLeft дн." else "Пароль устарел! Требуется смена", fontSize = 12.sp, color = if (daysLeft <= 7) Color(0xFFEF5350) else Color(0xFFFFEE58), fontWeight = FontWeight.Medium)
                } else { 
                    Text("Срок действия: Вечный", fontSize = 12.sp, color = Color.LightGray) 
                }
                
                Text("Установлен: ${dateFormat.format(account.createdAt)}", fontSize = 10.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(onClick = {}, label = { Text(if (account.isMnemonic) "Парольная фраза" else "Явный") })
                    if (account.isDualUserMode) SuggestionChip(onClick = {}, label = { Text("2 Игрока") })
                    if (account.expiresAt > 0L && !isSelectionMode) {
                        TextButton(onClick = { addPasswordRotationReminder(context, account.serviceName, account.expiresAt) }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)) { 
                            Text("⏰ В календарь", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary) 
                        }
                    }
                }
            }
            if (isSelectionMode) { 
                Checkbox(checked = isSelected, onCheckedChange = { onSelectToggle() }) 
            } else { 
                Row { 
                    IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, contentDescription = "Правка", tint = Color.LightGray) }
                    IconButton(onClick = { showQrDialog = true }) { Text("📷", fontSize = 16.sp) }
                    IconButton(onClick = onShowPasswordClick) { Icon(Icons.Default.Visibility, contentDescription = "Показать", tint = Color.White) }
                    IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error) } 
                } 
            }
        }
    }
    if (showQrDialog) {
        val qrBitmap = remember { QrCodeGenerator.generateAccountQr(account.id, account.profileId, account.serviceName) }
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = { Text("Физическая QR-метка", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Метка: MnemoKot 🐾 | ${account.serviceName}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (qrBitmap != null) { androidx.compose.foundation.Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(200.dp)) }
                    else { Text("Ошибка матрицы", color = MaterialTheme.colorScheme.error) }
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(color = Color(0xFF1B2A1C), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(text = "🛡 Безопасность: Данный код содержит только индекс учетной записи для мгновенного поиска. Ваш секретный мастер-пароль или фраза здесь НЕ хранятся и не могут быть перехвачены при сканировании сторонними программами.", fontSize = 11.sp, color = Color.LightGray, modifier = Modifier.padding(8.dp), lineHeight = 14.sp)
                    }
                }
            },
            confirmButton = { Button(onClick = { showQrDialog = false }) { Text("Закрыть") } }
        )
    }
}

@Composable
fun AddAccountDialog(
    accountToEdit: Account?, 
    onDismiss: () -> Unit, 
    onSave: (String, String, Boolean, Int, String, Int, String?, Boolean) -> Unit
) {
    var service by remember { mutableStateOf(accountToEdit?.serviceName ?: "") }
    var login by remember { mutableStateOf(accountToEdit?.login ?: "") }
    var selectedTab by remember { mutableStateOf(if (accountToEdit?.isMnemonic == false) 0 else 2) }
    var explicitPass by remember { mutableStateOf(accountToEdit?.explicitPassword ?: "") }
    var randomLength by remember { mutableStateOf("16") }
    var useUpper by remember { mutableStateOf(true) }
    var useDigits by remember { mutableStateOf(true) }
    var useSpecial by remember { mutableStateOf(false) }
    var length by remember { mutableStateOf(accountToEdit?.passwordLength?.toString() ?: "16") }
    
    // Переносим исходную фразу в rotationDate для постоянного хранения и чтения при правке
    var phraseInput by remember { mutableStateOf(if (accountToEdit?.isMnemonic == true) accountToEdit.rotationDate else "") }
    var salt by remember { mutableStateOf(accountToEdit?.salt ?: "") }
    var isDualUserMode by remember { mutableStateOf(accountToEdit?.isDualUserMode ?: false) }
    var daysMenuExpanded by remember { mutableStateOf(false) }
    val daysOptions = listOf(0 to "Вечный пароль", 30 to "30 дней", 90 to "90 дней", 180 to "180 дней")
    var selectedDaysOption by remember { mutableStateOf(daysOptions.first()) }

    AlertDialog(
        onDismissRequest = {}, 
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(if (accountToEdit != null) "Редактирование записи" else "Новая учетная запись", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item { OutlinedTextField(value = service, onValueChange = { service = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Название сервиса") }) }
                item { OutlinedTextField(value = login, onValueChange = { login = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Логин / Email") }) }
                
                item { 
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        Text("Конструктор секретного пароля", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                item { TabRow(selectedTabIndex = selectedTab, modifier = Modifier.padding(vertical = 4.dp)) { Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Ручной ввод", fontSize = 11.sp) }); Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Случайный", fontSize = 11.sp) }); Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Парольная фраза", fontSize = 11.sp) }) } }
                
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
                        item { OutlinedTextField(value = length, onValueChange = { length = it }, modifier = Modifier.fillMaxWidth(), label = { Text(if (isDualUserMode) { "Длина (16, 18 или 20)" } else { "Длина пароля" }) }) }
                        item { OutlinedTextField(value = phraseInput, onValueChange = { phraseInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Ваша парольная фраза-источник") }) }
                    }
                }
                item { Box(modifier = Modifier.fillMaxWidth()) { OutlinedButton(onClick = { daysMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Срок действия: ${selectedDaysOption.second}") }; DropdownMenu(expanded = daysMenuExpanded, onDismissRequest = { daysMenuExpanded = false }) { daysOptions.forEach { option -> DropdownMenuItem(text = { Text(option.second) }, onClick = { selectedDaysOption = option; daysMenuExpanded = false }) } } } }
                
                // Переместили опциональное текстовое поле тегов в самый конец формы создания
                item { OutlinedTextField(value = salt, onValueChange = { salt = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Тег записи (например, #работа)") }) }
            }
        },
        confirmButton = {
            Button(onClick = { 
                val isMnemonicType = (selectedTab == 2)
                val finalLen = if (selectedTab == 1) randomLength.toIntOrNull() ?: 16 else length.toIntOrNull() ?: 16
                // Если выбран режим фразы, фиксируем фразу-источник в поле метаданных для сохранения
                val finalExplicit = if (selectedTab == 2) "MnemoPass_" + phraseInput.hashCode() else explicitPass.takeIf { it.isNotEmpty() }
                onSave(service, login, isMnemonicType, finalLen, salt, selectedDaysOption.first, finalExplicit, isDualUserMode)
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
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
    
    val poolSize = length / 4
    if (poolSize >= 1 && useUpper && useDigits && useSpecial) {
        val secureRandom = java.security.SecureRandom()
        val p1 = (1..poolSize).map { lowerChars[secureRandom.nextInt(lowerChars.length)] }
        val p2 = (1..poolSize).map { upperChars[secureRandom.nextInt(upperChars.length)] }
        val p3 = (1..poolSize).map { digitChars[secureRandom.nextInt(digitChars.length)] }
        val p4 = (1..(length - poolSize * 3)).map { specialChars[secureRandom.nextInt(specialChars.length)] }
        return (p1 + p2 + p3 + p4).shuffled().joinToString("")
    }
    val secureRandom = java.security.SecureRandom()
    return (1..length).map { allowedChars[secureRandom.nextInt(allowedChars.length)] }.joinToString("")
}

fun addPasswordRotationReminder(context: android.content.Context, serviceName: String, expiresAt: Long) {
    if (expiresAt <= 0L) return
    val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
        data = android.provider.CalendarContract.Events.CONTENT_URI
        putExtra(android.provider.CalendarContract.Events.TITLE, "Ротация пароля MnemoKot: $serviceName")
        putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Необходимо сменить детерминированный пароль для сервиса $serviceName.")
        putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, expiresAt)
        putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, expiresAt + 60 * 60 * 1000)
        putExtra(android.provider.CalendarContract.Events.ACCESS_LEVEL, android.provider.CalendarContract.Events.ACCESS_PRIVATE)
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
