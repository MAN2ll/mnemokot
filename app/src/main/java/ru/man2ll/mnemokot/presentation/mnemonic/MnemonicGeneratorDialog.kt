package ru.man2ll.mnemokot.presentation.mnemonic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.man2ll.mnemokot.core.crypto.domain.model.MnemonicMode

@Composable
fun MnemonicGeneratorDialog(
    viewModel: MnemonicGeneratorViewModel,
    onDismissRequest: () -> Unit,
    onPasswordApplied: (CharArray) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .padding(16.dp),
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
                        text = "🐈 Арсенал Мнемотехника",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.toggleHelpVisibility() }) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Справка",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                val modes = listOf(
                    MnemonicMode.AMPG_V2_GOST to "AMPG v2",
                    MnemonicMode.QWERTY_SHIFT to "QWERTY сдвиг",
                    MnemonicMode.PURE_LEET_SPEAK to "Посимвольный Leet"
                )
                val selectedIndex = modes.indexOfFirst { it.first == uiState.mnemonicMode }

                TabRow(selectedTabIndex = selectedIndex) {
                    modes.forEachIndexed { index, pair ->
                        Tab(
                            selected = selectedIndex == index,
                            onClick = { viewModel.onMnemonicModeChanged(pair.first) }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 12.dp)) {
                                Text(
                                    text = pair.second,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = if (selectedIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = uiState.isHelpVisible,
                    enter = expandVertically(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300))
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(12.dp)
                    ) {
                        val helpText = when (uiState.mnemonicMode) {
                            MnemonicMode.AMPG_V2_GOST -> "AMPG v2: Переводит кириллицу в транслит ГОСТ, усекает корни слов и подмешивает соль аккаунта через SHA-256."
                            MnemonicMode.QWERTY_SHIFT -> "QWERTY сдвиг: Имитирует ввод русских букв на английской раскладке клавиатуры (например, 'Кот' -> 'Vjn')."
                            MnemonicMode.PURE_LEET_SPEAK -> "Посимвольный Leet: Преобразует каждый символ фразы по Leet-карте замен с чередованием регистров букв."
                        }
                        Text(text = helpText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                val phraseBuffer = remember { CharArray(256) { '\u0000' } }
                var phraseLength by remember { mutableStateOf(0) }

                OutlinedTextField(
                    value = String(phraseBuffer, 0, phraseLength),
                    onValueChange = { input ->
                        phraseLength = input.length.coerceAtMost(256)
                        viewModel.clearGeneratedPassword()
                        for (i in 0 until phraseLength) {
                            phraseBuffer[i] = input[i]
                            viewModel.onPhraseCharAdded(input[i])
                        }
                    },
                    label = { Text("Мнемоническая фраза") },
                    placeholder = { Text("Введите текст для трансформации...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.mnemonicMode == MnemonicMode.AMPG_V2_GOST) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Мутация повторов (Leet)", style = MaterialTheme.typography.bodyMedium)
                            Text("Подстановка спецсимволов вместо удаления дубликатов", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = uiState.useLeetSpeak, onCheckedChange = { viewModel.onLeetSpeakToggled(it) })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Text(text = "Целевая длина пароля", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(12, 16, 20).forEach { length ->
                        FilterChip(
                            selected = uiState.passwordLength == length,
                            onClick = { viewModel.onTargetLengthChanged(length) },
                            label = { Text(length.toString()) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(text = "Предпросмотр пароля", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = uiState.calculatedPassword?.let { String(it) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = { 
                                uiState.calculatedPassword?.let { 
                                    clipboardManager.setText(AnnotatedString(String(it))) 
                                } 
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Копировать")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
)
)
Spacer(modifier = Modifier.height(24.dp))
Row(
modifier = Modifier.fillMaxWidth(),
horizontalArrangement = Arrangement.End
) {
TextButton(onClick = {
phraseBuffer.fill('\u0000')
onDismissRequest()
}) {
Text("Отмена")
}
Spacer(modifier = Modifier.width(8.dp))
Button(onClick = { viewModel.generate() }, modifier = Modifier.padding(end = 8.dp)) {
Text("Рассчитать")
}
Button(
onClick = {
uiState.calculatedPassword?.let { onPasswordApplied(it.clone()) }
phraseBuffer.fill('\u0000')
onDismissRequest()
},
enabled = uiState.calculatedPassword != null
) {
Text("Утвердить")
}
}
}
}
}
}
