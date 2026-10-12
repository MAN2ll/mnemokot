package ru.man2ll.mnemokot.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pinInput by viewModel.pinInput.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Authenticated) {
            onAuthSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is AuthUiState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            is AuthUiState.CreateProfile -> CreateProfileView(onCreate = viewModel::onCreateProfile, error = errorMessage)
            is AuthUiState.EnterPin -> EnterPinView(
                profileName = state.profileName,
                pinInput = pinInput,
                error = errorMessage,
                onKeyClick = viewModel::onPinKeyClick
            )
            is AuthUiState.Authenticated -> CircularProgressIndicator(color = Color.Green)
        }
    }
}

@Composable
fun CreateProfileView(onCreate: (String, String) -> Unit, error: String?) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Создание профиля MnemoKot", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя профиля") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = pin, 
            onValueChange = { if (it.length <= 6) pin = it }, 
            label = { Text("Установите PIN (4-6 цифр)") },
            visualTransformation = PasswordVisualTransformation()
        )
        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onCreate(name, pin) }) {
            Text("Создать и войти")
        }
    }
}

@Composable
fun EnterPinView(profileName: String, pinInput: String, error: String?, onKeyClick: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Профиль: $profileName", fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Введите PIN-код для разблокировки", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))
        
        // Индикаторы точек PIN-кода
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

        Spacer(modifier = Modifier.height(40.dp))
        
        // Цифровая клавиатура 3х4
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "C")
        )

        for (row in keys) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                for (key in row) {
                    if (key.isEmpty()) {
                        Spacer(modifier = Modifier.size(64.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onKeyClick(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(key, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
