package com.damiao.pikachu.common

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import okhttp3.internal.notify
import okhttp3.internal.notifyAll
import java.io.File

class PKRealDownloadTask(
    override val pkRequest: PKDownloadTaskRequest,
    override val taskId: String,
    @Volatile
    override var downloadFileName: String? = null
) : PKDownloadTask {

    @Volatile
    override var taskAlreadyDone = false

    @Volatile
    override var downloadResultFile: File? = null

    @Volatile
    override var progress: Long = 0

    @Volatile
    override var contentLength: Long = 0

    @Volatile
    private var status: Int = TASK_STATUS_READY

    @Volatile
    private var failType: Int? = null


    override fun start() {
        status = TASK_STATUS_EXECUTING
    }

    override fun pause() {
        status = TASK_STATUS_PAUSE
    }

    override fun isPause(): Boolean {
        return status == TASK_STATUS_PAUSE
    }

    override fun resume() {
        status = TASK_STATUS_EXECUTING
        synchronized(this) {
            notifyAll()
        }
    }

    override fun cancel() {
        status = TASK_STATUS_FAIL
        failType = TASK_FAIL_TYPE_CANCEL
    }

    override fun fail(reason: String?, exception: RuntimeException?) {
        status = TASK_FAIL_TYPE_COMMON_FAIL
    }

    override fun success() {
        status = TASK_STATUS_COMPLETE
    }

    override fun changeProgress(appendSize: Long) {
        synchronized(this) {
            progress += appendSize
        }
    }

    @OnLifecycleEvent(value = Lifecycle.Event.ON_DESTROY)
    override fun cancelListener() {
        pkRequest.taskProcessListener = null
    }

    fun getDownloadFile(): File? {
        return downloadResultFile
    }

    companion object {

        const val TASK_STATUS_READY = 0
        const val TASK_STATUS_EXECUTING = 1
        const val TASK_STATUS_PAUSE = 2
        const val TASK_STATUS_RESUME = 3
        const val TASK_STATUS_COMPLETE = 4
        const val TASK_STATUS_FAIL = 5

        const val TASK_FAIL_TYPE_CANCEL = 10
        const val TASK_FAIL_TYPE_TIMEOUT = 11
        const val TASK_FAIL_TYPE_COMMON_FAIL = 12
    }
}