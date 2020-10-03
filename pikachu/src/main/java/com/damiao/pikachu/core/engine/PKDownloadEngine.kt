package com.damiao.pikachu.core.engine

import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask

//下载引擎接口
interface PKDownloadEngine {

    //支持的下载协议
    fun supportProto() : List<String>

    //download函数，执行真正的下载任务
    fun download(downloadTask: PKDownloadTask)

    //从本地恢复任务时是否使用本地数据库中的进度数据
    fun needDbProgress(): Boolean

    interface Factory {

        fun createDownloadEngine(pikachu: Pikachu): List<PKDownloadEngine>
    }
}