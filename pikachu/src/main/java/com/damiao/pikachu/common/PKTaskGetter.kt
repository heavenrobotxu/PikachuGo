package com.damiao.pikachu.common

interface PKTaskGetter {

    //获取所有下载任务列表，包含准备中、未下载完成、已下载、已失败的任务
    fun getAllTaskList(): List<PKDownloadTask>

    //获取所有下载任务列表，包含准备中、未下载完成、已下载、已失败的任务
    fun getUnCompleteTaskList() : List<PKDownloadTask>

    //获取已下载的任务列表
    fun getCompleteTaskList(): List<PKDownloadTask>

    //获取已失败的任务列表
    fun getFailTaskList(): List<PKDownloadTask>

    //获取已取消的任务列表
    fun getCancelTaskList(): List<PKDownloadTask>
}