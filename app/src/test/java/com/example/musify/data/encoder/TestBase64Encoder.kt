package com.example.musify.data.encoder

import com.example.musify.BuildConfig
import java.util.Base64

/**
 * This is a [Base64Encoder] that can be used for unit tests.
 * Since [AndroidBase64Encoder] uses [android.util.Base64] it can't
 * be used in tests without mocking. This class uses [java.util.Base64]
 * as a substitute for it.
 * Note: The value generated by [android.util.Base64.encodeToString] is
 * equivalent to [java.util.Base64] only when [android.util.Base64.NO_WRAP]
 * is used in conjunction with [android.util.Base64.encodeToString].
 */
class TestBase64Encoder : Base64Encoder {
    override fun encodeToString(input: ByteArray): String = Base64
        .getEncoder()
        .encodeToString("${BuildConfig.SPOTIFY_CLIENT_ID}:${BuildConfig.SPOTIFY_CLIENT_SECRET}".toByteArray())
}

