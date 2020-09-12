package com.damiao.pikachu.common

class PKDownloadTaskRequest(
    val targetUrl: String,
    val localSinkFilePath: String,
    var taskProcessListener: PKTaskProcessListener? = null
) {
}