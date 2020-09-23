package com.damiao.pikachugo.downloadfg

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask
import com.damiao.pikachu.common.PKTaskProcessListener
import com.damiao.pikachugo.DownloadTaskListAdapter
import com.damiao.pikachugo.R
import kotlinx.android.synthetic.main.fragment_complete_task.view.*


class CompleteTaskFragment : Fragment() {

    private val completeTaskList =
        mutableListOf<PKDownloadTask>()

    private val downloadTaskAdapter: DownloadTaskListAdapter by lazy {
        DownloadTaskListAdapter(completeTaskList)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_complete_task, container, false)
        initView(view)
        return view
    }

    private fun initView(view: View) {
        completeTaskList.addAll(Pikachu.pkTaskGetter.getCompleteTaskList())

        view.rv_fg_complete_list.setHasFixedSize(true)
        view.rv_fg_complete_list.layoutManager = LinearLayoutManager(activity)
        view.rv_fg_complete_list.adapter = downloadTaskAdapter


        val taskProcessListener = object : PKTaskProcessListener {

            override fun onComplete(downloadTask: PKDownloadTask) {
                Log.d("GOGOGOGO", "任务完成 ${downloadTask.downloadFileName}")
                completeTaskList.add(0, downloadTask)
                downloadTaskAdapter.notifyItemInserted(0)
            }
        }

        Pikachu.addGlobalTaskProcessListener(taskProcessListener, this)
    }
}