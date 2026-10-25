package ru.man2ll.mnemokot.ui.vault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    onDismiss: () -> Unit,
    onSave: (CreateAccountData) -> Unit
) {
    // Состояние формы
    var serviceName by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    
    // Состояние генератора
    var showGenerator by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(MnemonicMode.AMPG_V2_GOST) }
    var useLeetSpeak by remember { mutableStateOf(true) }
    var passwordLength by remember { mutableIntStateOf(16) }
    
    // Состояние мнемонической фразы
    var mnemonicPhrase by remember { mutableStateOf("") }
    var showMnemonicInput by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Заголовок
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
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Поле "Сервис"
                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text("Сервис *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Поле "Логин"
                OutlinedTextField(
                    value = login,
                    onValueChange = { login = it },
                    label = { Text("Логин / Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Поле "Пароль" с кнопкой генерации
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Показать пароль"
                                )
                            }
                            IconButton(onClick = { showGenerator = true }) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Сгенерировать")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Отмена")
                    }

                    Button(
                        onClick = {
                            val data = CreateAccountData(
                                serviceName = serviceName,
                                login = login,
                                password = password,
                                mnemonicPhrase = if (showMnemonicInput) mnemonicPhrase else null,
                                mode = selectedMode,
                                useLeetSpeak = useLeetSpeak,
                                passwordLength = passwordLength
                            )
                            onSave(data)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = serviceName.isNotBlank() && password.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }

    // Диалог генератора
    if (showGenerator) {
        GeneratorDialog(
            onDismiss = { showGenerator = false },
            onGenerated = { generatedPassword ->
                password = generatedPassword
                showGenerator = false
            }
        )
    }
}

@Composable
fun GeneratorDialog(
    onDismiss: () -> Unit,
    onGenerated: (String) -> Unit
) {
    var selectedMode by remember { mutableStateOf(MnemonicMode.AMPG_V2_GOST) }
    var useLeetSpeak by remember { mutableStateOf(true) }
    var passwordLength by remember { mutableIntStateOf(16) }
    var mnemonicPhrase by remember { mutableStateOf("") }
    var generatedPassword by remember { mutableStateOf("") }
    var showGeneratedPassword by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Заголовок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Генератор паролей",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Выбор режима генерации
                Text(
                    text = "Режим генерации",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MnemonicMode.values().forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode.name, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Поле ввода мнемонической фразы
                OutlinedTextField(
                    value = mnemonicPhrase,
                    onValueChange = { mnemonicPhrase = it },
                    label = { Text("Мнемоническая фраза") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Переключатель Leet Speak
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Использовать Leet Speak",
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = useLeetSpeak,
                        onCheckedChange = { useLeetSpeak = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Выбор длины пароля
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

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопка генерации
                Button(
                    onClick = {
                        // Здесь будет вызов генератора
                        // Пока заглушка
                        generatedPassword = "Generated: $mnemonicPhrase"
                        showGeneratedPassword = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = mnemonicPhrase.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Сгенерировать")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Предпросмотр пароля
                if (showGeneratedPassword && generatedPassword.isNotBlank()) {
                    OutlinedTextField(
                        value = generatedPassword,
                        onValueChange = {},
                        label = { Text("Предпросмотр пароля") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        singleLine = true,
                        visualTransformation = if (showGeneratedPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showGeneratedPassword = !showGeneratedPassword }) {
                                Icon(
                                    if (showGeneratedPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Показать пароль"
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Кнопка утверждения
                    Button(
                        onClick = {
                            onGenerated(generatedPassword)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Утвердить пароль")
                    }
                }
            }
        }
    }
}

data class CreateAccountData(
    val serviceName: String,
    val login: String,
    val password: String,
    val mnemonicPhrase: String?,
    val mode: MnemonicMode,
    val useLeetSpeak: Boolean,
    val passwordLength: Int
)
