package com.example.teluguphotoquote

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * The 124 bundled drawable images (image_1.jpg .. image_124.jpg) used as
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
        R.drawable.image_10,
        R.drawable.image_11,
        R.drawable.image_12,
        R.drawable.image_13,
        R.drawable.image_14,
        R.drawable.image_15,
        R.drawable.image_16,
        R.drawable.image_17,
        R.drawable.image_18,
        R.drawable.image_19,
        R.drawable.image_20,
        R.drawable.image_21,
        R.drawable.image_22,
        R.drawable.image_23,
        R.drawable.image_24,
        R.drawable.image_25,
        R.drawable.image_26,
        R.drawable.image_27,
        R.drawable.image_28,
        R.drawable.image_29,
        R.drawable.image_30,
        R.drawable.image_31,
        R.drawable.image_32,
        R.drawable.image_33,
        R.drawable.image_34,
        R.drawable.image_35,
        R.drawable.image_36,
        R.drawable.image_37,
        R.drawable.image_38,
        R.drawable.image_39,
        R.drawable.image_40,
        R.drawable.image_41,
        R.drawable.image_42,
        R.drawable.image_43,
        R.drawable.image_44,
        R.drawable.image_45,
        R.drawable.image_46,
        R.drawable.image_47,
        R.drawable.image_48,
        R.drawable.image_49,
        R.drawable.image_50,
        R.drawable.image_51,
        R.drawable.image_52,
        R.drawable.image_53,
        R.drawable.image_54,
        R.drawable.image_55,
        R.drawable.image_56,
        R.drawable.image_57,
        R.drawable.image_58,
        R.drawable.image_59,
        R.drawable.image_60,
        R.drawable.image_61,
        R.drawable.image_62,
        R.drawable.image_63,
        R.drawable.image_64,
        R.drawable.image_65,
        R.drawable.image_66,
        R.drawable.image_67,
        R.drawable.image_68,
        R.drawable.image_69,
        R.drawable.image_70,
        R.drawable.image_71,
        R.drawable.image_72,
        R.drawable.image_73,
        R.drawable.image_74,
        R.drawable.image_75,
        R.drawable.image_76,
        R.drawable.image_77,
        R.drawable.image_78,
        R.drawable.image_79,
        R.drawable.image_80,
        R.drawable.image_81,
        R.drawable.image_82,
        R.drawable.image_83,
        R.drawable.image_84,
        R.drawable.image_85,
        R.drawable.image_86,
        R.drawable.image_87,
        R.drawable.image_88,
        R.drawable.image_89,
        R.drawable.image_90,
        R.drawable.image_91,
        R.drawable.image_92,
        R.drawable.image_93,
        R.drawable.image_94,
        R.drawable.image_95,
        R.drawable.image_96,
        R.drawable.image_97,
        R.drawable.image_98,
        R.drawable.image_99,
        R.drawable.image_100,
        R.drawable.image_101,
        R.drawable.image_102,
        R.drawable.image_103,
        R.drawable.image_104,
        R.drawable.image_105,
        R.drawable.image_106,
        R.drawable.image_107,
        R.drawable.image_108,
        R.drawable.image_109,
        R.drawable.image_110,
        R.drawable.image_111,
        R.drawable.image_112,
        R.drawable.image_113,
        R.drawable.image_114,
        R.drawable.image_115,
        R.drawable.image_116,
        R.drawable.image_117,
        R.drawable.image_118,
        R.drawable.image_119,
        R.drawable.image_120,
        R.drawable.image_121,
        R.drawable.image_122,
        R.drawable.image_123,
        R.drawable.image_124
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
