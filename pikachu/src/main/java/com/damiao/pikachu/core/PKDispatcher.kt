package com.damiao.pikachu.core

import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask
import com.damiao.pikachu.common.PKDownloadTaskRequest

interface PKDispatcher {

    fun enqueue(pkRequest: PKDownloadTaskRequest) : PKDownloadTask

    fun complete(pkDownloadTask: PKDownloadTask)

    interface Factory {

        fun createPKDispatcher(pikachu: Pikachu) : PKDispatcher
    }
}