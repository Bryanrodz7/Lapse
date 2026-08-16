package dev.randyapps.lapse.data.photo

import android.net.Uri

/**
 * Owns the app's copy of a photo.
 *
 * Photos are copied into app-internal storage on selection and never referenced by their
 * original content Uri: a Photo Picker grant is temporary, so a stored Uri would quietly stop
 * resolving later. An interface so ViewModels stay JVM-testable.
 */
interface PhotoStore {

    /**
     * Copies [source] into internal storage, downscaled and re-encoded. Returns the absolute
     * path, or null if the image could not be read.
     */
    suspend fun save(source: Uri): String?

    /** Removes a stored photo. Safe to call with null or an already-deleted path. */
    suspend fun delete(path: String?)
}

/** Used by tests and anywhere photo side effects are not wanted. */
object NoOpPhotoStore : PhotoStore {
    override suspend fun save(source: Uri): String? = null
    override suspend fun delete(path: String?) = Unit
}
