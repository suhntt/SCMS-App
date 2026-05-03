package com.example.scms.utils
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    /**
     * Compresses and resizes an image from a given File to save data and improve upload reliability.
     * @param context The application context
     * @param originalFile The raw photo file from the camera
     * @param maxWidth The target maximum width for the resized image
     * @param quality JPEG compression quality (1-100)
     * @return A new compressed File
     */
    fun compressImage(context: Context, originalFile: File, maxWidth: Int = 1024, quality: Int = 75): File {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(originalFile.absolutePath, options)

            // Calculate scale factor
            var scale = 1
            while (options.outWidth / scale / 2 >= maxWidth) {
                scale *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            val bitmap = BitmapFactory.decodeFile(originalFile.absolutePath, decodeOptions)

            val compressedFile = File(context.cacheDir, "compressed_${originalFile.name}")
            val out = FileOutputStream(compressedFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.flush()
            out.close()
            
            // Clean up original raw file to save space on device
            if (originalFile.exists()) originalFile.delete()
            
            compressedFile
        } catch (e: Exception) {
            originalFile // Fallback to original on error
        }
    }
}
