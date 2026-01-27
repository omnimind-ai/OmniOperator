package cn.com.omnimind.omnibot.util

import android.util.Log

object OmniLog {
    enum class Level {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        ASSERT,
        DISABLE,
    }

    /**
     * log params
     */
    private var globalTag = "[Omni]"
    private var minLevel = Level.VERBOSE

    /**
     * get Log level
     */
    fun getLogLevel(): Level = minLevel

    /**
     * set Log level
     * set to DISABLE means disable OmniLog
     */
    fun setLogLevel(level: Level) {
        this.minLevel = level
    }

    /**
     * log verbose
     * Place at the beginning of each function call to monitor function invocation
     */
    @JvmOverloads
    fun v(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (minLevel.ordinal > Level.VERBOSE.ordinal) return
        val actualTag = globalTag + tag
        Log.v(actualTag, message, throwable)
    }

    /**
     * log debug
     * Record event outcome details
     */
    @JvmOverloads
    fun d(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (minLevel.ordinal > Level.DEBUG.ordinal) return
        val actualTag = globalTag + tag
        Log.d(actualTag, message, throwable)
    }

    /**
     * log info
     * Track user behavior and record critical data
     */
    @JvmOverloads
    fun i(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (minLevel.ordinal > Level.INFO.ordinal) return
        val actualTag = globalTag + tag
        Log.i(actualTag, message, throwable)
    }

    /**
     * log warn
     * Record handled exceptions that won't terminate the application
     */
    @JvmOverloads
    fun w(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (minLevel.ordinal > Level.WARN.ordinal) return
        val actualTag = globalTag + tag
        Log.w(actualTag, message, throwable)
    }

    /**
     * log error
     * Record serious failures that affect functionality
     */
    @JvmOverloads
    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (minLevel.ordinal > Level.ERROR.ordinal) return
        val actualTag = globalTag + tag
        Log.e(actualTag, message, throwable)
    }

    /**
     * log assert
     * Record critical errors that should never occur (what the fuck message)
     */
    @JvmOverloads
    fun wtf(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (minLevel.ordinal > Level.ASSERT.ordinal) return
        val actualTag = globalTag + tag
        Log.wtf(actualTag, message, throwable)
    }
}
