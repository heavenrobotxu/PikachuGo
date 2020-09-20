package com.damiao.pikachugo

import android.Manifest
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask
import com.damiao.pikachu.common.PKTaskProcessListener
import kotlinx.android.synthetic.main.activity_download.*
import org.jetbrains.anko.toast
import java.io.File

class DownloadActivity : AppCompatActivity() {

    private var taskIndex = 0

    private val downloadTaskList =
        mutableListOf<PKDownloadTask>()

    private val downloadTaskAdapter: DownloadTaskListAdapter by lazy {
        DownloadTaskListAdapter(downloadTaskList)
    }

    private lateinit var taskListener: PKTaskProcessListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)

        downloadTaskList.addAll(Pikachu.getLocalTask())

        rv_main_download_list.setHasFixedSize(true)
        rv_main_download_list.layoutManager = LinearLayoutManager(this)
        downloadTaskAdapter.setHasStableIds(true)
        rv_main_download_list.adapter = downloadTaskAdapter
        val localFileDir = File(Environment.getExternalStorageDirectory(), "miao-download")
        iv_main_add_new_task.setOnClickListener {
            val task = Pikachu.with(this)
                .url(DEFAULT_TASK_URL[taskIndex])
                //.targetPath(localFileDir.path)
                .download()
            downloadTaskList.add(0, task)
            downloadTaskAdapter.notifyItemInserted(0)
            taskIndex++
        }
        taskListener = object : PKTaskProcessListener {

            override fun onProcess(process: Long, length: Long, taskId: String) {
                notifyItem(taskId)
            }

            override fun onComplete(taskId: String) {
                notifyItem(taskId)
            }

            override fun onCancel(taskId: String) {
                notifyItem(taskId)
            }

            override fun onFail(reason: String, exception: Exception?, taskId: String) {
                toast("任务下载失败 $reason")
                notifyItem(taskId)
            }
        }
        Pikachu.addGlobalTaskProcessListener(taskListener, this)
    }

    fun notifyItem(taskId: String) {
        downloadTaskAdapter
            .notifyItemChanged(downloadTaskList.indexOf(downloadTaskList.find { it.taskId == taskId }))
    }

    override fun onDestroy() {
        Pikachu.removeGlobalTaskProcessListener(taskListener)
        super.onDestroy()
    }

}