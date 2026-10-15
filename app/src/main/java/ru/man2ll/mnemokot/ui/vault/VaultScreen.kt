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
    
    // Стейты для авторизации действия по PIN-коду
    var showPinDialogForAccountId by remember { mutableStateOf<Long?>(null) }
    var pinVerificationInput by remember { mutableStateOf("") }
    
    // Стейты для последующего раскрытия мнемофразы (если аккаунт — AMPG v2)
    var showPhraseDialogForAccountId by remember { mutableStateOf<Long?>(null) }
    var mnemonicPhraseInput by remember { mutableStateOf("") }
    var isCurrentAccountDualMode by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }, 
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add, 
                    contentDescription = "Добавить аккаунт", 
                    tint = Color.Black
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (accounts.isEmpty()) {
                Text(
                    text = "Журнал пуст. Нажмите +, чтобы добавить первую запись.",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp), 
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(accounts) { account ->
                        AccountItem(
                            account = account,
                            onShowPasswordClick = { 
                                viewModel.clearVaultError()
                                pinVerificationInput = ""
                                // Сначала триггерим запрос PIN-кода профиля
                                showPinDialogForAccountId = account.id 
                            },
                            onDeleteClick = { viewModel.onDeleteAccount(account.id) }
                        )
                    }
                }
            }

            // 1. ДИАЛОГ ЗАПРОСА PIN-КОДА ПРОФИЛЯ (Защита "Глаза")
            showPinDialogForAccountId?.let { accountId ->
                AlertDialog(
                    onDismissRequest = { 
                        showPinDialogForAccountId = null
                        pinVerificationInput = ""
                        viewModel.clearVaultError()
                    },
                    title = { Text("Подтверждение операции") },
                    text = {
                        Column {
                            Text("Введите PIN-код текущего профиля для доступа к паролю:", fontSize = 14.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = pinVerificationInput,
                                onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 6) pinVerificationInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("PIN-код профиля") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = vaultError != null
                            )
                            if (vaultError != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(vaultError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val targetAccount = accounts.firstOrNull { it.id == accountId }
                                viewModel.verifyProfilePinForAction(pinVerificationInput) {
                                    // Callback выполняется только при успешном PIN
                                    showPinDialogForAccountId = null
                                    pinVerificationInput = ""
                                    
                                    if (targetAccount != null) {
                                        if (targetAccount.isMnemonic) {
                                            // Если мнемонический — запрашиваем секретную фразу
                                            isCurrentAccountDualMode = targetAccount.isDualUserMode
                                            showPhraseDialogForAccountId = targetAccount.id
                                        } else {
                                            // Если явный — сразу вычисляем (дешифруем)
                                            viewModel.onCalculatePassword(accountId, null)
                                        }
                                    }
                                }
                            }
                        ) { Text("Подтвердить") }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showPinDialogForAccountId = null
                            pinVerificationInput = ""
                            viewModel.clearVaultError()
                        }) { Text("Отмена") }
                    }
                )
            }

            // 2. Диалог отображения вычисленного пароля (С поддержкой Dual-User разделения)
            calculatedPassword?.let { password ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearCalculatedPassword() },
                    title = { 
                        Text(
                            text = if (isCurrentAccountDualMode) {
                                "Разделенный секрет (Режим 2-х пользователей)"
                            } else {
                                "Ваш безопасный пароль"
                            }
                        ) 
                    },
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
            // 3. Диалог запроса мнемофразы для расчета пароля
showPhraseDialogForAccountId?.let { accountId ->
    AlertDialog(
        onDismissRequest = { 
            showPhraseDialogForAccountId = null
            mnemonicPhraseInput = "" 
        },
        title = { Text("Ввод мнемонической фразы") },
        text = {
            OutlinedTextField(
                value = mnemonicPhraseInput,
                onValueChange = { mnemonicPhraseInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Введите секретную фразу") }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val phraseArg = mnemonicPhraseInput.takeIf { it.isNotEmpty() }
                    viewModel.onCalculatePassword(accountId, phraseArg)
                    showPhraseDialogForAccountId = null
                    mnemonicPhraseInput = ""
                }
            ) { Text("Вычислить") }
        }
    )
}
