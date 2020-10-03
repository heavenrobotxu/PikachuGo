package com.damiao.pikachugo.downloadfg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask
import com.damiao.pikachu.common.PKTaskProcessListener
import com.damiao.pikachugo.DownloadTaskListAdapter
import com.damiao.pikachugo.R
import kotlinx.android.synthetic.main.fragment_downloading.view.*


class DownloadingTaskFragment : Fragment() {

    private val downloadingTaskList =
        mutableListOf<PKDownloadTask>()

    private val downloadTaskAdapter: DownloadTaskListAdapter by lazy {
        DownloadTaskListAdapter(downloadingTaskList)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_downloading, container, false)
        initView(view)
        return view
    }

    private fun initView(view: View) {
        downloadingTaskList.addAll(Pikachu.pkTaskGetter.getUnCompleteTaskList())

        view.rv_fg_downloading_list.setHasFixedSize(true)
        view.rv_fg_downloading_list.layoutManager = LinearLayoutManager(activity)
        downloadTaskAdapter.setHasStableIds(true)
        (view.rv_fg_downloading_list.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        view.rv_fg_downloading_list.adapter = downloadTaskAdapter


        val taskProcessListener = object : PKTaskProcessListener {

            override fun onReady(downloadTask: PKDownloadTask) {
                if (downloadingTaskList.find { it.taskId == downloadTask.taskId } == null) {
                    downloadingTaskList.add(0, downloadTask)
                    downloadTaskAdapter.notifyItemInserted(0)
                }
            }

            override fun onStart(downloadTask: PKDownloadTask) {
                notifyAdapter(downloadTask)
            }

            override fun onProcess(process: Long, length: Long, downloadTask: PKDownloadTask) {
                notifyAdapter(downloadTask)
            }

            override fun onComplete(downloadTask: PKDownloadTask) {
                val position = downloadingTaskList.indexOf(downloadingTaskList
                    .find { it.taskId == downloadTask.taskId })
                if (position != -1) {
                    downloadingTaskList.removeAt(position)
                    downloadTaskAdapter.notifyItemRemoved(position)
                }
            }

            override fun onFail(
                reason: String,
                exception: Exception?,
                downloadTask: PKDownloadTask
            ) {
                val position = downloadingTaskList.indexOf(downloadingTaskList
                    .find { it.taskId == downloadTask.taskId })
                if (position != -1) {
                    downloadingTaskList.removeAt(position)
                    downloadTaskAdapter.notifyItemRemoved(position)
                }
            }

            override fun onCancel(downloadTask: PKDownloadTask) {
                val position = downloadingTaskList.indexOf(downloadingTaskList
                    .find { it.taskId == downloadTask.taskId })
                if (position != -1) {
                    downloadingTaskList.removeAt(position)
                    downloadTaskAdapter.notifyItemRemoved(position)
                }
            }
        }

        Pikachu.addGlobalTaskProcessListener(taskProcessListener, this)
    }

    private fun notifyAdapter(downloadTask: PKDownloadTask) {
        val changedItem = downloadingTaskList.find { it.taskId == downloadTask.taskId}
        if (changedItem != null) {
            downloadTaskAdapter
                .notifyItemChanged(downloadingTaskList.indexOf(changedItem))
        }
    }

}