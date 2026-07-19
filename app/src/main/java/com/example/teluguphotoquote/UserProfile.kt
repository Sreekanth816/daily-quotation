package com.example.teluguphotoquote

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * The 10 bundled drawable images (image_1.jpg .. image_10.jpg) used as
 * random quote-photo backgrounds.
 */
object ImagePool {
    val drawableResIds: List<Int> = listOf(
        R.drawable.image_1,
        R.drawable.image_2,
        R.drawable.image_3,
        R.drawable.image_4,
        R.drawable.image_5,
        R.drawable.image_6,
        R.drawable.image_7,
        R.drawable.image_8,
        R.drawable.image_9,
        R.drawable.image_10
    )

    fun randomResId(exclude: Int? = null): Int {
        if (drawableResIds.size <= 1) return drawableResIds.first()
        var pick: Int
        do {
            pick = drawableResIds.random()
        } while (pick == exclude)
        return pick
    }
}

/**
 * The saved user profile: a display name plus the absolute path of the
 * profile picture the user uploaded (copied into app-private storage so it
 * keeps working even if the original gallery/camera file is later removed).
 */
data class UserProfile(val name: String, val profileImagePath: String)

/**
 * Simple SharedPreferences-backed store for the user profile. The profile
 * picture itself lives on disk (see [ProfileImageStore]); only its path is
 * kept here.
 */
object UserProfileStore {
    private const val PREFS_NAME = "telugu_photo_quote_prefs"
    private const val KEY_NAME = "profile_name"
    private const val KEY_IMAGE_PATH = "profile_image_path"
    private const val KEY_ONBOARDED = "onboarding_complete"

    fun isOnboarded(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ONBOARDED, false)
    }

    fun getProfile(context: Context): UserProfile? {
        val p = prefs(context)
        if (!p.getBoolean(KEY_ONBOARDED, false)) return null
        val name = p.getString(KEY_NAME, null) ?: return null
        val path = p.getString(KEY_IMAGE_PATH, null) ?: return null
        if (!File(path).exists()) return null
        return UserProfile(name = name, profileImagePath = path)
    }

    fun saveProfile(context: Context, name: String, profileImagePath: String) {
        prefs(context).edit()
            .putString(KEY_NAME, name)
            .putString(KEY_IMAGE_PATH, profileImagePath)
            .putBoolean(KEY_ONBOARDED, true)
            .apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

/**
 * Persists a user-picked profile picture (from gallery, camera, or an
 * in-memory bitmap) into app-private internal storage so it survives across
 * app restarts and isn't affected by the original source file being deleted.
 */
object ProfileImageStore {
    private const val FILE_NAME = "profile_picture.jpg"

    /** Copies the bytes at [uri] into internal storage and returns the saved file's absolute path. */
    fun saveFromUri(context: Context, uri: Uri): String {
        val destFile = File(context.filesDir, FILE_NAME)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        return destFile.absolutePath
    }

    /** Saves an in-memory [bitmap] (e.g. straight from the camera) into internal storage. */
    fun saveFromBitmap(context: Context, bitmap: Bitmap): String {
        val destFile = File(context.filesDir, FILE_NAME)
        FileOutputStream(destFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
        }
        return destFile.absolutePath
    }
}
