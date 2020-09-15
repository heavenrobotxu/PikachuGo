package com.damiao.pikachu.common

import androidx.lifecycle.LifecycleOwner
import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.util.uuid
import java.io.File

class PKTaskParam(
    private var lifecycle: LifecycleOwner,
    private val client: Pikachu,
    private var url: String = "",
    private var targetDirectorPath: String = "",
    private var taskProcessListener: PKTaskProcessListener? = null
) {

    fun url(url: String): PKTaskParam {
        this.url = url
        return this
    }

    //设置本地目标文件路径
    fun targetPath(path: String): PKTaskParam {
        this.targetDirectorPath = path
        return this
    }

    //设置任务监听器
    fun taskProcessListener(listener: PKTaskProcessListener): PKTaskParam {
        this.taskProcessListener = listener
        return this
    }

    //提交下载任务到Dispatcher中，返回PkDownloadTask信息
    fun download(): PKDownloadTask {
        if (!checkArgs()) {
            throw IllegalArgumentException("build pikachu task argument illegal")
        }
        val downloadRequest =
            PKDownloadTaskRequest(this.url, targetDirectorPath, taskProcessListener)
        val downloadTask = PKRealDownloadTask(downloadRequest, uuid())
        lifecycle.lifecycle.addObserver(downloadTask)
        client.pkDispatcher.enqueue(downloadTask)
        return downloadTask
    }

    //校验构建的任务参数是否合法
    private fun checkArgs(): Boolean {
        if (this.url.isBlank()) {
            PKLog.error("url can not be empty")
            return false
        }
        if (this.targetDirectorPath.isBlank()) {
            PKLog.error("targetDirectorPath can not be empty")
            return false
        }
        val file = File(targetDirectorPath)
        if (!file.exists() || !file.isDirectory) {
            PKLog.error("targetDirectorPath must exists and is a directory")
            return false
        }
        return true
    }
}