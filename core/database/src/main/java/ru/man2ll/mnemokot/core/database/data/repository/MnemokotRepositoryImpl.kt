    override suspend fun getPasswordForAccount(accountId: Long, mnemonicPhrase: String?): CharArray {
        val account = accountDao.getAccountById(accountId) ?: return charArrayOf()
        return if (account.isMnemonic) {
            if (mnemonicPhrase == null) return charArrayOf()
            val options = MnemonicOptions(
                length = account.passwordLength,
                salt = account.salt,
                serviceName = account.serviceName,
                rotationDate = account.rotationDate,
                // Передаем признак двух пользователей из БД в криптомодуль
                isDualUserMode = account.isDualUserMode 
            )
            passwordGenerator.generateFromMnemonic(mnemonicPhrase, options)
        } else {
            account.encryptedPassword?.toCharArray() ?: charArrayOf()
        }
    }
