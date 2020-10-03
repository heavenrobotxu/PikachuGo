package com.damiao.pikachu.core.engine

import android.os.Handler
import android.os.Looper
import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask
import com.damiao.pikachu.common.PKLog
import com.damiao.pikachu.common.PKTask
import org.libtorrent4j.AddTorrentParams
import org.libtorrent4j.AlertListener
import org.libtorrent4j.SessionManager
import org.libtorrent4j.TorrentFlags
import org.libtorrent4j.alerts.*
import java.io.File
import java.util.*

//BT链接下载引擎（LibTorrent）
class PKMagnetDownloadEngine(private val client: Pikachu) : PKDownloadEngine {

    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        const val MAGNET_PROTO = "magnet:?xt=urn:btih:"
    }

    override fun supportProto(): List<String> = listOf(MAGNET_PROTO)

    override fun needDbProgress() = true

    override fun download(downloadTask: PKDownloadTask) {
        if (!checkProto(downloadTask)) {
            return
        }
        val localFile = File(downloadTask.pkRequest.localSinkFilePath)
        val sessionManager = SessionManager()
        downloadTask.taskStatusChangedListener.add { status ->
            when (status) {
                //任务被暂停
                PKTask.TASK_CHANGE_TYPE_EXECUTING_TO_PAUSE -> {
                    sessionManager.pause()
                    PKLog.debug("磁力链接下载 ${downloadTask.downloadFileName} 被暂停")
                }
                //任务被恢复
                PKTask.TASK_CHANGE_TYPE_PAUSE_TO_EXECUTING -> {
                    sessionManager.resume()
                    PKLog.debug("磁力链接下载 ${downloadTask.downloadFileName} 被恢复")
                }
                //任务被取消
                PKTask.TASK_CHANGE_TYPE_CANCELED -> {
                    localFile.delete()
                    sessionManager.stop()
                    mainHandler.post {
                        PKLog.debug(
                            "磁力链接下载 ${downloadTask.downloadFileName} 被取消，" +
                                    "停止该任务的下载"
                        )
                        downloadTask.pkRequest.taskProcessListener?.onCancel(downloadTask)
                        client.pkGlobalTaskProcessListenerList.forEach {
                            it.onCancel(downloadTask)
                        }
                    }
                    client.pkDispatcher.complete(downloadTask)
                }
                else -> {
                }
            }
        }
        sessionManager.addListener(object : AlertListener {
            override fun alert(alert: Alert<*>) {
                when (alert.type()) {
                    AlertType.ADD_TORRENT -> {
                        val ata = alert as AddTorrentAlert
                        PKLog.debug("磁力种子添加成功 ${ata.torrentName()}, 开始下载目标文件")
                        downloadTask.start()
                        mainHandler.post {
                            downloadTask.pkRequest.taskProcessListener?.onStart(downloadTask)
                            client.pkGlobalTaskProcessListenerList.forEach {
                                it.onStart(downloadTask)
                            }
                        }
                    }
                    AlertType.BLOCK_FINISHED -> {
                        val bfa = alert as BlockFinishedAlert
                        val status = bfa.handle().status()
                        //目标文件总大小，单位字节
                        val total = status.total()
                        //已下载大小
                        val progress = status.totalDone()
                        PKLog.debug(
                            "磁力分块下载完成 ${bfa.torrentName()} " +
                                    "已下载 $progress 总大小 $total 下载百分比 ${(status.progress() * 100).toInt()}"
                        )
                        var firstChange = false
                        if (total != 0L && downloadTask.contentLength == 0L) {
                            downloadTask.contentLength = status.total()
                            downloadTask.downloadFileName = bfa.torrentName()
                            downloadTask.triggerPersist()
                            firstChange = true
                        }
                        if (downloadTask.progress == progress && !firstChange) {
                            return
                        }
                        downloadTask.progress = progress
                        //磁力链接由于分片下载的原因，需要实时更新数据库中的progress，无法使用本地文件的大小作为progress
                        client.pkDownloadTaskPersister.updateDownloadTask(downloadTask)
                        mainHandler.post {
                            downloadTask.pkRequest.taskProcessListener?.onProcess(
                                progress,
                                total,
                                downloadTask
                            )
                            client.pkGlobalTaskProcessListenerList.forEach {
                                it.onProcess(progress, total, downloadTask)
                            }
                        }
                    }
                    AlertType.FILE_COMPLETED -> {
                        val fca = alert as FileCompletedAlert
                        PKLog.debug("磁力链接目标文件 ${fca.torrentName()} 已经下载完成")
                        downloadTask.success()
                        downloadTask.downloadResultFile =
                            File(downloadTask.pkRequest.localSinkFilePath, fca.torrentName())
                        mainHandler.post {
                            downloadTask.pkRequest.taskProcessListener?.onComplete(downloadTask)
                            client.pkGlobalTaskProcessListenerList.forEach {
                                it.onComplete(downloadTask)
                            }
                        }
                        client.pkDispatcher.complete(downloadTask)
                    }
                    else -> {
                    }
                }
            }

            override fun types(): IntArray? {
                return null
            }

        })
        sessionManager.start()
        sessionManager.download(
            downloadTask.pkRequest.targetUrl,
            localFile,
            TorrentFlags.NEED_SAVE_RESUME
        )
    }

    //校验磁力链接url是否合法
    private fun checkProto(downloadTask: PKDownloadTask): Boolean {
        val btUrl = downloadTask.pkRequest.targetUrl
        if (!btUrl.startsWith(MAGNET_PROTO, ignoreCase = true)) {
            //磁力链接url格式校验失败，直接将下载任务置为失败
            downloadTask.fail(reason = "download url is invalid for BT-MAGNET!")
            PKLog.debug("磁力链接url:${btUrl}校验失败")
            client.pkDispatcher.complete(downloadTask)
            return false
        }
        try {
            AddTorrentParams.parseMagnetUri(btUrl)
        } catch (exception: Exception) {
            PKLog.debug("磁力链接url:${btUrl}校验失败")
            downloadTask.fail(reason = "download url is invalid for BT-MAGNET!")
            client.pkDispatcher.complete(downloadTask)
            return false
        }
        return true
    }
}