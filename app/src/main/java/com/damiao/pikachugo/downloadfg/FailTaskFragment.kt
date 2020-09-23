package com.damiao.pikachugo.downloadfg

import android.os.Bundle
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
import kotlinx.android.synthetic.main.fragment_fail_task.view.*

class FailTaskFragment : Fragment() {

    private val failTaskList =
        mutableListOf<PKDownloadTask>()

    private val downloadTaskAdapter: DownloadTaskListAdapter by lazy {
        DownloadTaskListAdapter(failTaskList)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fail_task, container, false)
        initView(view)
        return view
    }


    private fun initView(view: View) {
        failTaskList.addAll(Pikachu.pkTaskGetter.getFailTaskList())

        view.rv_fg_fail_list.setHasFixedSize(true)
        view.rv_fg_fail_list.layoutManager = LinearLayoutManager(activity)
        view.rv_fg_fail_list.adapter = downloadTaskAdapter


        val taskProcessListener = object : PKTaskProcessListener {

            override fun onFail(
                reason: String,
                exception: Exception?,
                downloadTask: PKDownloadTask
            ) {
                failTaskList.add(0, downloadTask)
                downloadTaskAdapter.notifyItemInserted(0)
            }
        }

        Pikachu.addGlobalTaskProcessListener(taskProcessListener, this)
    }
}