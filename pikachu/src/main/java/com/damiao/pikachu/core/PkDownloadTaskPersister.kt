package com.damiao.pikachu.core

import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask

interface PkDownloadTaskPersister : PKTaskPersister {

    fun saveDownloadTask(downloadTask: PKDownloadTask)

    fun updateDownloadTask(downloadTask: PKDownloadTask)

    fun updateDownloadTaskProgress(downloadTask: PKDownloadTask)

    fun deleteDownloadTask(downloadTask: PKDownloadTask)

    fun getAllDownloadTaskList(): List<PKDownloadTask>

    fun getUnCompleteTaskList(): List<PKDownloadTask>

    fun getCompleteTaskList(): List<PKDownloadTask>

    fun getFailTaskList(): List<PKDownloadTask>

    fun getCancelTaskList(): List<PKDownloadTask>

    fun getDownloadTaskByTaskId(taskId: String): PKDownloadTask?

    interface Factory {

        fun createDownloadTaskPersister(pikachu: Pikachu): PkDownloadTaskPersister
    }
}