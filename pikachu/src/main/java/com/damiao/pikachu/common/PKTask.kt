package com.damiao.pikachu.common

import androidx.lifecycle.LifecycleObserver
import java.lang.RuntimeException

interface PKTask : LifecycleObserver{

    var progress: Long

    var contentLength: Long

    val taskId: String

    fun start()

    fun pause()

    fun isPause() : Boolean

    fun resume()

    fun cancel()

    fun success()

    fun fail(reason: String? = null, exception: RuntimeException? = null)

    fun cancelListener()

}