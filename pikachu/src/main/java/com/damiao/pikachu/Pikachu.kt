package com.damiao.pikachu

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.damiao.pikachu.common.*
import com.damiao.pikachu.common.PKLog
import com.damiao.pikachu.core.PKDispatcher
import com.damiao.pikachu.core.PKRealDownloadEngineRegister
import com.damiao.pikachu.core.engine.PKDownloadEngine
import com.damiao.pikachu.core.PkDownloadTaskPersister

object Pikachu {

    lateinit var app: Application
    private var isInit: Boolean = false

    internal lateinit var pkConfig: PKConfig

    val pkDispatcher: PKDispatcher by lazy {
        pkConfig.pkDispatcherFactory.createPKDispatcher(this)
    }

    val pkDownloadTaskPersister: PkDownloadTaskPersister by lazy {
        pkConfig.pkDownloadTaskPersisterFactory.createDownloadTaskPersister(this)
    }

    val pkTaskGetter: PKTaskGetter = PKRealTaskListGetter(this)

    val pkDownloadEngRegister : PKRealDownloadEngineRegister = PKRealDownloadEngineRegister()

    val pkGlobalTaskProcessListenerList: MutableList<PKTaskProcessListener> = mutableListOf()

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
        pkConfig.pkDownloadEngineFactory.createDownloadEngine(this).forEach {
            pkDownloadEngRegister.registerDownloadEngine(it)
        }
    }

    fun initWithConfig(context: Application, pkConfig: PKConfig) {
        isInit = true
        app = context
        this.pkConfig = pkConfig
    }

    fun addGlobalTaskProcessListener(listener: PKTaskProcessListener, lifecycle: LifecycleOwner) {
        pkGlobalTaskProcessListenerList.add(listener)
        lifecycle.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                PKLog.debug("listener component finished , remove the listener")
                removeGlobalTaskProcessListener(listener)
            }
        })
    }

    fun removeGlobalTaskProcessListener(listener: PKTaskProcessListener) {
        pkGlobalTaskProcessListenerList.remove(listener)
    }

    fun deleteLocalTask(downloadTask: PKDownloadTask, deleteFile: Boolean = false) {
        if (deleteFile) {
            downloadTask.downloadResultFile?.delete()
        }
        pkDownloadTaskPersister.deleteDownloadTask(downloadTask)
    }
}