package ru.man2ll.mnemokot.ui.vault

import com.journeyapps.barcodescanner.CaptureActivity

/**
 * Кастомная Activity сканера камеры.
 * Расширяет CaptureActivity из ZXing Embedded для фиксации портретной ориентации
 * и автономной работы без сетевых запросов.
 */
class QrScannerActivity : CaptureActivity() {
    // Оставляем тело пустым, конфигурация подтягивается аппаратно через Manifest
}
