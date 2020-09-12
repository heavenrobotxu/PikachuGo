package com.damiao.pikachu.common

import java.io.File

interface PKDownloadTask : PKTask {

    val pkRequest: PKDownloadTaskRequest

    var taskAlreadyDone: Boolean

    var downloadResultFile: File?

    var downloadFileName: String?

    fun changeProgress(appendSize: Long)
}