package com.elutto.app.util

import android.util.Log

/**
 * Log helper
 */
object LogUtil {
    private val LOG_PREFIX = "elutto:"

    private val MAX_TAG_LENGTH = 23
    private val LOG_PREFIX_LENGTH = LOG_PREFIX.length

    fun makeLogTag(clazz: Class<*>): String {
        return makeLogTag(clazz.simpleName)
    }


    fun makeLogTag(str: String): String {
        return if (str.length > MAX_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            LOG_PREFIX + str.substring(0, MAX_TAG_LENGTH - LOG_PREFIX_LENGTH - 1)
        } else LOG_PREFIX + str

    }


    fun logD(tag: String, message: String) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message)
        }
    }

    fun logD(tag: String, message: String, cause: Throwable) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message, cause)
        }
    }

    fun logV(tag: String, message: String) {
        if (Log.isLoggable(tag, Log.VERBOSE)) {
            Log.v(tag, message)
        }
    }

    fun logV(tag: String, message: String, cause: Throwable) {
        if (Log.isLoggable(tag, Log.VERBOSE)) {
            Log.v(tag, message, cause)
        }
    }

    fun logI(tag: String, message: String) {
        var threadId = Thread.currentThread().id
        Log.i(tag, "[$threadId] $message")
    }

    fun logI(tag: String, message: String, cause: Throwable) {
        Log.i(tag, message, cause)
    }

    fun logW(tag: String, message: String) {
        Log.w(tag, message)
    }

    fun logw(tag: String, message: String, cause: Throwable) {
        Log.w(tag, message, cause)
    }

    fun logE(tag: String, message: String) {
        Log.e(tag, message)
    }

    fun logE(tag: String, message: String, cause: Throwable) {
        Log.e(tag, message, cause)
    }
}
