package ru.man2ll.mnemokot.core.database.domain.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {

    private val _activeProfileId = MutableStateFlow<Long?>(null)
    val activeProfileId: StateFlow<Long?> = _activeProfileId.asStateFlow()

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    fun selectProfile(profileId: Long) {
        _activeProfileId.value = profileId
        _isAuthorized.value = false // Сбрасываем авторизацию, требуя ввести PIN для нового профиля
    }

    fun authorizeSuccess() {
        _isAuthorized.value = true
    }

    fun logout() {
        _isAuthorized.value = false
        _activeProfileId.value = null
    }
}
