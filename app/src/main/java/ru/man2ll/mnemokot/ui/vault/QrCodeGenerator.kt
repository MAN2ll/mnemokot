package ru.man2ll.mnemokot.ui.vault

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

object QrCodeGenerator {

    /**
     * Генерирует Bitmap QR-кода, внутри которого зашита безопасная оффлайн-ссылка на ID записи.
     * Пароль внутри кода НЕ содержится.
     */
    fun generateAccountQr(accountId: Long, profileId: Long, serviceName: String): Bitmap? {
        // Формируем оффлайн-интент ссылку для быстрого поиска внутри приложения MnemoKot
        val qrContent = "mnemokot://vault/account?id=$accountId&profile=$profileId&service=$serviceName"
        
        val width = 512
        val height = 512
        
        return try {
            val bitMatrix = MultiFormatWriter().encode(
                qrContent,
                BarcodeFormat.QR_CODE,
                width,
                height
            )
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
