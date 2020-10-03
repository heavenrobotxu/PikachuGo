package com.damiao.pikachu.core

import com.damiao.pikachu.core.engine.PKDownloadEngine

//下载引擎注册类，负责持有所有类型的下载引擎
interface PKDownloadEngineRegister {

    //注册下载引擎
    fun registerDownloadEngine(downloadEngine: PKDownloadEngine)
    //根据传入url寻找与其适配的下载引擎
    fun findDownloadEngine(url: String) : PKDownloadEngine?
}