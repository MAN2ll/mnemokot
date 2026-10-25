package ru.man2ll.mnemokot.ui.vault

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.man2ll.mnemokot.core.database.domain.model.Account

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onNavigateToGenerator: () -> Unit,
    onNavigateToAudit: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val calculatedPassword by viewModel.calculatedPassword.collectAsState()
    val vaultError by viewModel.vaultError.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedAccountIds.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var targetCalculateId by remember { mutableStateOf<Long?>(null) }
    
    val pinConfirmBuffer = remember { CharArray(6) { '\u0000' } }
    var pinConfirmLength by remember { mutableStateOf(0) }
    var showPinDialogForAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Обработка системной кнопки "Назад"
    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сейф MnemoKot", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToGenerator) { 
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Арсенал мнемотехник") 
                    }
                    IconButton(onClick = onNavigateToAudit) { 
                        Icon(Icons.Default.Shield, contentDescription = "Аудит") 
                    }
                    IconButton(onClick = onNavigateToBackup) { 
                        Icon(Icons.Default.Backup, contentDescription = "Бэкап") 
                    }
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.runSelectedPasswordRotation() }) { 
                            Icon(Icons.Default.Shuffle, contentDescription = "Перемешать пароли") 
                        }
                        IconButton(onClick = { viewModel.cancelSelectionMode() }) { 
                            Icon(Icons.Default.Close, contentDescription = "Отмена выбора") 
                        }
                    } else {
                        IconButton(onClick = { viewModel.startSelectionMode() }) { 
                            Icon(Icons.Default.Checklist, contentDescription = "Выбрать карточки") 
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить аккаунт")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (accounts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "В сейфе пока нет записей",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Нажмите + чтобы создать первую карточку пароля",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(accounts) { account ->
                        AccountItemRow(
                            account = account,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedIds.contains(account.id),
                            onToggleSelect = { viewModel.toggleAccountSelection(account.id) },
                            onCalculateClick = { 
                                targetCalculateId = account.id
                                showPinDialogForAction = {
                                    targetCalculateId?.let { id ->
                                        val cleanPin = CharArray(pinConfirmLength) { pinConfirmBuffer[it] }
                                        viewModel.onCalculatePassword(id, cleanPin)
                                        cleanPin.fill('\u0000')
                                    }
                                }
                            },
                            onDeleteClick = { viewModel.onDeleteAccount(account.id) }
                        )
                    }
                }
            }

            // Диалог подтверждения PIN для расчёта пароля
            if (showPinDialogForAction != null) {
                AlertDialog(
                    onDismissRequest = { 
                        showPinDialogForAction = null
                        pinConfirmBuffer.fill('\u0000')
                        pinConfirmLength = 0
                        viewModel.clearVaultError()
                    },
                    title = { Text("Подтвердите PIN-код профиля") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = String(pinConfirmBuffer, 0, pinConfirmLength),
                                onValueChange = { input ->
                                    val clean = input.filter { it.isDigit() }.take(6)
                                    pinConfirmLength = clean.length
                                    for (i in 0 until 6) {
                                        pinConfirmBuffer[i] = if (i < clean.length) clean[i] else '\u0000'
                                    }
                                },
                                label = { Text("PIN-код") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (vaultError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    vaultError!!, 
                                    color = MaterialTheme.colorScheme.error, 
                                    fontSize = 14.sp
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val cleanPinToVerify = CharArray(pinConfirmLength) { pinConfirmBuffer[it] }
                                viewModel.verifyProfilePinForAction(cleanPinToVerify) {
                                    showPinDialogForAction?.invoke()
                                    showPinDialogForAction = null
                                    pinConfirmBuffer.fill('\u0000')
                                    pinConfirmLength = 0
                                }
                                cleanPinToVerify.fill('\u0000')
                            },
                            enabled = pinConfirmLength >= 4
                        ) { 
                            Text("Подтвердить") 
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showPinDialogForAction = null
                            pinConfirmBuffer.fill('\u0000')
                            pinConfirmLength = 0
                        }) { 
                            Text("Отмена") 
                        }
                    }
                )
            }

            // Диалог отображения рассчитанного пароля
            if (calculatedPassword != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearCalculatedPassword() },
                    title = { Text("Рассчитанный пароль") },
                    text = {
                        val displayPass = calculatedPassword!!
                        Column {
                            Text(
                                text = String(displayPass),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Скопируйте пароль. Он будет стёрт из памяти после закрытия диалога.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.clearCalculatedPassword() }) { 
                            Text("Закрыть и стереть") 
                        }
                    }
                )
            }
        }
    }

    // Диалог создания новой карточки пароля
    if (showCreateDialog) {
        CreateAccountDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { data ->
                viewModel.saveAccount(data)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun AccountItemRow(
    account: Account,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onCalculateClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (isSelectionMode) {
                    onToggleSelect() 
                } else {
                    onCalculateClick()
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer 
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected, 
                    onCheckedChange = { onToggleSelect() }
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    account.serviceName, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Логин: ${account.login}", 
                    fontSize = 14.sp, 
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    if (account.isMnemonic) "Тип: Мнемоника (детерминированный)" else "Тип: Хранилище (зашифрованный)", 
                    fontSize = 12.sp, 
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (!isSelectionMode) {
                IconButton(onClick = onDeleteClick) { 
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = "Удалить", 
                        tint = Color.Gray
                    ) 
                }
            }
        }
    }
}
