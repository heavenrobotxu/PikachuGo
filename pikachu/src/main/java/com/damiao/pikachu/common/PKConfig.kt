package com.damiao.pikachu.common

import android.app.Application
import android.os.Environment
import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.core.*
import com.damiao.pikachu.core.PKRealDownloadDispatcher
import okhttp3.OkHttpClient

class PKConfig(
    //最大并发下载任务数量，默认为20
    val maxConcurrentTaskSize: Int = 20,
    //任务的默认下载目录
    val targetFilePath: String,
    //线程分发器工厂
    val pkDispatcherFactory: PKDispatcher.Factory = DefaultPkDispatcherFactory(),
    //下载引擎工厂
    val pkDownloadEngineFactory: PKDownloadEngine.Factory = DefaultPkDownloadEngineFactory(),
    //下载任务持久化工厂
    val pkDownloadTaskPersisterFactory: PkDownloadTaskPersister.Factory = DefaultPKDownloadTaskPersisterFactory()
) {
    companion object {

        fun defaultConfig(context: Application): PKConfig {

            return PKConfig(
                targetFilePath = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.path
            )
        }

        class DefaultPkDispatcherFactory : PKDispatcher.Factory {

            override fun createPKDispatcher(pikachu: Pikachu): PKDispatcher {
                return PKRealDownloadDispatcher(pikachu)
            }
        }

        class DefaultPkDownloadEngineFactory : PKDownloadEngine.Factory {

            override fun createDownloadEngine(
                pikachu: Pikachu, okHttpClient: OkHttpClient?): PKDownloadEngine {
                return PKOkHttpDownloadEngine(pikachu)
            }
        }

        class DefaultPKDownloadTaskPersisterFactory: PkDownloadTaskPersister.Factory {

            override fun createDownloadTaskPersister(pikachu: Pikachu): PkDownloadTaskPersister {
                return PKSQLiteDownloadTaskPersister(pikachu)
            }
        }
    }
}