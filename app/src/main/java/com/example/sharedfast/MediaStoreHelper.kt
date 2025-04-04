package com.example.sharedfast

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MediaStoreHelper {

    fun saveImageToMediaStore(context: Context, folderName: String, imageUri: Uri): String? {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "SharedFast_${folderName}_$timeStamp.jpg"

            // Create appropriate ContentValues
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

                // For Android 10 and above, use RELATIVE_PATH
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/SharedFast/$folderName")
                }
            }

            // Insert into MediaStore and get new URI
            val resolver = context.contentResolver
            val destinationUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (destinationUri != null) {
                // Copy input stream to output stream
                resolver.openInputStream(imageUri)?.use { input ->
                    resolver.openOutputStream(destinationUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                return destinationUri.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun saveCapturedImageToMediaStore(context: Context, folderName: String, sourcePath: String): String? {
        try {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) return null

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "SharedFast_${folderName}_$timeStamp.jpg"

            // Create appropriate ContentValues
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

                // For Android 10 and above, use RELATIVE_PATH
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/SharedFast/$folderName")
                }
            }

            // Insert into MediaStore and get new URI
            val resolver = context.contentResolver
            val destinationUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (destinationUri != null) {
                // Copy source file to destination
                val bitmap = BitmapFactory.decodeFile(sourcePath)
                resolver.openOutputStream(destinationUri)?.use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                }
                return destinationUri.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}