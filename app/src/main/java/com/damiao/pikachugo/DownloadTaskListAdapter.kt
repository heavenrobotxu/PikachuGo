package com.damiao.pikachugo

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask
import com.damiao.pikachu.common.PKTask
import com.damiao.pikachu.util.getDownloadFileSizeDescription
import kotlinx.android.synthetic.main.item_layout_download_detail_info.view.*

class DownloadTaskListAdapter(private val downloadTaskList: MutableList<PKDownloadTask>) :
    RecyclerView.Adapter<DownloadTaskListAdapter.DownloadTaskViewHolder>() {
    //状态模式，操作不同状态下viewHolder的表现
    private val stateMap = mapOf(
        PKTask.TASK_STATUS_READY to ReadyStateBehavior(),
        PKTask.TASK_STATUS_EXECUTING to DownloadingStateBehavior(),
        PKTask.TASK_STATUS_PAUSE to PauseStateBehavior(),
        PKTask.TASK_STATUS_FAIL to FailStateBehavior(),
        PKTask.TASK_STATUS_CANCEL to CancelStateBehavior(),
        PKTask.TASK_STATUS_COMPLETE to CompleteStateBehavior(),
        PKTask.TASK_STATUS_INTERRUPTED to InterruptedStateBehavior()
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadTaskViewHolder {
        return DownloadTaskViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_layout_download_detail_info, parent, false)
        )
    }

    override fun getItemCount(): Int = downloadTaskList.size

    override fun onBindViewHolder(
        holder: DownloadTaskViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        }
    }

    override fun getItemId(position: Int): Long {
        return downloadTaskList[position].hashCode().toLong()
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: DownloadTaskViewHolder, position: Int) {
        val downloadTask = downloadTaskList[position]
        stateMap[downloadTask.status]?.change(
            holder,
            holder.itemView, position,
            downloadTask, this
        )
    }

    class DownloadTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    abstract class StateBehavior {

        abstract fun change(
            holder: DownloadTaskViewHolder,
            itemView: View,
            p: Int,
            downloadTask: PKDownloadTask,
            adapter: DownloadTaskListAdapter
        )
    }

    class ReadyStateBehavior : StateBehavior() {

        override fun change(
            holder: DownloadTaskViewHolder,
            itemView: View,
            p: Int,
            downloadTask: PKDownloadTask,
            adapter: DownloadTaskListAdapter
        ) {
            itemView.tv_item_download_task_name.text = downloadTask.pkRequest.targetUrl
            itemView.tv_item_download_task_progress_detail.text = "等待下载..."
            itemView.tv_item_download_task_speed.text = ""
            itemView.pb_item_download_progress.progress = 0
            itemView.pb_item_download_progress.max = 0
            itemView.iv_item_download_task_pause.isEnabled = false
            itemView.iv_item_download_task_resume.isEnabled = false
            itemView.iv_item_download_task_cancel.setImageResource(R.drawable.selector_cancel)
            itemView.iv_item_download_task_cancel.isEnabled = true
            itemView.iv_item_download_task_open_folder.isEnabled = false
            itemView.iv_item_download_task_cancel.setOnClickListener {
                downloadTask.cancel()
            }
        }
    }

    class DownloadingStateBehavior : StateBehavior() {

        @SuppressLint("SetTextI18n")
        override fun change(
            holder: DownloadTaskViewHolder,
            itemView: View,
            p: Int,
            downloadTask: PKDownloadTask,
            adapter: DownloadTaskListAdapter
        ) {
            itemView.tv_item_download_task_name.text = downloadTask.downloadFileName
            itemView.tv_item_download_task_progress_detail.text =
                "${getDownloadFileSizeDescription(downloadTask.progress)} / " +
                        getDownloadFileSizeDescription(downloadTask.contentLength)
            itemView.tv_item_download_task_speed.text = downloadTask.downloadSpeed
            if (itemView.pb_item_download_progress.max != downloadTask.contentLength.toInt()) {
                itemView.pb_item_download_progress.max = downloadTask.contentLength.toInt()
            }
            itemView.pb_item_download_progress.progress = downloadTask.progress.toInt()
            itemView.iv_item_download_task_pause.isEnabled = true
            itemView.iv_item_download_task_pause.setOnClickListener {
                downloadTask.pause()
            }
            itemView.iv_item_download_task_resume.isEnabled = false
            itemView.iv_item_download_task_cancel.setImageResource(R.drawable.selector_cancel)
            itemView.iv_item_download_task_cancel.isEnabled = true
            itemView.iv_item_download_task_cancel.setOnClickListener {
                downloadTask.cancel()
            }
            itemView.iv_item_download_task_open_folder.isEnabled = false
        }
    }

    class PauseStateBehavior : StateBehavior() {

        @SuppressLint("SetTextI18n")
        override fun change(
            holder: DownloadTaskViewHolder,
            itemView: View,
            p: Int,
            downloadTask: PKDownloadTask,
            adapter: DownloadTaskListAdapter
        ) {
            itemView.tv_item_download_task_name.text = downloadTask.downloadFileName
            itemView.tv_item_download_task_progress_detail.text =
                "${getDownloadFileSizeDescription(downloadTask.progress)} / " +
                        getDownloadFileSizeDescription(downloadTask.contentLength)
            itemView.tv_item_download_task_speed.text = ""
            if (itemView.pb_item_download_progress.max != downloadTask.contentLength.toInt()) {
                itemView.pb_item_download_progress.max = downloadTask.contentLength.toInt()
            }
            itemView.pb_item_download_progress.progress = downloadTask.progress.toInt()
            itemView.iv_item_download_task_pause.isEnabled = false
            itemView.iv_item_download_task_resume.isEnabled = true
            itemView.iv_item_download_task_resume.setOnClickListener {
                downloadTask.resume()
            }
            itemView.iv_item_download_task_cancel.setImageResource(R.drawable.selector_cancel)
            itemView.iv_item_download_task_cancel.isEnabled = true
            itemView.iv_item_download_task_cancel.setOnClickListener {
                downloadTask.cancel()
            }
            itemView.iv_item_download_task_open_folder.isEnabled = false
        }
    }

    inner class FailStateBehavior : StateBehavior() {

        @SuppressLint("SetTextI18n")
        override fun change(
            holder: DownloadTaskViewHolder,
            itemView: View,
            p: Int,
            downloadTask: PKDownloadTask,
            adapter: DownloadTaskListAdapter
        ) {

            itemView.tv_item_download_task_name.text =
                downloadTask.downloadFileName ?: downloadTask.pkRequest.targetUrl
            itemView.tv_item_download_task_progress_detail.text = "任务已失败"
            itemView.tv_item_download_task_speed.text = ""
            itemView.pb_item_download_progress.max = downloadTask.contentLength.toInt()
            itemView.pb_item_download_progress.progress = downloadTask.progress.toInt()
            itemView.iv_item_download_task_pause.isEnabled = false
            itemView.iv_item_download_task_resume.isEnabled = false
            itemView.iv_item_download_task_cancel.setImageResource(R.drawable.vector_drawable_delete)
            itemView.iv_item_download_task_cancel.setOnClickListener {
                Pikachu.deleteLocalTask(downloadTask, true)
                downloadTaskList.remove(downloadTask)
                //注意要用layoutPosition,不要用原本的p，因为onBind中传入的位置不会立刻因为item的删除或是移动而改变，
                //而layoutPosition才是真正可以随时保持刷新的真实位置
                adapter.notifyItemRemoved(holder.layoutPosition)
            }
            itemView.iv_item_download_task_open_folder.isEnabled = false
        }
    }

    inner class CancelStateBehavior : StateBehavior() {

        @SuppressLint("SetTextI18n")
        override fun change(
            holder: DownloadTaskViewHolder,
            itemView: View,
            p: Int,
            downloadTask: PKDownloadTask,
            adapter: DownloadTaskListAdapter
        ) {

            itemView.tv_item_download_task_name.text =
                downloadTask.downloadFileName ?: downloadTask.pkRequest.targetUrl
            itemView.tv_item_download_task_progress_detail.text = "任务已取消"
            itemView.tv_item_download_task_speed.text = ""
            itemView.pb_item_download_progress.max = 0
            itemView.pb_item_download_progress.progress = 0
            itemView.iv_item_download_task_pause.isEnabled = false
            itemView.iv_item_download_task_resume.isEnabled = false
            itemView.iv_item_download_task_cancel.isEnabled = true
            itemView.iv_item_download_task_cancel.setImageResource(R.drawable.vector_drawable_delete)
            itemView.iv_item_download_task_cancel.setOnClickListener {
                Pikachu.deleteLocalTask(downloadTask, true)
                downloadTaskList.remove(downloadTask)
                adapter.notifyItemRemoved(holder.layoutPosition)
            }
            itemView.iv_item_download_task_open_folder.isEnabled = false
        }
    }

    inner class CompleteStateBehavior : StateBehavior() {

        @SuppressLint("SetTextI18n")
        override fun change(
            holder: DownloadTaskViewHolder,
            itemView: View,
            p: Int,
            downloadTask: PKDownloadTask,
            adapter: DownloadTaskListAdapter
        ) {

            itemView.tv_item_download_task_name.text = downloadTask.downloadFileName
            itemView.tv_item_download_task_progress_detail.text =
                "${getDownloadFileSizeDescription(downloadTask.progress)} / " +
                        getDownloadFileSizeDescription(downloadTask.contentLength)
            itemView.tv_item_download_task_speed.text = "任务已完成"
            itemView.pb_item_download_progress.max = downloadTask.contentLength.toInt()
            itemView.pb_item_download_progress.progress = downloadTask.progress.toInt()
            itemView.iv_item_download_task_pause.isEnabled = false
            itemView.iv_item_download_task_resume.isEnabled = false
            itemView.iv_item_download_task_cancel.isEnabled = true
            itemView.iv_item_download_task_cancel.setImageResource(R.drawable.vector_drawable_delete)
            itemView.iv_item_download_task_cancel.setOnClickListener {
                Pikachu.deleteLocalTask(downloadTask, true)
                downloadTaskList.remove(downloadTask)
                adapter.notifyItemRemoved(holder.layoutPosition)
            }
            itemView.iv_item_download_task_open_folder.isEnabled = true
            itemView.iv_item_download_task_open_folder.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                val uri = Uri.parse(downloadTask.downloadResultFile?.path)
                intent.setDataAndType(uri, "video/*")
                it.context.startActivity(intent)
            }
        }
    }

    class InterruptedStateBehavior : StateBehavior() {

        @SuppressLint("SetTextI18n")
        override fun change(
            holder: DownloadTaskViewHolder,
            itemView: View,
            p: Int,
            downloadTask: PKDownloadTask,
            adapter: DownloadTaskListAdapter
        ) {

            itemView.tv_item_download_task_name.text = downloadTask.downloadFileName
            itemView.tv_item_download_task_progress_detail.text =
                "${getDownloadFileSizeDescription(downloadTask.progress)} / " +
                        getDownloadFileSizeDescription(downloadTask.contentLength)
            itemView.tv_item_download_task_speed.text = ""
            itemView.pb_item_download_progress.max = downloadTask.contentLength.toInt()
            itemView.pb_item_download_progress.progress = downloadTask.progress.toInt()
            itemView.iv_item_download_task_pause.isEnabled = false
            itemView.iv_item_download_task_resume.isEnabled = true
            itemView.iv_item_download_task_resume.setOnClickListener {
                downloadTask.resume()
            }
            itemView.iv_item_download_task_cancel.setImageResource(R.drawable.selector_cancel)
            itemView.iv_item_download_task_cancel.isEnabled = true
            itemView.iv_item_download_task_cancel.setOnClickListener {
                downloadTask.cancel()
            }
            itemView.iv_item_download_task_open_folder.isEnabled = false

        }
    }
}