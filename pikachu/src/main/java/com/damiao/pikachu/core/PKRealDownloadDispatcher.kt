package com.damiao.pikachu.core

import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask
import com.damiao.pikachu.common.PKRealDownloadTask
import com.damiao.pikachu.util.getDownloadFileSizeDescription
import java.util.*
import java.util.concurrent.Executors
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
    //task下载速度监听单线程池，定时计算所有下载任务的当前速度
    private val speedWatchExecutor = Executors.newSingleThreadExecutor()

    init {
        startCalculateTaskSpeed()
    }

    //下载线程池
    private val executorService = ThreadPoolExecutor(
        5, maxRunningSize, 60,
        TimeUnit.SECONDS, LinkedBlockingDeque(), PkThreadFactory()
    )

    //将下载任务添加到准备队列中
    override fun enqueue(pkDownloadTask: PKDownloadTask) {
        //提交到准备队列之前先注册数据库持久器监听其状态变化
        pkDownloadTask.registerObserver(client.pkDownloadTaskPersister)
        pkDownloadTask.triggerPersist(isUpdate = false)
        synchronized(this) {
            pkDownloadTask.submit()
            readyTaskList.add(pkDownloadTask)
        }
        promoteAndExecuteDownloadTask()
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

    //执行准备队列中的下载任务取出并提交给线程池执行
    private fun promoteAndExecuteDownloadTask() {
        synchronized(this) {
            while (runningTaskList.size < maxRunningSize && !readyTaskList.isEmpty()) {
                readyTaskList.pollFirst()?.let {
                    //若下载任务在提交执行前已经被取消，那么直接完成该下载任务，不进行下载
                    if (it.isCancel()) {
                        complete(it)
                        return@let
                    }
                    executorService.submit {
                        downloadEngine.download(it)
                    }
                    runningTaskList.add(it)
                }
            }
        }
    }

    //开启任务下载速度计算线程，定时计算并更新所有正在下载中的task下载速度
    private fun startCalculateTaskSpeed() {
        speedWatchExecutor.submit {
            while (true) {
                Thread.sleep(500)
                synchronized(this) {
                    if (!runningTaskList.isEmpty()) {
                        for (pkDownloadTask in runningTaskList) {
                            val realTask = pkDownloadTask as PKRealDownloadTask
                            val now = System.currentTimeMillis()
                            if (realTask.lastCalculateSpeedTime == -1L) {
                                realTask.lastCalculateSpeedTime = now
                                realTask.lastCalculateProgress = realTask.progress
                                continue
                            } else {
                                val duration = now - realTask.lastCalculateSpeedTime
                                if (duration >= 1000) {
                                    realTask.downloadSpeed = "${getDownloadFileSizeDescription(realTask.progress
                                            - realTask.lastCalculateProgress)}/s"
                                    realTask.lastCalculateSpeedTime = now
                                    realTask.lastCalculateProgress = realTask.progress
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    @Synchronized
    override fun gerRunningTaskList(): List<PKDownloadTask> {
        return runningTaskList.toList()
    }

}