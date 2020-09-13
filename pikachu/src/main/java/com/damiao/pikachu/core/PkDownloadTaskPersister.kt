package com.damiao.pikachu.core

import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask

interface PkDownloadTaskPersister : PKTaskPersister {

    fun saveDownloadTask(downloadTask: PKDownloadTask)

    fun updateDownloadTask(downloadTask: PKDownloadTask)

    fun getDownloadTaskList(): List<PKDownloadTask>

    fun getDownloadingTaskList(): List<PKDownloadTask>

    fun getDownloadTask(taskId: String): PKDownloadTask?

    interface Factory {

        fun createDownloadTaskPersister(pikachu: Pikachu): PkDownloadTaskPersister
    }
}