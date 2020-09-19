package com.damiao.pikachu.common

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.util.getDownloadFileSizeDescription
import okhttp3.internal.notifyAll
import java.io.File
import java.lang.UnsupportedOperationException

class PKRealDownloadTask(
    override val pkRequest: PKDownloadTaskRequest,
    override val taskId: String,
    @Volatile
    override var downloadFileName: String? = null
) : PKDownloadTask() {

    @Volatile
    override var taskAlreadyDone = false

    @Volatile
    override var downloadResultFile: File? = null

    @Volatile
    override var progress: Long = 0

    @Volatile
    override var contentLength: Long = 0

    @Volatile
    override var status: Int = PKTask.TASK_STATUS_READY

    @Volatile
    override var failType: Int? = null

    override var failMessage: String? = null

    override var versionTagId: String? = null

    override var downloadSpeed: String? = null

    @Volatile
    var lastCalculateSpeedTime: Long = -1L
    @Volatile
    var lastCalculateProgress: Long = -1L

    override fun submit() {
        status = PKTask.TASK_STATUS_SUBMITTED
    }

    override fun start() {
        status = PKTask.TASK_STATUS_EXECUTING
        triggerPersist()
    }

    override fun pause() {
        if (status != PKTask.TASK_STATUS_EXECUTING) {
            PKLog.error("Download Task can only pause in EXECUTING status")
            return
        }
        status = PKTask.TASK_STATUS_PAUSE
    }

    override fun resume() {
        if (status == PKTask.TASK_STATUS_INTERRUPTED) {
            Pikachu.pkDispatcher.enqueue(this)
            return
        }
        if (status != PKTask.TASK_STATUS_PAUSE) {
            PKLog.error("Downloading Task can only resume in PAUSE status")
            return
        }
        status = PKTask.TASK_STATUS_EXECUTING
        synchronized(this) {
            notifyAll()
        }
    }

    override fun cancel() {
        if (status <= PKTask.TASK_STATUS_SUBMITTED) {
            pkRequest.taskProcessListener?.onCancel(taskId)
            Pikachu.pkDispatcher.complete(this)
            Pikachu.pkGlobalTaskProcessListenerList.forEach {
                it.onCancel(taskId)
            }
            status = PKTask.TASK_STATUS_CANCEL
            triggerPersist()
            return
        }
        if (isPause()) {
            synchronized(this) {
                status = PKTask.TASK_STATUS_CANCEL
                triggerPersist()
                notifyAll()
            }
        }
    }

    override fun fail(reason: String?, exception: RuntimeException?) {
        status = PKTask.TASK_STATUS_FAIL
        failType = PKTask.TASK_FAIL_TYPE_COMMON_FAIL
        failMessage = reason ?: exception?.message ?: "unknown fail reason"
        triggerPersist()
    }

    override fun success() {
        status = PKTask.TASK_STATUS_COMPLETE
        triggerPersist()
    }

    override fun changeProgress(appendSize: Long) {
        synchronized(this) {
            progress += appendSize
        }
        //进度更新时不更新数据库进度信息，否则会造成读写数据库太频繁而拖慢下载速度
    }

    @OnLifecycleEvent(value = Lifecycle.Event.ON_DESTROY)
    override fun cancelListener() {
        pkRequest.taskProcessListener = null
    }
}