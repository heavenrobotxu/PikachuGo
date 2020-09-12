package com.damiao.pikachu.core

import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask

//下载引擎接口
interface PKDownloadEngine {

    //download函数，执行真正的下载任务
    fun download(downloadTask: PKDownloadTask)

    interface Factory {

        fun createDownloadEngine(pikachu: Pikachu) : PKDownloadEngine
    }
}