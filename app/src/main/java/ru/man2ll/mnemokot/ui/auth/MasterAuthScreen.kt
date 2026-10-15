package ru.man2ll.mnemokot.ui.auth

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat

@Composable
fun MasterAuthScreen(
    viewModel: MasterAuthViewModel,
    onAccessGranted: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    // Метод вызова аппаратного сканера отпечатка пальца с мягким подавлением отмены
    fun showBiometricPrompt() {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)
        
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.onBiometricSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // ФИКС: Если пользователь нажал кнопку отмены или "Ввести пароль" — не закрываем приложение
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("MnemoKot: Вход по отпечатку")
            .setSubtitle("Приложите палец к сканеру для разблокировки")
            .setNegativeButtonText("Ввести пароль")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    LaunchedEffect(uiState) {
        if (uiState is MasterUiState.AccessGranted) {
            onAccessGranted()
        } else if (uiState is MasterUiState.EnterMaster && (uiState as MasterUiState.EnterMaster).isBiometricAvailable) {
            showBiometricPrompt()
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        when (val state = uiState) {
            is MasterUiState.Loading -> CircularProgressIndicator()
            is MasterUiState.SetupMaster -> {
                MasterSetupView(
                    onSave = { pass, enableBio -> 
                        viewModel.setBiometricPreference(enableBio)
                        viewModel.onSetupMasterPassword(pass)
                    }, 
                    error = errorMessage
                )
            }
            is MasterUiState.EnterMaster -> {
                MasterLoginView(
                    isBioAvailable = state.isBiometricAvailable,
                    onVerify = viewModel::onVerifyMasterPassword,
                    onBioClick = { showBiometricPrompt() },
                    error = errorMessage
                )
            }
            is MasterUiState.AccessGranted -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
@Composable
fun MasterSetupView(onSave: (String, Boolean) -> Unit, error: String?) {
    var pass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var enableBiometric by remember { mutableStateOf(true) } // По умолчанию предлагаем биометрию
    var localError by remember { mutableStateOf<String?>(null) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Первичная защита MnemoKot", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Придумайте главный Мастер-пароль для шифрования и входа в приложение:", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Придумайте Мастер-пароль") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = confirmPass, onValueChange = { confirmPass = it }, label = { Text("Повторите Мастер-пароль") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(16.dp))
        // ОПЦИЯ: Включение биометрии прямо на этапе создания пароля
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Switch(checked = enableBiometric, onCheckedChange = { enableBiometric = it })
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Вход по отпечатку пальца", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Использовать биометрию устройства", fontSize = 11.sp, color = Color.Gray)
            }
        }

        val displayError = error ?: localError
        if (displayError != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(displayError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (pass != confirmPass) { localError = "Пароли не совпадают!" }
                else { localError = null; onSave(pass, enableBiometric) }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Установить глобальную защиту") }
    }
}

@Composable
fun MasterLoginView(isBioAvailable: Boolean, onVerify: (String) -> Unit, onBioClick: () -> Unit, error: String?) {
    var pass by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("MnemoKot заблокирован", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Введите глобальный Мастер-пароль") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        
        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { onVerify(pass) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
            Text("Разблокировать приложение")
        }
        
        if (isBioAvailable) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onBioClick, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                Text("🐾 Войти по отпечатку пальца")
            }
        }
    }
}
