package ru.man2ll.mnemokot.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Журнал паролей MnemoKot", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
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
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(accounts) { account ->
                        AccountItem(
                            account = account,
                            onShowPasswordClick = { showPhraseDialogForAccountId = account.id },
                            onDeleteClick = { viewModel.onDeleteAccount(account.id) }
                        )
                    }
                }
            }

            // Диалог отображения вычисленного на лету пароля
            calculatedPassword?.let { password ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearCalculatedPassword() },
                    title = { Text("Ваш безопасный пароль") },
                    text = { Text(password, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
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
                            label = { Text("Введите вашу секретную фразу") }
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
                    onSave = { service, login, isMnemonic, len, salt, date, explicit ->
                        viewModel.onAddAccount(service, login, isMnemonic, len, salt, date, explicit)
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun AccountItem(account: Account, onShowPasswordClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), architecturalElement = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(account.serviceName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(account.login, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                SuggestionChip(
                    onClick = {},
                    label = { Text(if (account.isMnemonic) "AMPG v2 Мнемоника" else "Стандартный") }
                )
            }
            Row {
                IconButton(onClick = onShowPasswordClick) {
                    Icon(Icons.Default.Visibility, contentDescription = "Показать пароль", tint = Color.White)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun Row(modifier: Modifier, architecturalElement: Alignment.Vertical, horizontalArrangement: Arrangement.HorizontalOrVertical, content: @Composable RowScope.() -> Unit) {
    Row(modifier = modifier, verticalAlignment = architecturalElement, horizontalArrangement = horizontalArrangement, content = content)
}

@Composable
fun AddAccountDialog(onDismiss: () -> Unit, onSave: (String, String, Boolean, Int, String, String, String?) -> Unit) {
    var service by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var isMnemonic by remember { mutableStateOf(true) }
    var length by remember { mutableStateOf("16") }
    var salt by remember { mutableStateOf("") }
    var rotationDate by remember { mutableStateOf("") }
    var explicitPass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая учетная запись") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(value = service, onValueChange = { service = it }, label = { Text("Название сервиса (например, Gmail)") }) }
                item { OutlinedTextField(value = login, onValueChange = { login = it }, label = { Text("Логин / Email") }) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isMnemonic, onCheckedChange = { isMnemonic = it })
                        Text("Использовать мнемокод AMPG v2")
                    }
                }
                if (isMnemonic) {
                    item { OutlinedTextField(value = length, onValueChange = { length = it }, label = { Text("Длина пароля (например, 16)") }) }
                    item { OutlinedTextField(value = salt, onValueChange = { salt = it }, label = { Text("Своя соль (необязательно)") }) }
                    item { OutlinedTextField(value = rotationDate, onValueChange = { rotationDate = it }, label = { Text("Дата ротации ММГГ (например, 1026)") }) }
                } else {
                    item { OutlinedTextField(value = explicitPass, onValueChange = { explicitPass = it }, label = { Text("Введите явный пароль") }) }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(service, login, isMnemonic, length.toIntOrNull() ?: 16, salt, rotationDate, explicitPass.takeIf { it.isNotEmpty() })
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
