package com.damiao.pikachu.core

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.*
import java.io.File
import java.util.*

internal class PKSQLiteDownloadTaskPersister(private val pikachu: Pikachu) :
    PkDownloadTaskPersister {

    private val dbHelper: PKDownloadTaskDbOpenHelper = PKDownloadTaskDbOpenHelper(pikachu.app)

    //向数据库中插入一条下载任务记录，在下载任务刚被提交到PK Dispatcher中后就会被触发执行
    override fun saveDownloadTask(downloadTask: PKDownloadTask) {
        PKLog.debug("插入一条下载任务 : ${downloadTask.pkRequest.targetUrl} ")
        dbHelper.writableDatabase.insert(
            PK_TABLE_TASK_NAME, null,
            ContentValues().apply {
                //保存任务taskId
                put(PK_TABLE_TASK_COLUMN_TASK_ID, downloadTask.taskId)
                //保存下载任务状态，默认为READY
                put(PK_TABLE_TASK_COLUMN_TASK_STATUS, downloadTask.status)
                //保存下载任务目标本地目录
                put(
                    PK_TABLE_TASK_COLUMN_TASK_LOCAL_FILE_PATH,
                    downloadTask.pkRequest.localSinkFilePath
                )
                //保存下载任务url
                put(PK_TABLE_TASK_COLUMN_TASK_TARGET_URL, downloadTask.pkRequest.targetUrl)
            })
    }

    //更新下载任务在本地数据库中的状态，当下载进度更新、任务状态变更时候会被触发
    @Synchronized
    override fun updateDownloadTask(downloadTask: PKDownloadTask) {
        PKLog.debug("下载任务被更新了 : ${downloadTask.downloadFileName} ")
        val updateValues = ContentValues().apply {
            downloadTask.downloadFileName?.let {
                put(PK_TABLE_TASK_COLUMN_TASK_DOWNLOAD_FILE_NAME, it)
            }
            downloadTask.failType?.let {
                put(PK_TABLE_TASK_COLUMN_TASK_FAIL_TYPE, it)
            }
            downloadTask.failMessage?.let {
                put(PK_TABLE_TASK_COLUMN_TASK_FAIL_MESSAGE, it)
            }
            downloadTask.versionTagId?.let {
                put(PK_TABLE_TASK_COLUMN_TASK_VERSION_TAG_ID, it)
            }
            put(PK_TABLE_TASK_COLUMN_TASK_STATUS, downloadTask.status)
            put(PK_TABLE_TASK_COLUMN_TASK_PROGRESS, downloadTask.progress)
            put(PK_TABLE_TASK_COLUMN_TASK_CONTENT_LENGTH, downloadTask.contentLength)
        }
        dbHelper.writableDatabase.update(
            PK_TABLE_TASK_NAME, updateValues,
            "$PK_TABLE_TASK_COLUMN_TASK_ID =?", arrayOf(downloadTask.taskId)
        )
    }

    @Synchronized
    override fun deleteDownloadTask(downloadTask: PKDownloadTask) {
        dbHelper.writableDatabase.delete(
            PK_TABLE_TASK_NAME,
            "$PK_TABLE_TASK_COLUMN_TASK_ID =?", arrayOf(downloadTask.taskId)
        )
    }

    //查询当前数据库中还未下载完成的任务(包含准备中、提交未执行、正在执行中、以及执行中被暂停4种状态的任务)
    @Synchronized
    override fun getUnCompleteTaskList(): List<PKDownloadTask> {
        val cursor = dbHelper.readableDatabase.query(
            PK_TABLE_TASK_NAME, null,
            "$PK_TABLE_TASK_COLUMN_TASK_STATUS = ? OR " +
                    "$PK_TABLE_TASK_COLUMN_TASK_STATUS = ? OR " +
                    "$PK_TABLE_TASK_COLUMN_TASK_STATUS = ? OR " +
                    "$PK_TABLE_TASK_COLUMN_TASK_STATUS = ?",
            arrayOf(
                "${PKTask.TASK_STATUS_READY}",
                "${PKTask.TASK_STATUS_EXECUTING}",
                "${PKTask.TASK_STATUS_PAUSE}"
            ), null, null, "$PK_TABLE_TASK_COLUMN_ID DESC"
        )
        return getTaskListFromCursor(cursor)
    }

    //查询当前数据库中已经正常下载完成的任务列表
    override fun getCompleteTaskList(): List<PKDownloadTask> {
        val cursor = dbHelper.readableDatabase.query(
            PK_TABLE_TASK_NAME, null,
            "$PK_TABLE_TASK_COLUMN_TASK_STATUS = ?",
            arrayOf("${PKTask.TASK_STATUS_COMPLETE}"),
            null, null, "$PK_TABLE_TASK_COLUMN_ID DESC"
        )
        return getTaskListFromCursor(cursor)
    }

    //查询当前数据库中下载失败的任务列表
    override fun getFailTaskList(): List<PKDownloadTask> {
        val cursor = dbHelper.readableDatabase.query(
            PK_TABLE_TASK_NAME, null,
            "$PK_TABLE_TASK_COLUMN_TASK_STATUS = ?",
            arrayOf("${PKTask.TASK_STATUS_FAIL}"),
            null, null, "$PK_TABLE_TASK_COLUMN_ID DESC"
        )
        return getTaskListFromCursor(cursor)
    }

    override fun getCancelTaskList(): List<PKDownloadTask> {
        val cursor = dbHelper.readableDatabase.query(
            PK_TABLE_TASK_NAME, null,
            "$PK_TABLE_TASK_COLUMN_TASK_STATUS = ?",
            arrayOf("${PKTask.TASK_STATUS_CANCEL}"),
            null, null, "$PK_TABLE_TASK_COLUMN_ID DESC"
        )
        return getTaskListFromCursor(cursor)
    }

    //查询数据库中的所有下载任务
    @Synchronized
    override fun getAllDownloadTaskList(): List<PKDownloadTask> {
        val cursor = dbHelper.readableDatabase.query(
            PK_TABLE_TASK_NAME, null, null,
            null, null, null, "$PK_TABLE_TASK_COLUMN_ID DESC"
        )
        return getTaskListFromCursor(cursor)
    }

    //根据指定taskId查询某个对应的下载任务
    @Synchronized
    override fun getDownloadTaskByTaskId(taskId: String): PKDownloadTask? {
        val cursor = dbHelper.readableDatabase.query(
            PK_TABLE_TASK_NAME, null,
            "$PK_TABLE_TASK_COLUMN_TASK_ID = ?",
            arrayOf(taskId), null, null, null
        )
        val result = getTaskListFromCursor(cursor)
        return if (result.isEmpty()) null else result[0]
    }

    private fun getTaskListFromCursor(
        cursor: Cursor
    ): List<PKDownloadTask> {
        val result = LinkedList<PKDownloadTask>()
        cursor.use {
            if (!it.moveToFirst()) return result
            do {
                val taskId = it.getString(it.getColumnIndex(PK_TABLE_TASK_COLUMN_TASK_ID))
                val contentLength =
                    it.getLongOrNull(it.getColumnIndex(PK_TABLE_TASK_COLUMN_TASK_CONTENT_LENGTH))
                val status = it.getInt(it.getColumnIndex(PK_TABLE_TASK_COLUMN_TASK_STATUS))
                val downloadFileName = it.getStringOrNull(
                    it.getColumnIndex(
                        PK_TABLE_TASK_COLUMN_TASK_DOWNLOAD_FILE_NAME
                    )
                )
                val versionTagId = it.getStringOrNull(
                    it.getColumnIndex(
                        PK_TABLE_TASK_COLUMN_TASK_VERSION_TAG_ID
                    )
                )
                val failType =
                    it.getIntOrNull(it.getColumnIndex(PK_TABLE_TASK_COLUMN_TASK_FAIL_TYPE))
                val failMessage = it.getStringOrNull(
                    it.getColumnIndex(
                        PK_TABLE_TASK_COLUMN_TASK_FAIL_MESSAGE
                    )
                )
                val localPath = it.getString(
                    it.getColumnIndex(
                        PK_TABLE_TASK_COLUMN_TASK_LOCAL_FILE_PATH
                    )
                )
                val targetUrl =
                    it.getString(it.getColumnIndex(PK_TABLE_TASK_COLUMN_TASK_TARGET_URL))
                val downloadRequest = PKDownloadTaskRequest(targetUrl, localPath, null)
                val task = PKRealDownloadTask(
                    downloadRequest,
                    taskId = taskId,
                    downloadFileName = downloadFileName
                )
                task.progress = 0
                downloadFileName?.let { name ->
                    val localFile = File(localPath, name)
                    if (localFile.exists()) {
                        //若本地文件存在，则直接使用本地文件的length作为已下载的progress值，若本地文件被删除或不存在，则progress为0
                        //任务从0开始重新下载
                        task.progress = localFile.length()
                        if (localFile.length() == contentLength) {
                            //本地文件大小与下载总大小相同，说明当前文件已经下载完成
                            task.downloadResultFile = localFile
                        }
                    }
                }
                task.contentLength = contentLength ?: 0
                //假如入库时当前task已经在执行下载，则将status设置为被中断
                if (status == PKTask.TASK_STATUS_READY || status == PKTask.TASK_STATUS_PAUSE ||
                    status == PKTask.TASK_STATUS_EXECUTING
                ) {
                    task.status = PKTask.TASK_STATUS_INTERRUPTED
                } else {
                    task.status = status
                }
                task.failType = failType
                task.failMessage = failMessage
                task.versionTagId = versionTagId
                result.add(task)
            } while (it.moveToNext())
        }
        return result
    }

    companion object {

        private const val PK_DB_NAME = "pikachu_task_info"

        private const val PK_TABLE_TASK_NAME = "download_task_info"

        private const val PK_TABLE_TASK_COLUMN_ID = "_id"
        private const val PK_TABLE_TASK_COLUMN_TASK_ID = "task_id"
        private const val PK_TABLE_TASK_COLUMN_TASK_PROGRESS = "progress"
        private const val PK_TABLE_TASK_COLUMN_TASK_CONTENT_LENGTH = "content_length"
        private const val PK_TABLE_TASK_COLUMN_TASK_STATUS = "status"
        private const val PK_TABLE_TASK_COLUMN_TASK_FAIL_TYPE = "fail_type"
        private const val PK_TABLE_TASK_COLUMN_TASK_DOWNLOAD_FILE_NAME = "down_file_name"
        private const val PK_TABLE_TASK_COLUMN_TASK_TARGET_URL = "target_url"
        private const val PK_TABLE_TASK_COLUMN_TASK_LOCAL_FILE_PATH = "local_file_path"
        private const val PK_TABLE_TASK_COLUMN_TASK_FAIL_MESSAGE = "fail_message"
        private const val PK_TABLE_TASK_COLUMN_TASK_VERSION_TAG_ID = "e_tag_id"

    }

    private class PKDownloadTaskDbOpenHelper(context: Context) : SQLiteOpenHelper(
        context, PK_DB_NAME, null, 1
    ) {

        override fun onCreate(db: SQLiteDatabase?) {
            db?.let {
                createTaskInfoTable(it)
            }
        }

        private fun createTaskInfoTable(db: SQLiteDatabase) {
            //新增download_task_info持久化task信息
            PKLog.debug("初始化下载任务表")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS $PK_TABLE_TASK_NAME (
                          $PK_TABLE_TASK_COLUMN_ID integer PRIMARY KEY AUTOINCREMENT,
                          $PK_TABLE_TASK_COLUMN_TASK_ID text NOT NULL UNIQUE,
                          $PK_TABLE_TASK_COLUMN_TASK_PROGRESS long,
                          $PK_TABLE_TASK_COLUMN_TASK_CONTENT_LENGTH long,
                          $PK_TABLE_TASK_COLUMN_TASK_STATUS integer,
                          $PK_TABLE_TASK_COLUMN_TASK_FAIL_TYPE integer,
                          $PK_TABLE_TASK_COLUMN_TASK_FAIL_MESSAGE text,
                          $PK_TABLE_TASK_COLUMN_TASK_DOWNLOAD_FILE_NAME text,
                          $PK_TABLE_TASK_COLUMN_TASK_VERSION_TAG_ID text,
                          $PK_TABLE_TASK_COLUMN_TASK_TARGET_URL text NOT NULL,
                          $PK_TABLE_TASK_COLUMN_TASK_LOCAL_FILE_PATH text  NOT NULL)"""
            )
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
    }
}