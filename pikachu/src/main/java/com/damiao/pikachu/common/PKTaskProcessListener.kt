package com.damiao.pikachu.common

import java.lang.Exception

//任务进度监听接口
interface PKTaskProcessListener {
    //任务开始执行时回调，有默认空实现
    fun onStart(taskId: String) {}
    //任务进度改变时执行时回调，有默认空实现
    fun onProcess(process: Long, length: Long, taskId: String) {}
    //任务完成时改变时执行时回调，有默认空实现
    fun onComplete(taskId: String) {}
    //任务失败时回调，有默认空实现
    fun onFail(reason: String, exception: Exception?, taskId: String){}
    //任务取消时回调，有默认空实现
    fun onCancel(taskId: String){}
}