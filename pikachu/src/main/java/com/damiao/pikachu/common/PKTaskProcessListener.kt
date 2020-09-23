package com.damiao.pikachu.common

import java.lang.Exception

//任务进度监听接口
interface PKTaskProcessListener {
    //任务被提交到准备列表后执行时回调，有默认空实现
    fun onReady(downloadTask: PKDownloadTask) {}
    //任务开始执行时回调，有默认空实现
    fun onStart(downloadTask: PKDownloadTask) {}
    //任务进度改变时执行时回调，有默认空实现
    fun onProcess(process: Long, length: Long, downloadTask: PKDownloadTask) {}
    //任务完成时改变时执行时回调，有默认空实现
    fun onComplete(downloadTask: PKDownloadTask) {}
    //任务失败时回调，有默认空实现
    fun onFail(reason: String, exception: Exception?, downloadTask: PKDownloadTask){}
    //任务取消时回调，有默认空实现
    fun onCancel(downloadTask: PKDownloadTask){}
}