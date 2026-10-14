package ru.man2ll.mnemokot.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.man2ll.mnemokot.core.database.domain.model.UserProfile

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pinInput by viewModel.pinInput.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is AuthUiState.Loading -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            is AuthUiState.ProfileSelection -> {
                ProfileSelectionView(
                    profiles = state.profiles,
                    onSelect = viewModel::onSelectProfile,
                    onCreateClick = viewModel::navigateToCreateProfile
                )
            }
            is AuthUiState.CreateProfile -> {
                CreateProfileView(
                    onCreate = viewModel::onCreateProfile,
                    onCancel = viewModel::cancelCreateProfile,
                    error = errorMessage,
                    showCancelButton = state != AuthUiState.CreateProfile // Показываем отмену, если профили вообще есть
                )
            }
            is AuthUiState.EnterPin -> {
                EnterPinView(
                    profileName = state.profileName,
                    remainingAttempts = state.remainingAttempts,
                    pinInput = pinInput,
                    error = errorMessage,
                    onKeyClick = viewModel::onPinKeyClick,
                    onBackClick = viewModel::cancelCreateProfile
                )
            }
            is AuthUiState.Authenticated -> {
                LaunchedEffect(Unit) {
                    onAuthSuccess()
                }
                CircularProgressIndicator(color = Color.Green)
            }
        }
    }
}

@Composable
fun ProfileSelectionView(
    profiles: List<UserProfile>,
    onSelect: (UserProfile) -> Unit,
    onCreateClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Выберите профиль сейфа",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            items(profiles) { profile ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(profile) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = profile.profileName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Создать новый профиль")
        }
    }
}

@Composable
fun CreateProfileView(
    onCreate: (String, String) -> Unit,
    onCancel: () -> Unit,
    error: String?,
    showCancelButton: Boolean
) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Создание профиля MnemoKot",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Имя профиля (Личный, Работа...)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 6) pin = it },
            label = { Text("Установите PIN (4-6 цифр)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        
        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { onCreate(name, pin) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Создать и войти")
        }

        if (showCancelButton) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Назад к выбору профилей")
            }
        }
    }
}

@Composable
fun EnterPinView(
    profileName: String,
    remainingAttempts: Int,
    pinInput: String,
    error: String?,
    onKeyClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Профиль: $profileName", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Введите PIN-код для разблокировки сейфа", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))

        // Индикаторы ввода PIN
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(4) { index ->
                val isFilled = index < pinInput.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isFilled) MaterialTheme.colorScheme.primary else Color.DarkGray)
                )
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Клавиатура 3х4
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "C")
        )

        for (row in keys) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                for (key in row) {
                    if (key.isEmpty()) {
                        Spacer(modifier = Modifier.size(64.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onKeyClick(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(key, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
