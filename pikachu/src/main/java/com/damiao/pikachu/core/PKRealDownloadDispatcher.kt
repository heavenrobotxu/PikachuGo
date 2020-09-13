package com.damiao.pikachu.core

import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask
import com.damiao.pikachu.common.PKDownloadTaskRequest
import com.damiao.pikachu.common.PKRealDownloadTask
import com.damiao.pikachu.util.uuid
import java.util.*
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal class PKRealDownloadDispatcher(private val client: Pikachu) : PKDispatcher {
    //等待执行下载的Task列表
    private val readyTaskList = ArrayDeque<PKDownloadTask>()

    //正在执行下载的Task列表
    private val runningTaskList = ArrayDeque<PKDownloadTask>()

    //下载执行完成的Task列表（包含下载成功\失败\被取消的Task）
    private val completeTaskList = ArrayDeque<PKDownloadTask>()

    //最大可同时并发下载的任务个数
    private val maxRunningSize = client.pkConfig.maxConcurrentTaskSize

    //下载引擎,惰性加载，promote调用时才从pikachu中获取下载引擎
    private val downloadEngine: PKDownloadEngine by lazy {
        client.pkDownloadEngine
    }

    //下载线程池
    private val executorService = ThreadPoolExecutor(
        5, maxRunningSize, 60,
        TimeUnit.SECONDS, LinkedBlockingDeque(), PkThreadFactory()
    )

    //将下载任务添加到准备队列中
    override fun enqueue(pkRequest: PKDownloadTaskRequest): PKDownloadTask {
        val downloadTask = PKRealDownloadTask(pkRequest, uuid())
        //提交到准备队列之前先注册数据库持久器监听其状态变化
        downloadTask.registerObserver(client.pkDownloadTaskPersister)
        downloadTask.triggerPersist(isUpdate = false)
        synchronized(this) {
            readyTaskList.add(downloadTask)
        }
        promoteAndExecuteDownloadTask()
        return downloadTask
    }

    //执行准备队列中的下载任务取出并提交给线程池执行
    private fun promoteAndExecuteDownloadTask() {
        synchronized(this) {
            while (runningTaskList.size < maxRunningSize && !readyTaskList.isEmpty()) {
                readyTaskList.pollFirst()?.let {
                    executorService.submit {
                        downloadEngine.download(it)
                    }
                    runningTaskList.add(it)
                }
            }
        }
    }

    //将执行完成的下载任务从正在执行列表中删除
    override fun complete(pkDownloadTask: PKDownloadTask) {
        synchronized(this) {
            runningTaskList.remove(pkDownloadTask)
            if (!completeTaskList.contains(pkDownloadTask)) {
                completeTaskList.add(pkDownloadTask)
            }
        }
        //继续触发执行准备队列中的下载任务
        promoteAndExecuteDownloadTask()
    }

}