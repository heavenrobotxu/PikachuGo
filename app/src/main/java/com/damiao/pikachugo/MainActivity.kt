package com.damiao.pikachugo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask
import com.damiao.pikachu.common.PKTaskProcessListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private var taskIndex = 0

    private val downloadTaskList =
        mutableListOf<PKDownloadTask>()

    private val downloadTaskAdapter: DownloadTaskListAdapter by lazy {
        DownloadTaskListAdapter(downloadTaskList)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        downloadTaskList.addAll(Pikachu.getLocalTask())

        rv_main_download_list.setHasFixedSize(true)
        rv_main_download_list.layoutManager = LinearLayoutManager(this)
        downloadTaskAdapter.setHasStableIds(true)
        rv_main_download_list.adapter = downloadTaskAdapter

        iv_main_add_new_task.setOnClickListener {
            val task = Pikachu.with(this)
                .url(DEFAULT_TASK_URL[taskIndex])
                .download()
            downloadTaskList.add(0, task)
            downloadTaskAdapter.notifyItemInserted(0)
            taskIndex++
        }
        Pikachu.addGlobalTaskProcessListener(object : PKTaskProcessListener {

            override fun onProcess(process: Long, length: Long, taskId: String) {
                downloadTaskAdapter
                    .notifyItemChanged(downloadTaskList.indexOf(downloadTaskList.find { it.taskId == taskId }))
            }

            override fun onComplete(taskId: String) {
                downloadTaskAdapter
                    .notifyItemChanged(downloadTaskList.indexOf(downloadTaskList.find { it.taskId == taskId }))
            }

            override fun onCancel(taskId: String) {
                downloadTaskAdapter
                    .notifyItemChanged(downloadTaskList.indexOf(downloadTaskList.find { it.taskId == taskId }))
            }
        })
    }

}