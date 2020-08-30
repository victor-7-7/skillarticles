package ru.skillbranch.skillarticles.data.providers

import android.net.Uri
import androidx.core.content.FileProvider

// Класс создан для того, чтобы делиться изображениями с другими приложениями
class ImageFileProvider : FileProvider() {
    override fun getType(uri: Uri): String? {
//        return super.getType(uri)
        return "image/jpeg"
    }
}