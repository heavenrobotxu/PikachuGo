package com.damiao.pikachu.core

import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask
import com.damiao.pikachu.common.PKDownloadTaskRequest

interface PKDispatcher {

    fun enqueue(pkDownloadTask: PKDownloadTask)

    fun complete(pkDownloadTask: PKDownloadTask)

    fun gerRunningTaskList() : List<PKDownloadTask>

    interface Factory {

        fun createPKDispatcher(pikachu: Pikachu) : PKDispatcher
    }
}