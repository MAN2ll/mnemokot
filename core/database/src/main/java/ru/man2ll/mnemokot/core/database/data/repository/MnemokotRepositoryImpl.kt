// Добавьте импорт в самый верх:
import ru.man2ll.mnemokot.core.security.crypto.SecurityCryptoEngine

// Обновите конструктор класса (добавьте инжект криптодвижка):
@Singleton
class MnemokotRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val accountDao: AccountDao,
    private val passwordGenerator: PasswordGenerator,
    private val cryptoEngine: SecurityCryptoEngine // Инжектируем сюда движок безопасности
) : MnemokotRepository {

    // ... все остальные методы остаются прежними ...

    override suspend fun verifyPin(profileId: Long, pin: String): Boolean {
        val profile = profileDao.getProfileById(profileId) ?: return false
        val storedHash = profile.pinHash ?: return true // Если PIN не установлен, пускаем сразу
        
        // КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: Вызываем правильный метод верификации, 
        // который распарсит строку на СОЛЬ и ХЭШ и выполнит PBKDF2 сравнение!
        return cryptoEngine.verifyPin(pin.toCharArray(), storedHash)
    }
}
