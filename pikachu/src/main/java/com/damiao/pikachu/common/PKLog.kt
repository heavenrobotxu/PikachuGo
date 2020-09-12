package com.damiao.pikachu.common

import android.util.Log

internal object PKLog {

    private const val PIKACHU_TAG: String = "-PIKACHU-"

    fun debug(info: String) {
        Log.d(PIKACHU_TAG + Thread.currentThread().name, info)
    }

    fun info(info: String) {
        Log.i(PIKACHU_TAG + Thread.currentThread().name, info)
    }

    fun error(error: String) {
        Log.e(PIKACHU_TAG + Thread.currentThread().name, error)
    }
}