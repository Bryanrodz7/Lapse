package dev.randyapps.lapse.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

@RunWith(AndroidJUnit4::class)
class InternalPhotoStoreTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private lateinit var store: InternalPhotoStore
    private val sources = mutableListOf<File>()

    @Before
    fun setUp() {
        store = InternalPhotoStore(context)
    }

    @After
    fun tearDown() {
        sources.forEach { it.delete() }
        File(context.filesDir, InternalPhotoStore.DIRECTORY).listFiles()?.forEach { it.delete() }
    }

    /** Writes a JPEG of the given size to the cache and returns a Uri for it. */
    private fun sourceImage(width: Int, height: Int): Uri {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFF3366AA.toInt())
        val file = File(context.cacheDir, "src-${width}x$height.jpg").also { sources += it }
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        bitmap.recycle()
        return Uri.fromFile(file)
    }

    private fun dimensionsOf(path: String): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        return options.outWidth to options.outHeight
    }

    @Test
    fun aLargePhotoIsDownscaledBeforeItIsStored() = runTest {
        val path = store.save(sourceImage(4000, 3000))
        assertNotNull("save should return a path", path)

        val (w, h) = dimensionsOf(path!!)
        assertTrue(
            "longest edge should be capped at ${InternalPhotoStore.MAX_DIMENSION}, was ${max(w, h)}",
            max(w, h) <= InternalPhotoStore.MAX_DIMENSION,
        )
    }

    @Test
    fun downscalingPreservesTheAspectRatio() = runTest {
        val path = store.save(sourceImage(4000, 3000))!!
        val (w, h) = dimensionsOf(path)
        // 4:3 within a pixel of rounding.
        assertEquals(4f / 3f, w.toFloat() / h.toFloat(), 0.01f)
    }

    @Test
    fun aSmallPhotoIsNotUpscaled() = runTest {
        val path = store.save(sourceImage(400, 300))!!
        assertEquals(400 to 300, dimensionsOf(path))
    }

    @Test
    fun theStoredFileIsInsideAppInternalStorage() = runTest {
        val path = store.save(sourceImage(2000, 1500))!!
        val file = File(path)

        assertTrue("must live under filesDir", file.absolutePath.startsWith(context.filesDir.absolutePath))
        assertEquals(InternalPhotoStore.DIRECTORY, file.parentFile!!.name)
        assertTrue(file.exists())
    }

    @Test
    fun theStoredFileIsSubstantiallySmallerThanTheOriginal() = runTest {
        val sourceUri = sourceImage(4000, 3000)
        val originalSize = File(sourceUri.path!!).length()

        val stored = File(store.save(sourceUri)!!)
        assertTrue(
            "stored ${stored.length()} should be well under original $originalSize",
            stored.length() < originalSize,
        )
    }

    @Test
    fun eachSaveGetsItsOwnFile() = runTest {
        val first = store.save(sourceImage(800, 600))!!
        val second = store.save(sourceImage(800, 600))!!
        assertTrue("paths must not collide", first != second)
        assertTrue(File(first).exists() && File(second).exists())
    }

    @Test
    fun deleteRemovesTheStoredFile() = runTest {
        val path = store.save(sourceImage(800, 600))!!
        store.delete(path)
        assertFalse(File(path).exists())
    }

    @Test
    fun deleteToleratesNullAndUnknownPaths() = runTest {
        store.delete(null)
        store.delete("")
        store.delete("/data/does/not/exist.jpg")
    }

    @Test
    fun deleteRefusesPathsOutsideThePhotoDirectory() = runTest {
        // Guards against a corrupt or hostile stored path removing something else.
        val outsider = File(context.filesDir, "not-a-photo.txt").apply { writeText("keep me") }
        store.delete(outsider.absolutePath)
        assertTrue("files outside the photo directory must be left alone", outsider.exists())
        outsider.delete()
    }

    @Test
    fun anUnreadableSourceReturnsNullRatherThanThrowing() = runTest {
        assertNull(store.save(Uri.fromFile(File(context.cacheDir, "nope.jpg"))))
    }
}
