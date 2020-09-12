package com.damiao.pikachu.core

import android.os.Handler
import android.os.Looper
import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask
import com.damiao.pikachu.common.PKLog
import com.damiao.pikachu.util.getDownloadFileSizeDescription
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.closeQuietly
import okhttp3.internal.wait
import okio.buffer
import okio.sink
import java.io.EOFException
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.max

class PKOkHttpDownloadEngine(private val pikachu: Pikachu) : PKDownloadEngine {

    companion object {
        //默认的读取字节数组大小，一次读取4KB的内容
        const val DEFAULT_CHUNK_SIZE = 4096L
    }

    private val okHttpClient = OkHttpClient.Builder().build()

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun download(downloadTask: PKDownloadTask) {
        //校验download的合法性
        val downloadRequest = downloadTask.pkRequest
        val url = downloadRequest.targetUrl.toHttpUrlOrNull()
        if (url == null) {
            downloadTask.fail(reason = "download url is invalid for HTTP!")
            return
        }
        val okHttpRequest = Request.Builder().url(url)
            .get().build()
        try {
            val response = okHttpClient.newCall(okHttpRequest).execute()
            val fileName = response.request.url.pathSegments.last()
            downloadTask.downloadFileName = fileName
            response.body?.use {
                downloadTask.contentLength = it.contentLength()
                PKLog.debug("当前任务下载文件总大小为${getDownloadFileSizeDescription(it.contentLength())}")
                val targetFile = File(downloadTask.pkRequest.localSinkFilePath, fileName)
                val fileSink = targetFile.sink()
                val targetBufferedSink = fileSink.buffer()
                mainHandler.post {
                    downloadTask.pkRequest.taskProcessListener?.onStart(downloadTask)
                }
                var remainingSize = it.contentLength()
                val bodySource = it.source()
                while (true) {
                    if (remainingSize == 0L) {
                        break
                    }
                    synchronized(downloadTask) {
                        while (downloadTask.isPause()) {
                            downloadTask.wait()
                        }
                    }
                    var array: ByteArray
                    val readByteCount: Long = if (remainingSize < DEFAULT_CHUNK_SIZE) {
                        remainingSize
                    } else {
                        DEFAULT_CHUNK_SIZE
                    }
                    array = bodySource.readByteArray(readByteCount)
                    remainingSize -= readByteCount

                    if (array.isNotEmpty()) {
                        targetBufferedSink.write(array)
                        targetBufferedSink.flush()

                        downloadTask.changeProgress(array.size.toLong())
                        mainHandler.post {
                            downloadTask.pkRequest.taskProcessListener?.onProcess(
                                downloadTask.progress, it.contentLength()
                            )
                        }
                    } else {
                        break
                    }
                }
                bodySource.closeQuietly()
                targetBufferedSink.closeQuietly()
                fileSink.closeQuietly()
                downloadTask.downloadResultFile = targetFile
                downloadTask.success()
                mainHandler.post {
                    downloadTask.pkRequest.taskProcessListener?.onComplete(downloadTask)
                }
                pikachu.pkDispatcher.complete(downloadTask)
            }
        } catch (e: Exception) {
            PKLog.error("download fail ${e.message}")
            downloadTask.fail(reason = "download execute fail")
            pikachu.pkDispatcher.complete(downloadTask)
            mainHandler.post {
                downloadTask.pkRequest.taskProcessListener?.onFail("download execute fail")
            }
        }
    }
}