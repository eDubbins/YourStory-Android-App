package com.example.yourstory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.File
import java.io.FileOutputStream

class ImageStorage(private val context: Context) {

    // Saves Base64 string to a file, returns the File Path
    fun saveImage(base64String: String, filename: String): String? {
        return try {
            // 1. Clean the string
            val cleanBase64 = base64String.substringAfter(",")
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)

            // 2. Create file in app's private storage
            val file = File(context.filesDir, "$filename.jpg")
            val fos = FileOutputStream(file)

            // 3. Write data
            fos.write(decodedBytes)
            fos.close()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}