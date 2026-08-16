package dev.randyapps.lapse.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Writes photos to `filesDir/photos`, which is app-internal: not readable by other apps, not on
 * external storage, and removed with the app.
 *
 * Every image is downscaled and re-encoded before it is written. A modern phone camera produces
 * a 12MP file of several megabytes; the app only ever shows this at thumbnail size, so storing
 * the original would waste space for no visible benefit.
 */
@Singleton
class InternalPhotoStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : PhotoStore {

    override suspend fun save(source: Uri): String? = withContext(Dispatchers.IO) {
        val bitmap = decodeDownscaled(source) ?: return@withContext null
        val directory = File(context.filesDir, DIRECTORY).apply { mkdirs() }
        val target = File(directory, "${UUID.randomUUID()}.jpg")

        val written = runCatching {
            FileOutputStream(target).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            }
        }.isSuccess
        bitmap.recycle()

        if (written) {
            target.absolutePath
        } else {
            target.delete()
            null
        }
    }

    override suspend fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        withContext(Dispatchers.IO) {
            val file = File(path)
            // Only ever delete inside our own photo directory, so a corrupt or hostile path
            // can't be used to remove something else.
            if (file.parentFile?.absolutePath == File(context.filesDir, DIRECTORY).absolutePath) {
                file.delete()
            }
        }
    }

    private fun decodeDownscaled(source: Uri): Bitmap? = runCatching {
        // Pass 1: read the dimensions only, so a huge image never has to fit in memory.
        //
        // decodeStream returns null by design when inJustDecodeBounds is set — the result is in
        // `bounds`, not the return value. Null-checking the decode here would reject every
        // image; check the stream and the decoded dimensions instead.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = context.contentResolver.openInputStream(source)
            ?: return@runCatching null
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        // Pass 2: decode at the nearest power-of-two subsample, then scale the rest of the way.
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        val decoded = context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return@runCatching null

        val rotated = applyExifRotation(source, decoded)
        scaleToBound(rotated)
    }.getOrNull()

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= MAX_DIMENSION) sample *= 2
        return sample
    }

    private fun scaleToBound(bitmap: Bitmap): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= MAX_DIMENSION) return bitmap

        val ratio = MAX_DIMENSION.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).roundToInt().coerceAtLeast(1),
            (bitmap.height * ratio).roundToInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    /**
     * Cameras record orientation in EXIF rather than rotating the pixels, so a card photographed
     * in portrait decodes sideways unless this is applied.
     */
    private fun applyExifRotation(source: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(source)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    companion object {
        const val DIRECTORY = "photos"

        /** Longest edge. Plenty for a full-screen view, a fraction of a 12MP original. */
        const val MAX_DIMENSION = 1600
        private const val QUALITY = 85
    }
}
