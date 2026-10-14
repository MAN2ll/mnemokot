    private fun verifyPin() {
        val profileId = sessionManager.activeProfileId.value ?: return
        viewModelScope.launch {
            val enteredPinChars = _pinInput.value.toCharArray()
            val profiles = repository.getProfiles().first()
            val activeProfile = profiles.firstOrNull { it.id == profileId }

            if (activeProfile != null) {
                // ИСПРАВЛЕНИЕ: Запрашиваем полный профиль из БД через DAO для доступа к полю pinHash
                val dbProfiles = repository.getProfiles().first()
                
                val storedHash = repository.getProfiles().first()
                    .firstOrNull { it.id == profileId }?.let {
                        // Чтобы получить доступ к сырому хэшу, нам нужно вытащить его.
                        // Для этого временно сделаем проверку PIN прямо через сверку хэша из репозитория.
                    }
            }
        }
    }
