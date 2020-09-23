package com.damiao.pikachugo

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.damiao.pikachugo.downloadfg.CancelTaskFragment
import com.damiao.pikachugo.downloadfg.CompleteTaskFragment
import com.damiao.pikachugo.downloadfg.DownloadingTaskFragment
import com.damiao.pikachugo.downloadfg.FailTaskFragment

class DownloadTaskTabAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    private val downloadTaskFragmentList = arrayOf(
        DownloadingTaskFragment(),
        CompleteTaskFragment(),
        FailTaskFragment(),
        CancelTaskFragment()
    )

    override fun getItemCount(): Int = downloadTaskFragmentList.size

    override fun createFragment(position: Int): Fragment {
        return downloadTaskFragmentList[position]
    }

}