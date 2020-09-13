package com.damiao.pikachu

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import com.damiao.pikachu.common.PKConfig
import com.damiao.pikachu.common.PKLog
import com.damiao.pikachu.common.PKRealDownloadTask
import com.damiao.pikachu.common.PKTaskParam
import com.damiao.pikachu.core.PKDispatcher
import com.damiao.pikachu.core.PKDownloadEngine
import com.damiao.pikachu.core.PkDownloadTaskPersister

object Pikachu {

    lateinit var app: Application
    private var isInit: Boolean = false

    internal lateinit var pkConfig: PKConfig

    val pkDispatcher: PKDispatcher by lazy {
        pkConfig.pkDispatcherFactory.createPKDispatcher(this)
    }
    val pkDownloadEngine: PKDownloadEngine by lazy {
        pkConfig.pkDownloadEngineFactory.createDownloadEngine(this)
    }
    val pkDownloadTaskPersister: PkDownloadTaskPersister by lazy {
        pkConfig.pkDownloadTaskPersisterFactory.createDownloadTaskPersister(this)
    }

    fun with(lifecycle: LifecycleOwner): PKTaskParam {
        if (!isInit) {
            PKLog.error("Pikachu must be init before use")
            throw UnsupportedOperationException("Pikachu must be init")
        }
        return PKTaskParam(
            lifecycle = lifecycle, client = this,
            targetDirectorPath = pkConfig.targetFilePath
        )
    }

    fun init(context: Application) {
        isInit = true
        app = context
        pkConfig = PKConfig.defaultConfig(context)
    }

    fun initWithConfig(context:Application, pkConfig: PKConfig) {
        isInit = true
        app = context
        this.pkConfig = pkConfig
    }

}