package com.damiao.pikachu.common

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.util.getDownloadFileSizeDescription
import okhttp3.internal.notifyAll
import java.io.File
import java.lang.UnsupportedOperationException

internal class PKRealDownloadTask(
    override val pkRequest: PKDownloadTaskRequest,
    override val taskId: String,
    @Volatile
    override var downloadFileName: String? = null
) : PKDownloadTask() {

    @Volatile
    override var downloadResultFile: File? = null

    @Volatile
    override var progress: Long = 0

    @Volatile
    override var contentLength: Long = 0

    @Volatile
    override var status: Int = PKTask.TASK_STATUS_NEW

    @Volatile
    override var failType: Int? = null

    override var failMessage: String? = null

    override var versionTagId: String? = null

    override var downloadSpeed: String? = null

    @Volatile
    internal var lastCalculateSpeedTime: Long = -1L
    @Volatile
    internal var lastCalculateProgress: Long = -1L

    override fun submit() {
        val oldStatus = status
        status = PKTask.TASK_STATUS_READY
        triggerPersist(oldStatus == PKTask.TASK_STATUS_INTERRUPTED)
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
        taskStatusChangedListener.forEach {
            it.invoke(PKTask.TASK_CHANGE_TYPE_EXECUTING_TO_PAUSE)
        }
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
        taskStatusChangedListener.forEach {
            it.invoke(PKTask.TASK_CHANGE_TYPE_PAUSE_TO_EXECUTING)
        }
    }

    override fun cancel() {
        if (status <= PKTask.TASK_STATUS_READY) {
            pkRequest.taskProcessListener?.onCancel(this)
            Pikachu.pkDispatcher.complete(this)
            Pikachu.pkGlobalTaskProcessListenerList.forEach {
                it.onCancel(this)
            }
            status = PKTask.TASK_STATUS_CANCEL
            triggerPersist()
            return
        }
        status = PKTask.TASK_STATUS_CANCEL
        triggerPersist()
        if (isPause()) {
            synchronized(this) {
                notifyAll()
            }
        }
        taskStatusChangedListener.forEach {
            it.invoke(PKTask.TASK_CHANGE_TYPE_CANCELED)
        }
    }

    override fun fail(reason: String?, exception: RuntimeException?) {
        status = PKTask.TASK_STATUS_FAIL
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

    //当监听任务进度的组件（如Activity）被销毁时，及时移除监听，防止下载线程长时间持有对已销毁的组件的引用导致无法回收
    @OnLifecycleEvent(value = Lifecycle.Event.ON_DESTROY)
    override fun cancelListener() {
        PKLog.debug("listener component finished , set the listener null")
        pkRequest.taskProcessListener = null
    }
}