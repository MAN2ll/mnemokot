package ru.man2ll.mnemokot.presentation.mnemonic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.mnemokot.core.crypto.domain.model.MnemonicMethod


@Composable
fun MnemonicGeneratorDialog(
    viewModel: MnemonicGeneratorViewModel,
    onDismissRequest: () -> Unit,
    onPasswordApplied: (String) -> Unit
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
                        text = "🐈 Комбайн Мнемотехник",
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

                val methods = listOf(
                    MnemonicMethod.AMPG_V2 to "AMPG v2",
                    MnemonicMethod.QWERTY_SHIFT to "QWERTY сдвиг",
                    MnemonicMethod.ACRONYMS to "Акронимы"
                )
                val selectedIndex = methods.indexOfFirst { it.first == uiState.config.method }

                TabRow(selectedTabIndex = selectedIndex) {
                    methods.forEachIndexed { index, pair ->
                        Tab(
                            selected = selectedIndex == index,
                            onClick = { viewModel.onMethodChanged(pair.first) },
                            text = { Text(pair.second, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = uiState.isHelpVisible,
                    enter = expandVertically(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300))
                ) {
                    val helpText = when (uiState.config.method) {
                        MnemonicMethod.AMPG_V2 -> "AMPG v2: Переводит кириллицу в транслит ГОСТ, пропорционально усекает длину каждого слова (сохраняя корни) и мутирует повторяющиеся символы в спецсимволы по Leet-карте."
                        MnemonicMethod.QWERTY_SHIFT -> "QWERTY сдвиг: Заменяет русские буквы на английские символы, расположенные на тех же клавишах клавиатуры. Идеально воспроизводит слепой ввод в неверной раскладке."
                        MnemonicMethod.ACRONYMS -> "Акронимы: Вырезает только первые буквы из каждого слова вашей фразы, после чего переводит их в транслит ГОСТ. Позволяет создавать ультра-компактные пароли."
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = helpText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = uiState.config.phrase,
                    onValueChange = { viewModel.onPhraseChanged(it) },
                    label = { Text("Мнемоническая фраза") },
                    placeholder = { Text("Введите длинную фразу для преобразования...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.config.serviceName,
                        onValueChange = { viewModel.onServiceNameChanged(it) },
                        label = { Text("Сервис") },
                        placeholder = { Text("github") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = uiState.config.dateContext,
                        onValueChange = { viewModel.onDateContextChanged(it) },
                        label = { Text("Дата (ММГГ)") },
                        placeholder = { Text("1026") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.config.anchorWord,
                    onValueChange = { viewModel.onAnchorWordChanged(it) },
                    label = { Text("Якорное слово (Соль)") },
                    placeholder = { Text("СекретноеСлово") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Суффиксы безопасности в конце пароля",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = uiState.config.includeService,
                            onCheckedChange = { viewModel.onIncludeServiceChanged(it) }
                        )
                        Text("Включить имя сервиса (3 буквы в UPPER)", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = uiState.config.includeDate,
                            onCheckedChange = { viewModel.onIncludeDateChanged(it) }
                        )
                        Text("Включить контекстную дату (ММГГ)", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = uiState.config.includeAnchor,
                            onCheckedChange = { viewModel.onIncludeAnchorChanged(it) }
                        )
                        Text("Включить якорное слово", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.config.method == MnemonicMethod.AMPG_V2) {
                    Text(
                        text = "Специфичные настройки метода",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Сохранять корни слов", style = MaterialTheme.typography.bodyMedium)
                            Text("Пропорциональное усечение длины", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = uiState.config.keepRoots, onCheckedChange = { viewModel.onKeepRootsChanged(it) })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Мутация повторов (Leet)", style = MaterialTheme.typography.bodyMedium)
                            Text("Подстановка спецсимволов вместо удаления дубликатов", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = uiState.config.mutateRepeats, onCheckedChange = { viewModel.onMutateRepeatsChanged(it) })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(text = "Целевая длина пароля", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(12, 16, 20).forEach { length ->
                        FilterChip(
                            selected = uiState.config.targetLength == length,
                            onClick = { viewModel.onTargetLengthChanged(length) },
                            label = { Text(length.toString()) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(text = "Предпросмотр пароля", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = uiState.steps.finalPassword,
                    onValueChange = {},
                    readOnly = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(uiState.steps.finalPassword)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Копировать")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(text = "Этапы трансформации", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column {
                        Text(text = "1. Входная фраза:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(text = uiState.steps.sourcePhrase.ifEmpty { "..." }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column {
                        Text(text = "2. Базовая трансформация метода:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(text = uiState.steps.baseProcessed.ifEmpty { "..." }, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column {
                        Text(text = "3. Пост-обработка (Усечение/Мутация):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(text = uiState.steps.postProcessed.ifEmpty { "..." }, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column {
                        Text(text = "4. Сформированный суффикс:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(text = uiState.steps.generatedSuffix.ifEmpty { "отсутствует" }, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column {
                        Text(text = "5. Утвержденный результат:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(text = uiState.steps.finalPassword.ifEmpty { "..." }, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onPasswordApplied(uiState.steps.finalPassword)
                            onDismissRequest()
                        },
                        enabled = uiState.steps.finalPassword.isNotEmpty()
                    ) {
                        Text("Утвердить пароль")
                    }
                }
            }
        }
    }
}
