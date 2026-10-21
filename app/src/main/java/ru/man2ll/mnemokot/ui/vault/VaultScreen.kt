package ru.man2ll.mnemokot.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.man2ll.mnemokot.core.database.domain.model.Account

@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onNavigateToAudit: () -> Unit,
    onNavigateToBackup: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val calculatedPassword by viewModel.calculatedPassword.collectAsState()
    val vaultError by viewModel.vaultError.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedAccountIds.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var targetCalculateId by remember { mutableStateOf<Long?>(null) }
    
    val pinConfirmBuffer = remember { CharArray(6) { '\u0000' } }
    var pinConfirmLength by remember { mutableStateOf(0) }
    var showPinDialogForAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Сейф MnemoKot", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToAudit) { Icon(Icons.Default.Shield, contentDescription = "Аудит") }
                    IconButton(onClick = onNavigateToBackup) { Icon(Icons.Default.Backup, contentDescription = "Бэкап") }
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.runSelectedPasswordRotation() }) { Icon(Icons.Default.Shuffle, contentDescription = "Перемешать") }
                        IconButton(onClick = { viewModel.cancelSelectionMode() }) { Icon(Icons.Default.Close, contentDescription = "Отмена") }
                    } else {
                        IconButton(onClick = { viewModel.startSelectionMode() }) { Icon(Icons.Default.Checklist, contentDescription = "Выбрать") }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить аккаунт")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (accounts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("В сейфе пока нет записей. Создайте первую!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        viewModel.onCalculatePassword(id, pinConfirmBuffer.clone())
                                    }
                                }
                            },
                            onDeleteClick = { viewModel.onDeleteAccount(account.id) }
                        )
                    }
                }
            }
            if (showPinDialogForAction != null) {
                AlertDialog(
                    onDismissRequest = { 
                        showPinDialogForAction = null
                        pinConfirmBuffer.fill('\u0000')
                        pinConfirmLength = 0
                    },
                    title = { Text("Подтвердите PIN-код профиля") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = String(pinConfirmBuffer, 0, pinConfirmLength),
                                onValueChange = { input ->
                                    val clean = input.filter { it.isDigit() }.take(6)
                                    pinConfirmLength = clean.length
                                    for (i in 0 until 6) pinConfirmBuffer[i] = if (i < clean.length) clean[i] else '\u0000'
                                },
                                label = { Text("PIN-код") },
                                visualTransformation = PasswordVisualTransformation()
                            )
                            if (vaultError != null) {
                                Text(vaultError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.verifyProfilePinForAction(pinConfirmBuffer.clone()) {
                                showPinDialogForAction?.invoke()
                                showPinDialogForAction = null
                                pinConfirmBuffer.fill('\u0000')
                                pinConfirmLength = 0
                            }
                        }) { Text("Подтвердить") }
                    }
                )
            }

            if (calculatedPassword != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearCalculatedPassword() },
                    title = { Text("Рассчитанный пароль") },
                    text = {
                        val displayPass = calculatedPassword!!
                        Text(
                            text = String(displayPass),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.clearCalculatedPassword() }) { Text("Закрыть") }
                    }
                )
            }
        }
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
        modifier = Modifier.fillMaxWidth().clickable { if (isSelectionMode) onToggleSelect() else onCalculateClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(account.serviceName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Логин: ${account.login}", fontSize = 14.sp, color = Color.Gray)
                Text(if (account.isMnemonic) "Тип: Мнемоника" else "Тип: Хранилище", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            if (!isSelectionMode) {
                IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Gray) }
            }
        }
    }
}
