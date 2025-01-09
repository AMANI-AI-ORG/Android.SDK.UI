package ai.amani.sdk.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


internal object BitmapUtils {


    /**Convert bitmap to File? object.
     *
     * @param bitmap : Input bitmap
     * @param context : Activity pointer
     */
    fun saveBitmapAsFile(bitmap: Bitmap?, name: String, context: Context):File? {
        if (bitmap == null) return null
        val filesDir: File = context.filesDir
        val imageFile = File(filesDir, "$name.jpg")
        val os: OutputStream
        return try {
            os = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
            os.flush()
            os.close()
            imageFile
        } catch (e: Exception) {
            null
        }
    }
}

