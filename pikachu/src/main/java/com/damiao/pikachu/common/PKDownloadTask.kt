package com.damiao.pikachu.common

import android.database.Observable
import com.damiao.pikachu.core.PkDownloadTaskPersister
import java.io.File
import java.lang.RuntimeException

abstract class PKDownloadTask : PKTask, Observable<PkDownloadTaskPersister>() {

    abstract val pkRequest: PKDownloadTaskRequest

    abstract var downloadResultFile: File?

    abstract var downloadFileName: String?

    abstract var versionTagId: String?

    //下载速度，单位为kb/s或b/s或mb/s
    abstract var downloadSpeed: String?

    internal var needDbProgress: Boolean = false

    //更新任务的下载进度
    internal abstract fun changeProgress(appendSize: Long)

    //将任务提交到准备列表
    internal abstract fun submit()

    //开始执行Task
    internal abstract fun start()

    //内部调用，触发Task执行成功
    internal abstract fun success()

    //内部调用，触发Task失败状态
    internal abstract fun fail(reason: String? = null, exception: RuntimeException? = null)

    override fun isCancel(): Boolean {
        return status == PKTask.TASK_STATUS_CANCEL
    }

    override fun isPause(): Boolean {
        return status == PKTask.TASK_STATUS_PAUSE
    }

    //触发下载任务的持久化
    internal fun triggerPersist(isUpdate: Boolean = true) {
        for (mObserver in mObservers) {
            if (isUpdate) mObserver.updateDownloadTask(this)
            else mObserver.saveDownloadTask(this)
        }
    }

    //下载任务Task状态改变类型监听
    internal var taskStatusChangedListener: MutableList<(Int) -> Unit> = mutableListOf()
}