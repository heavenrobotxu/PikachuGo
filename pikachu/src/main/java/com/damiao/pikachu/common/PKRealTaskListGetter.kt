package com.damiao.pikachu.common

import com.damiao.pikachu.Pikachu

class PKRealTaskListGetter(private val client: Pikachu) : PKTaskGetter{

    override fun getAllTaskList(): List<PKDownloadTask> {
        return fixTaskList(client.pkDownloadTaskPersister.getAllDownloadTaskList())
    }

    override fun getUnCompleteTaskList(): List<PKDownloadTask> {
        return fixTaskList(client.pkDownloadTaskPersister.getUnCompleteTaskList())
    }

    override fun getCompleteTaskList(): List<PKDownloadTask> {
        return client.pkDownloadTaskPersister.getCompleteTaskList()
    }

    override fun getFailTaskList(): List<PKDownloadTask> {
        return client.pkDownloadTaskPersister.getFailTaskList()
    }

    override fun getCancelTaskList(): List<PKDownloadTask> {
        return client.pkDownloadTaskPersister.getCancelTaskList()
    }

    //比较本地持久器返回的以及在Dispatcher中的Task,相同taskId优先使用Dispatcher中的task
    private fun fixTaskList(localTaskList: List<PKDownloadTask>) : List<PKDownloadTask>{
        val result = mutableListOf<PKDownloadTask>()
        synchronized(client.pkDispatcher) {
            val readyMap = client.pkDispatcher.gerReadyTaskList().associateBy { it.taskId }
            val runningMap= client.pkDispatcher.gerRunningTaskList().associateBy { it.taskId }
            for (pkDownloadTask in localTaskList) {
                val readyTask = readyMap[pkDownloadTask.taskId]
                if (readyTask != null) {
                    result.add(readyTask)
                } else {
                    val runningTask = runningMap[pkDownloadTask.taskId]
                    if (runningTask != null) {
                        result.add(runningTask)
                    } else {
                        result.add(pkDownloadTask)
                    }
                }
            }
        }
        return result.toList()
    }

}