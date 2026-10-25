package ru.man2ll.mnemokot.ui.vault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountDialog(
    viewModel: VaultViewModel,
    onDismiss: () -> Unit
) {
    var serviceName by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf(AccountType.MNEMONIC) }
    var selectedMode by remember { mutableStateOf(MnemonicMode.AMPG_V2_GOST) }
    var useLeetSpeak by remember { mutableStateOf(true) }
    var passwordLength by remember { mutableIntStateOf(16) }
    var phraseHint by remember { mutableStateOf("") }

    val phraseBuffer = remember { CharArray(256) { '\u0000' } }
    var phraseLength by remember { mutableStateOf(0) }

    val vaultPasswordBuffer = remember { CharArray(128) { '\u0000' } }
    var vaultPasswordLength by remember { mutableStateOf(0) }

    var generatedPreview by remember { mutableStateOf<CharArray?>(null) }
    var showPreview by remember { mutableStateOf(true) }
    var currentSalt by remember { mutableStateOf<ByteArray?>(null) }

    var showMasterPasswordStep by remember { mutableStateOf(false) }
    val masterPasswordBuffer = remember { CharArray(128) { '\u0000' } }
    var masterPasswordLength by remember { mutableStateOf(0) }

    val vaultError by viewModel.vaultError.collectAsState()

    fun generatePreview() {
        if (phraseLength == 0) return
        currentSalt?.fill(0)
        val salt = viewModel.generateSalt()
        currentSalt = salt
        val phrase = CharArray(phraseLength) { phraseBuffer[it] }
        val preview = viewModel.generatePasswordPreview(
            phrase = phrase,
            salt = salt,
            mode = selectedMode,
            useLeetSpeak = useLeetSpeak,
            length = passwordLength
        )
        generatedPreview?.fill('\u0000')
        generatedPreview = preview
        phrase.fill('\u0000')
    }

    fun generateRandomForVault() {
        val randomPassword = viewModel.generateRandomPassword(passwordLength)
        for (i in 0 until passwordLength) {
            vaultPasswordBuffer[i] = randomPassword[i]
        }
        vaultPasswordLength = passwordLength
        randomPassword.fill('\u0000')
    }

    fun attemptSave() {
        if (accountType == AccountType.VAULT || accountType == AccountType.HYBRID) {
            showMasterPasswordStep = true
        } else {
            performSave(null)
        }
    }

    fun performSave(masterPassword: CharArray?) {
        if (masterPassword != null) {
            viewModel.setMasterPasswordForEncryption(masterPassword)
        }

        val salt = currentSalt ?: viewModel.generateSalt()
        val password: CharArray
        val phrase: CharArray?

        when (accountType) {
            AccountType.MNEMONIC -> {
                password = charArrayOf()
                phrase = CharArray(phraseLength) { phraseBuffer[it] }
            }
            AccountType.HYBRID -> {
                phrase = CharArray(phraseLength) { phraseBuffer[it] }
                password = generatedPreview?.clone() ?: charArrayOf()
            }
            AccountType.VAULT -> {
                password = CharArray(vaultPasswordLength) { vaultPasswordBuffer[it] }
                phrase = null
            }
        }

        val data = CreateAccountData(
            serviceName = serviceName.trim(),
            login = login.trim(),
            password = password,
            accountType = accountType,
            mnemonicPhrase = phrase,
            mode = selectedMode,
            useLeetSpeak = useLeetSpeak,
            passwordLength = passwordLength,
            phraseHint = phraseHint.ifBlank { null },
            salt = salt
        )

        viewModel.saveAccount(data)
        cleanupAndDismiss()
    }

    fun cleanupAndDismiss() {
        phraseBuffer.fill('\u0000')
        vaultPasswordBuffer.fill('\u0000')
        masterPasswordBuffer.fill('\u0000')
        generatedPreview?.fill('\u0000')
        currentSalt?.fill(0)
        onDismiss()
    }

    Dialog(
        onDismissRequest = { cleanupAndDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Новая запись",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { cleanupAndDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountType.values().forEach { type ->
                        FilterChip(
                            selected = accountType == type,
                            onClick = {
                                accountType = type
                                generatedPreview?.fill('\u0000')
                                generatedPreview = null
                            },
                            label = {
                                Text(
                                    when (type) {
                                        AccountType.MNEMONIC -> "Мнемоника"
                                        AccountType.HYBRID -> "Гибрид"
                                        AccountType.VAULT -> "Сейф"
                                    },
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text("Сервис *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = login,
                    onValueChange = { login = it },
                    label = { Text("Логин / Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (accountType) {
                    AccountType.MNEMONIC, AccountType.HYBRID -> {
                        Text(
                            text = "Режим генерации",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MnemonicMode.values().forEach { mode ->
                                FilterChip(
                                    selected = selectedMode == mode,
                                    onClick = { selectedMode = mode },
                                    label = {
                                        Text(
                                            when (mode) {
                                                MnemonicMode.AMPG_V2_GOST -> "AMPG"
                                                MnemonicMode.QWERTY_SHIFT -> "QWERTY"
                                                MnemonicMode.PURE_LEET_SPEAK -> "Leet"
                                            },
                                            fontSize = 10.sp
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = String(phraseBuffer, 0, phraseLength),
                            onValueChange = { input ->
                                phraseLength = input.length.coerceAtMost(256)
                                for (i in 0 until phraseLength) phraseBuffer[i] = input[i]
                                generatedPreview?.fill('\u0000')
                                generatedPreview = null
                            },
                            label = { Text("Мнемоническая фраза *") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = phraseHint,
                            onValueChange = { phraseHint = it },
                            label = { Text("Подсказка для фразы (не секрет)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Leet Speak",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = useLeetSpeak,
                                onCheckedChange = { useLeetSpeak = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Длина пароля: $passwordLength",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = passwordLength.toFloat(),
                            onValueChange = { passwordLength = it.toInt() },
                            valueRange = 8f..32f,
                            steps = 23,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { generatePreview() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = phraseLength > 0,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Сгенерировать")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (generatedPreview != null) {
                            OutlinedTextField(
                                value = String(generatedPreview!!),
                                onValueChange = {},
                                label = { Text("Предпросмотр пароля") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                visualTransformation = if (showPreview) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPreview = !showPreview }) {
                                        Icon(
                                            if (showPreview) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    AccountType.VAULT -> {
                        OutlinedTextField(
                            value = String(vaultPasswordBuffer, 0, vaultPasswordLength),
                            onValueChange = { input ->
                                vaultPasswordLength = input.length.coerceAtMost(128)
                                for (i in 0 until vaultPasswordLength) vaultPasswordBuffer[i] = input[i]
                            },
                            label = { Text("Пароль *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showPreview) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { showPreview = !showPreview }) {
                                        Icon(
                                            if (showPreview) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                    IconButton(onClick = { generateRandomForVault() }) {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Длина пароля: $passwordLength",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = passwordLength.toFloat(),
                            onValueChange = { passwordLength = it.toInt() },
                            valueRange = 8f..32f,
                            steps = 23,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (vaultError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = vaultError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { cleanupAndDismiss() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Отмена")
                    }

                    Button(
                        onClick = { attemptSave() },
                        modifier = Modifier.weight(1f),
                        enabled = serviceName.isNotBlank() && when (accountType) {
                            AccountType.MNEMONIC, AccountType.HYBRID -> phraseLength > 0
                            AccountType.VAULT -> vaultPasswordLength > 0
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }

    if (showMasterPasswordStep) {
        AlertDialog(
            onDismissRequest = {
                showMasterPasswordStep = false
                masterPasswordBuffer.fill('\u0000')
                masterPasswordLength = 0
            },
            title = { Text("Мастер-пароль") },
            text = {
                Column {
                    Text(
                        text = "Для шифрования записи введите мастер-пароль",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = String(masterPasswordBuffer, 0, masterPasswordLength),
                        onValueChange = { input ->
                            masterPasswordLength = input.length.coerceAtMost(128)
                            for (i in 0 until masterPasswordLength) masterPasswordBuffer[i] = input[i]
                        },
                        label = { Text("Мастер-пароль") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val masterPassword = CharArray(masterPasswordLength) { masterPasswordBuffer[it] }
                        masterPasswordBuffer.fill('\u0000')
                        masterPasswordLength = 0
                        showMasterPasswordStep = false
                        performSave(masterPassword)
                    },
                    enabled = masterPasswordLength > 0
                ) {
                    Text("Подтвердить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showMasterPasswordStep = false
                    masterPasswordBuffer.fill('\u0000')
                    masterPasswordLength = 0
                }) {
                    Text("Отмена")
                }
            }
        )
    }
}
