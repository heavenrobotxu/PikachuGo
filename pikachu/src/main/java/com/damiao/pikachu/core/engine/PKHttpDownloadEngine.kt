package com.damiao.pikachu.core.engine

import android.os.Handler
import android.os.Looper
import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask
import com.damiao.pikachu.common.PKLog
import com.damiao.pikachu.core.exception.PKTaskCanceledException
import com.damiao.pikachu.core.exception.PKTaskInterruptedException
import com.damiao.pikachu.core.exception.PKTaskNeedReDownloadException
import com.damiao.pikachu.util.closeAll
import com.damiao.pikachu.util.getDownloadFileSizeDescription
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import okhttp3.internal.wait
import okio.*
import java.io.File

//HTTP下载引擎(OkHttp)
internal class PKHttpDownloadEngine(
    private val client: Pikachu,
    private val okHttpClient: OkHttpClient = OkHttpClient()
) : PKDownloadEngine {

    companion object {
        //默认的读取字节数组大小，一次读取4KB的内容
        const val DEFAULT_CHUNK_SIZE = 4096L
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun supportProto(): List<String> = listOf("http", "https")

    override fun needDbProgress() = false

    override fun download(downloadTask: PKDownloadTask) {
        val url = checkProto(downloadTask) ?: return
        //改变任务状态，将任务状态置为EXECUTING
        downloadTask.start()
        //是否为断点续传标志位,不论是本地持久器返回的任务还是被中断需要继续的task，只要progress不为0，就说明仅需要使用分片下载
        val isAppendedDownload = downloadTask.progress != 0L
        //根据断点续传标志构建OkHttp Request
        val okHttpRequest = buildRequest(downloadTask, url, isAppendedDownload)
        //开始从response流中读取二进制数据，并写入到本地文件中，执行下载流程
        readResponseAndWriteLocalFile(downloadTask, okHttpRequest, isAppendedDownload)
    }

    //校验download的合法性，是否为HTTP协议的下载链接
    private fun checkProto(downloadTask: PKDownloadTask): HttpUrl? {
        val downloadRequest = downloadTask.pkRequest
        val url = downloadRequest.targetUrl.toHttpUrlOrNull()
        if (url == null) {
            //将下载任务置为失败
            downloadTask.fail(reason = "download url is invalid for HTTP!")
            client.pkDispatcher.complete(downloadTask)
            return null
        }
        return url
    }

    //根据是否是断点续传标志位构建用来请求的OkHttpRequest
    private fun buildRequest(
        downloadTask: PKDownloadTask,
        url: HttpUrl,
        isAppendedDownload: Boolean
    ): Request {
        return if (isAppendedDownload) {
            //添加Range请求Header
            val range = "bytes=${downloadTask.progress}-"
            PKLog.debug("断点续传Range范围 : $range")
            Request.Builder().url(url).header("Range", range)
                .get().build()
        } else {
            //否则默认是新任务，完整的下载整个目标文件
            Request.Builder().url(url).get().build()
        }
    }

    //读取HTTP Response，并写入本地目标文件
    private fun readResponseAndWriteLocalFile(
        downloadTask: PKDownloadTask,
        httpRequest: Request,
        isAppendedDownload: Boolean
    ) {
        var fileSink: Sink? = null
        var targetBufferedSink: BufferedSink? = null
        var response: Response? = null
        var bodySource: BufferedSource? = null
        try {
            //使用okHttp创建HTTP同步连接
            response = okHttpClient.newCall(httpRequest).execute()
            //206为分片下载返回的code
            if (response.code != 200 && response.code != 206) {
                throw RuntimeException("Download request return code ${response.code}")
            }
            val fileName = response.request.url.pathSegments.last()
            //获取下载目标文件的文件名
            downloadTask.downloadFileName = fileName
            val resourceTag = response.header("ETag")
            //比较目标文件唯一版本标识是否与当前task持有的有所不同，若不同需要重新下载
            if (!downloadTask.versionTagId.isNullOrBlank() && !resourceTag.isNullOrBlank()) {
                //如果任务本身的versionTagID不为空说明当前任务是本地持久器返回的或是之前被暂停的任务
                if (downloadTask.versionTagId != resourceTag) {
                    /*如果task曾经保存的versionTag和当前服务器返回的ETag不相同，说明在这段时间服务器上的相同文件发生了变化，
                    那么需要重新下载该文件*/
                    response.closeQuietly()
                    downloadTask.versionTagId = resourceTag
                    downloadTask.progress = 0L
                    throw PKTaskNeedReDownloadException()
                }
            }
            //获取目标文件在服务器的唯一版本标识
            downloadTask.versionTagId = resourceTag
            response.body?.use {
                if (!isAppendedDownload) {
                    //只有在完整下载时才将task的目标文件大小设置为ResponseBody中的contextLength
                    downloadTask.contentLength = it.contentLength()
                }
                PKLog.debug("当前任务需要下载的分片大小为${getDownloadFileSizeDescription(it.contentLength())}")
                val targetFile = File(downloadTask.pkRequest.localSinkFilePath, fileName)
                //根据断点续传标志位设置是否需要在文件末尾追加写入
                fileSink = if (isAppendedDownload) targetFile.appendingSink() else targetFile.sink()
                targetBufferedSink = fileSink?.buffer()
                //在主线程触发task onStart回调监听，开始读取流并写入到本地文件
                mainHandler.post {
                    downloadTask.pkRequest.taskProcessListener?.onStart(downloadTask)
                    client.pkGlobalTaskProcessListenerList.forEach { listener ->
                        listener.onStart(downloadTask)
                    }
                }
                downloadTask.triggerPersist()
                var remainingSize = it.contentLength()
                bodySource = it.source()
                while (true) {
                    if (remainingSize == 0L) {
                        break
                    }
                    //判断任务是否被取消，若被取消，则中断下载
                    if (downloadTask.isCancel()) {
                        closeAll(fileSink, targetBufferedSink, bodySource)
                        client.pkDispatcher.complete(downloadTask)
                        targetFile.delete()
                        throw PKTaskCanceledException()
                    }
                    synchronized(downloadTask) {
                        while (downloadTask.isPause()) {
                            downloadTask.wait()
                            //任务恢复后需要再次判断任务是否被取消，若任务被取消，则直接中断下载
                            if (downloadTask.isCancel()) {
                                closeAll(fileSink, targetBufferedSink, bodySource)
                                client.pkDispatcher.complete(downloadTask)
                                targetFile.delete()
                                throw PKTaskCanceledException()
                            }
                            closeAll(fileSink, targetBufferedSink, bodySource)
                            //重新建立连接，继续从当前进度继续下载（不直接继续读取body的原因是为了防止连接超时）
                            throw PKTaskInterruptedException()
                        }
                    }
                    var array: ByteArray
                    val readByteCount: Long = if (remainingSize < DEFAULT_CHUNK_SIZE) {
                        remainingSize
                    } else {
                        DEFAULT_CHUNK_SIZE
                    }
                    array = bodySource!!.readByteArray(readByteCount)
                    remainingSize -= readByteCount

                    if (array.isNotEmpty()) {
                        targetBufferedSink?.write(array)
                        targetBufferedSink?.flush()

                        downloadTask.changeProgress(array.size.toLong())
                        //在主线程触发下载进度改变回调监听
                        mainHandler.post {
                            downloadTask.pkRequest.taskProcessListener?.onProcess(
                                downloadTask.progress, it.contentLength(), downloadTask
                            )
                            client.pkGlobalTaskProcessListenerList.forEach { listener ->
                                listener.onProcess(
                                    downloadTask.progress, it.contentLength(), downloadTask
                                )
                            }
                        }
                    } else {
                        break
                    }
                }
                //下载完成后，关闭所有资源
                closeAll(fileSink, targetBufferedSink, bodySource)
                //将下载完成后的file赋值给task的resultFile
                downloadTask.downloadResultFile = targetFile
                PKLog.debug("目标文件${downloadTask.downloadFileName}下载完成")
                downloadTask.success()
                //在主线程触发任务完成回调监听
                mainHandler.post {
                    downloadTask.pkRequest.taskProcessListener?.onComplete(downloadTask)
                    client.pkGlobalTaskProcessListenerList.forEach { listener ->
                        listener.onComplete(downloadTask)
                    }
                }
                client.pkDispatcher.complete(downloadTask)
            }
        } catch (reDownloadException: PKTaskNeedReDownloadException) {
            PKLog.debug("目标资源发生了变更，需要重新下载该文件")
            download(downloadTask)
        } catch (interruptedException: PKTaskInterruptedException) {
            PKLog.debug("任务被中断，需要重新建立连接继续向服务器下载文件")
            download(downloadTask)
        } catch (canceledException: PKTaskCanceledException) {
            PKLog.debug("任务被取消，停止该任务的下载")
            downloadTask.pkRequest.taskProcessListener?.onCancel(downloadTask)
            client.pkGlobalTaskProcessListenerList.forEach { listener ->
                listener.onCancel(downloadTask)
            }
            client.pkDispatcher.complete(downloadTask)
        } catch (e: Exception) {
            //先关闭所有IO资源
            closeAll(fileSink, targetBufferedSink, bodySource, response)
            //将对应的task status置为失败，
            PKLog.error("download fail ${e.message}")
            downloadTask.fail(reason = "download execute fail by exception ${e.message}")
            client.pkDispatcher.complete(downloadTask)
            //在主线程触发任务失败监听
            mainHandler.post {
                downloadTask.pkRequest.taskProcessListener?.onFail(
                    "download execute fail by exception ${e.message}",
                    e, downloadTask
                )
                client.pkGlobalTaskProcessListenerList.forEach { listener ->
                    listener.onFail(
                        "download execute fail by exception ${e.message}",
                        e, downloadTask
                    )
                }
            }
        }
    }
}