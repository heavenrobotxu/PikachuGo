package com.damiao.pikachu.core

import android.os.Handler
import android.os.Looper
import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask
import com.damiao.pikachu.common.PKLog
import com.damiao.pikachu.util.getDownloadFileSizeDescription
import com.damiao.pikachu.util.sha1
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.closeQuietly
import okhttp3.internal.wait
import okio.appendingSink
import okio.buffer
import okio.sink
import java.io.File
import java.lang.RuntimeException

class PKOkHttpDownloadEngine(
    private val client: Pikachu,
    private val okHttpClient: OkHttpClient = OkHttpClient()
) : PKDownloadEngine {

    companion object {
        //默认的读取字节数组大小，一次读取4KB的内容
        const val DEFAULT_CHUNK_SIZE = 4096L
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun download(downloadTask: PKDownloadTask) {
        //校验download的合法性
        val downloadRequest = downloadTask.pkRequest
        val url = downloadRequest.targetUrl.toHttpUrlOrNull()
        if (url == null) {
            downloadTask.fail(reason = "download url is invalid for HTTP!")
            client.pkDispatcher.complete(downloadTask)
            return
        }
        downloadTask.start()
        //是否为断点续传标志位
        val appendWrite = downloadTask.progress != 0L
        val okHttpRequest = if (appendWrite) {
            val range = "bytes=${downloadTask.progress}-"
            PKLog.debug("断点续传Range范围 : $range")
            Request.Builder().url(url).header("Range", range)
                .get().build()
        } else {
            Request.Builder().url(url)
                .get().build()
        }
        try {
            val response = okHttpClient.newCall(okHttpRequest).execute()
            if (response.code != 200 && response.code != 206) {
                throw RuntimeException("Download request return code ${response.code}")
            }
            val fileName = response.request.url.pathSegments.last()
            //获取下载目标文件的文件名
            downloadTask.downloadFileName = fileName
            //获取目标文件在服务器的唯一版本标识
            downloadTask.versionTagId = response.header("ETag")

            // TODO: 2020/9/13 校验eTag是否发生改变，改变则重新下载文件 
            response.body?.use {
                if (!appendWrite) {
                    downloadTask.contentLength = it.contentLength()
                }
                PKLog.debug("当前任务下载文件总大小为${getDownloadFileSizeDescription(it.contentLength())}")
                val targetFile = File(downloadTask.pkRequest.localSinkFilePath, fileName)
                //根据断点续传标志位设置是否需要在文件末尾写入
                val fileSink = if (appendWrite) targetFile.appendingSink() else targetFile.sink()
                val targetBufferedSink = fileSink.buffer()
                mainHandler.post {
                    downloadTask.pkRequest.taskProcessListener?.onStart(downloadTask)
                }
                downloadTask.triggerPersist()
                var remainingSize = it.contentLength()
                val bodySource = it.source()
                while (true) {
                    if (remainingSize == 0L) {
                        break
                    }
                    if (downloadTask.isCancel()) {
                        fileSink.closeQuietly()
                        targetBufferedSink.closeQuietly()
                        bodySource.closeQuietly()
                        client.pkDispatcher.complete(downloadTask)
                        targetFile.delete()
                        throw PKTaskCanceledException()
                    }
                    synchronized(downloadTask) {
                        while (downloadTask.isPause()) {
                            downloadTask.wait()
                            fileSink.closeQuietly()
                            targetBufferedSink.closeQuietly()
                            bodySource.closeQuietly()
                            //重新建立连接，继续从当前进度继续下载
                            throw PKTaskInterruptedException()
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
                PKLog.debug("下载完成的目标文件大小为${targetFile.length()}")
                downloadTask.success()
                mainHandler.post {
                    downloadTask.pkRequest.taskProcessListener?.onComplete(downloadTask)
                }
                client.pkDispatcher.complete(downloadTask)

            }
        } catch (interruptedException: PKTaskInterruptedException) {
            PKLog.debug("任务被中断，需要重新建立连接继续向服务器下载文件")
            download(downloadTask)
        } catch (canceledException: PKTaskCanceledException) {
            PKLog.debug("任务被取消，停止该任务的下载")
            client.pkDispatcher.complete(downloadTask)
        } catch (e: Exception) {
            PKLog.error("download fail ${e.message}")
            downloadTask.fail(reason = "download execute fail by exception ${e.message}")
            client.pkDispatcher.complete(downloadTask)
            mainHandler.post {
                downloadTask.pkRequest.taskProcessListener?.onFail(
                    "download execute fail by exception ${e.message}",
                    e
                )
            }
        }
    }
}