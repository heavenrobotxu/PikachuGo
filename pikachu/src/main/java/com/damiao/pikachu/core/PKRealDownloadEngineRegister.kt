package com.damiao.pikachu.core

import com.damiao.pikachu.core.engine.PKDownloadEngine

class PKRealDownloadEngineRegister : PKDownloadEngineRegister {

    private val engineMap = mutableMapOf<String, PKDownloadEngine>()

    override fun registerDownloadEngine(downloadEngine: PKDownloadEngine) {
        downloadEngine.supportProto().forEach {
            if (!engineMap.containsKey(it)) {
                engineMap[it] = downloadEngine
            }
        }
    }

    override fun findDownloadEngine(url: String): PKDownloadEngine? {
        for ((proto, engine) in engineMap) {
            if (url.startsWith(proto, ignoreCase = true)) {
                return engine
            }
        }
        return null
    }
}