package ru.man2ll.mnemokot.ui.auth

import androidx.activity.compose.BackHandler
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
    val pinLength by viewModel.pinLengthState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    BackHandler(enabled = uiState !is AuthUiState.ProfileSelection) {
        viewModel.cancelCreateProfile()
    }

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
                    onSelect = { userProfile -> viewModel.onSelectProfile(userProfile) },
                    onCreateClick = viewModel::navigateToCreateProfile
                )
            }
            is AuthUiState.CreateProfile -> {
                CreateProfileView(
                    onCreate = viewModel::onCreateProfile,
                    onCancel = viewModel::cancelCreateProfile,
                    error = errorMessage
                )
            }
            is AuthUiState.EnterPin -> {
                EnterPinView(
                    profileName = state.profileName,
                    pinLength = pinLength,
                    error = errorMessage,
                    onKeyClick = viewModel::onPinKeyClick,
                    onBackClick = viewModel::cancelCreateProfile,
                    onConfirmClick = viewModel::triggerManualVerification
                )
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
    onCreate: (String, CharArray) -> Unit,
    onCancel: () -> Unit,
    error: String?
) {
    var name by remember { mutableStateOf("") }
    val pinBuffer = remember { CharArray(6) { '\u0000' } }
    var pinLength by remember { mutableStateOf(0) }

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
            value = String(pinBuffer, 0, pinLength),
            onValueChange = { input ->
                val clean = input.filter { it.isDigit() }.take(6)
                pinLength = clean.length
                for (i in 0 until 6) {
                    pinBuffer[i] = if (i < clean.length) clean[i] else '\u0000'
                }
            },
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
            onClick = {
                val finalPin = CharArray(pinLength) { pinBuffer[it] }
                onCreate(name, finalPin)
                pinBuffer.fill('\u0000')
                pinLength = 0
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Создать и войти")
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Назад к выбору профилей")
        }
    }
}

@Composable
fun EnterPinView(
    profileName: String,
    pinLength: Int,
    error: String?,
    onKeyClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onConfirmClick: () -> Unit
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
        Text(text = "Введите PIN-код", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(16.dp)) {
            if (pinLength == 0) {
                Text("-", fontSize = 16.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
            } else {
                repeat(pinLength) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

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
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onConfirmClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Войти", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}
