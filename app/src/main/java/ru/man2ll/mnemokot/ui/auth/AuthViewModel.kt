    fun onCreateProfile(name: String, pin: String) {
        if (name.isBlank() || pin.length < 4) {
            _errorMessage.value = "Заполните имя и PIN (мин. 4 цифры)"
            return
        }
        viewModelScope.launch {
            val pinChars = pin.toCharArray()
            // Используем фиксированную статическую соль для PIN-кодов, чтобы избежать циклической зависимости модулей
            val staticSalt = "MnemoKot_Pin_Salt".toByteArray(Charsets.UTF_8)
            val securedHash = cryptoEngine.hashPin(pinChars, staticSalt)
            pinChars.fill(' ')
            
            val newId = repository.createProfile(name, securedHash)
            sessionManager.selectProfile(newId)
        }
    }

    private fun verifyPin() {
        val profileId = sessionManager.activeProfileId.value ?: return
        viewModelScope.launch {
            val enteredPinChars = _pinInput.value.toCharArray()
            
            // Генерируем хэш с той же самой фиксированной солью
            val staticSalt = "MnemoKot_Pin_Salt".toByteArray(Charsets.UTF_8)
            val testHash = cryptoEngine.hashPin(enteredPinChars, staticSalt)
            enteredPinChars.fill(' ')

            val isValid = repository.verifyPin(profileId, testHash)

            if (isValid) {
                repository.resetFailedAttempts(profileId)
                _pinInput.value = ""
                sessionManager.authorizeSuccess()
            } else {
                _pinInput.value = ""
                val result = repository.incrementFailedAttempts(profileId)
                if (result == -1) {
                    sessionManager.logout()
                    _errorMessage.value = "Профиль уничтожен из-за брутфорса!"
                } else {
                    val remaining = repository.getRemainingAttempts(profileId)
                    _errorMessage.value = "Неверный PIN. Осталось попыток: $remaining"
                }
            }
        }
    }
