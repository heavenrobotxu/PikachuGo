package com.damiao.pikachu.common

import android.app.Application
import android.os.Environment
import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.core.*
import com.damiao.pikachu.core.PKRealDownloadDispatcher
import com.damiao.pikachu.core.engine.PKDownloadEngine
import com.damiao.pikachu.core.engine.PKHttpDownloadEngine
import com.damiao.pikachu.core.engine.PKMagnetDownloadEngine
import okhttp3.OkHttpClient

class PKConfig(
    //最大并发下载任务数量，默认为20
    val maxConcurrentTaskSize: Int = 20,
    //任务的默认下载目录
    val targetFilePath: String,
    //线程分发器工厂
    val pkDispatcherFactory: PKDispatcher.Factory = DefaultPkDispatcherFactory(),
    //若使用okHttp作为下载引擎，外部可传入定制后的okHttpClient，应对一些定制化处理例如处理HTTPS证书问题等
    private val okHttpClient: OkHttpClient? = null,
    //下载引擎工厂
    val pkDownloadEngineFactory: PKDownloadEngine.Factory = DefaultPkDownloadEngineFactory(okHttpClient),
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

        class DefaultPkDownloadEngineFactory(private val okHttpClient: OkHttpClient? = null) :
            PKDownloadEngine.Factory {

            override fun createDownloadEngine(pikachu: Pikachu): List<PKDownloadEngine> {
                val httpDownloadEngine = if (okHttpClient != null) {
                    PKHttpDownloadEngine(
                        pikachu,
                        okHttpClient
                    )
                } else {
                    PKHttpDownloadEngine(pikachu)
                }
                val magnetDownloadEngine = PKMagnetDownloadEngine(pikachu)
                return listOf(httpDownloadEngine, magnetDownloadEngine)
            }
        }

        class DefaultPKDownloadTaskPersisterFactory : PkDownloadTaskPersister.Factory {

            override fun createDownloadTaskPersister(pikachu: Pikachu): PkDownloadTaskPersister {
                return PKSQLiteDownloadTaskPersister(pikachu)
            }
        }
    }
}