package com.mnemokot.app.presentation.mnemonic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.mnemokot.core.crypto.domain.model.MnemonicMethod

@Composable
fun MnemonicGeneratorDialog(
    onDismissRequest: () -> Unit,
    onPasswordApplied: (String) -> Unit,
    viewModel: MnemonicGeneratorViewModel = hiltViewModel()
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
