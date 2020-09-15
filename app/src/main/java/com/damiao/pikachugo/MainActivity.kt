package com.damiao.pikachugo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask
import com.damiao.pikachu.common.PKRealDownloadTask
import com.damiao.pikachu.common.PKTask
import com.damiao.pikachu.common.PKTaskProcessListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var task : PKDownloadTask? = null

        btn_a_download.setOnClickListener {
            task = Pikachu.with(this)
                .url("http://192.168.0.102:8080/download/a.mp4")
                .taskProcessListener(object : PKTaskProcessListener {
                    override fun onStart(downloadTask: PKDownloadTask) {
                        toast("任务A下载开始啦")
                    }

                    override fun onProcess(process: Long, length: Long) {
                        if (pb_a_process.max == 100) {
                            pb_a_process.max = length.toInt()
                        }
                        pb_a_process.progress = process.toInt()
                        tv_a_speed.text = task?.downloadSpeed
                    }

                    override fun onComplete(downloadTask: PKDownloadTask) {
                        toast("下载完成 ${downloadTask.downloadResultFile?.path}")
                    }

                    override fun onFail(reason: String, exception: Exception?) {
                        toast("下载失败，呵呵 $reason")
                    }
                })
                .download()

        }

        btn_a_pause.setOnClickListener {
            task?.pause()
        }

        btn_a_resume.setOnClickListener {
            task?.resume()
        }

        val interruptedTaskList = Pikachu.getLocalInterruptedTask()
        for (pkDownloadTask in interruptedTaskList) {
            pb_a_process.max = pkDownloadTask.contentLength.toInt()
            pb_a_process.progress = pkDownloadTask.progress.toInt()
            btn_a_local_resume.setOnClickListener {
                pkDownloadTask.resume()
            }
            pkDownloadTask.pkRequest.taskProcessListener = object : PKTaskProcessListener {
                override fun onProcess(process: Long, length: Long) {
                    pb_a_process.progress = process.toInt()
                    tv_a_speed.text = pkDownloadTask.downloadSpeed
                }

                override fun onComplete(downloadTask: PKDownloadTask) {
                    toast("下载完成 ${downloadTask.downloadResultFile?.path}")
                }

                override fun onFail(reason: String, exception: Exception?) {
                    toast("下载失败，呵呵 $reason")
                }
            }

            btn_a_pause.setOnClickListener {
                pkDownloadTask.pause()
            }

            btn_a_resume.setOnClickListener {
                pkDownloadTask.resume()
            }
        }


    }
}