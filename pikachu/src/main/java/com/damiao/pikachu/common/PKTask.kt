package com.damiao.pikachu.common

import androidx.lifecycle.LifecycleObserver

interface PKTask : LifecycleObserver {
    //Task已经执行上传、下载的当前进度
    var progress: Long

    //Task所要上传、下载文件的总大小，单位是byte
    var contentLength: Long

    //Task唯一ID
    val taskId: String

    //Task当前所处的状态
    var status: Int

    //假如Task执行失败，执行失败的类型
    var failType: Int?

    //假如Task执行失败，执行失败的具体原因
    var failMessage: String?

    //暂停执行Task
    fun pause()

    //Task是否已经暂停
    fun isPause(): Boolean

    //Task是否已经暂停
    fun isCancel(): Boolean

    //恢复执行Task
    fun resume()

    //取消Task，中断正在执行的上传、下载行为
    fun cancel()

    //主动取消Task的状态监听，防止内存泄露
    fun cancelListener()

    companion object {

        /*任务状态值*/
        const val TASK_STATUS_INTERRUPTED = -1
        const val TASK_STATUS_NEW = 0
        const val TASK_STATUS_READY = 1
        const val TASK_STATUS_EXECUTING = 2
        const val TASK_STATUS_PAUSE = 3
        const val TASK_STATUS_CANCEL = 4
        const val TASK_STATUS_COMPLETE = 5
        const val TASK_STATUS_FAIL = 6

        /*任务状态改变类型值*/

        //任务从执行中切换为暂停
        const val TASK_CHANGE_TYPE_EXECUTING_TO_PAUSE = 0

        //任务从暂停中切换为执行中
        const val TASK_CHANGE_TYPE_PAUSE_TO_EXECUTING = 1

        //任务被取消
        const val TASK_CHANGE_TYPE_CANCELED = 2
    }
}