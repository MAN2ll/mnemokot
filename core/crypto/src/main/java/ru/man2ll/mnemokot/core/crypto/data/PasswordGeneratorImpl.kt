    private fun adjustToRequiredLength(base: String, targetLength: Int): String {
        if (base.length >= targetLength) {
            return base.substring(0, targetLength)
        }
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(base.toByteArray(Charsets.UTF_8))
        val hexAppend = hashBytes.joinToString("") { "%02x".format(it) }
        
        val seenChars = mutableSetOf<Char>()
        base.forEach { seenChars.add(it.lowercaseChar()) }
        
        val finalBuilder = StringBuilder(base)
        for (ch in hexAppend) {
            val lowerCh = ch.lowercaseChar()
            if (!seenChars.contains(lowerCh) && finalBuilder.length < targetLength) {
                seenChars.add(lowerCh)
                finalBuilder.append(ch)
            }
        }
        
        while (finalBuilder.length < targetLength) {
            finalBuilder.append('!')
        }
        
        return finalBuilder.toString()
    }
