package com.damiao.pikachugo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKDownloadTask
import com.damiao.pikachu.common.PKRealDownloadTask
import com.damiao.pikachu.common.PKTask
import com.damiao.pikachu.common.PKTaskProcessListener
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var task : PKTask? = null

        btn_a_download.setOnClickListener {
            task = Pikachu.with(this)
                .url("http://192.168.0.100:8080/download/a.mp4")
                .taskProcessListener(object : PKTaskProcessListener {
                    override fun onStart(downloadTask: PKDownloadTask) {
                        toast("任务A下载开始啦")
                    }

                    override fun onProcess(process: Long, length: Long) {
                        if (pb_a_process.max == 100) {
                            pb_a_process.max = length.toInt()
                        }
                        pb_a_process.progress = process.toInt()
                    }

                    override fun onComplete(downloadTask: PKDownloadTask) {
                        toast("下载完成 ${downloadTask.downloadResultFile?.path}")
                    }

                    override fun onFail(reason: String) {
                        toast("下载失败，呵呵")
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

        btn_b_download.setOnClickListener {
            Pikachu.with(this).url("http://192.168.0.100:8080/download/b.mp4")
                .taskProcessListener(object : PKTaskProcessListener {
                    override fun onStart(downloadTask: PKDownloadTask) {
                        toast("任务B下载开始啦")
                    }

                    override fun onProcess(process: Long, length: Long) {
                        if (pb_b_process.max == 100) {
                            pb_b_process.max = length.toInt()
                        }
                        pb_b_process.progress = process.toInt()
                    }

                    override fun onComplete(downloadTask: PKDownloadTask) {
                        toast("下载完成 ${downloadTask.downloadResultFile?.path}")
                    }

                    override fun onFail(reason: String) {
                        toast("下载失败，呵呵")
                    }
                })
                .download()
        }

        btn_c_download.setOnClickListener {
            Pikachu.with(this).url("http://192.168.0.100:8080/download/c.mp4")
                .taskProcessListener(object : PKTaskProcessListener {
                    override fun onStart(downloadTask: PKDownloadTask) {
                        toast("任务C下载开始啦")
                    }

                    override fun onProcess(process: Long, length: Long) {
                        if (pb_c_process.max == 100) {
                            pb_c_process.max = length.toInt()
                        }
                        pb_c_process.progress = process.toInt()
                    }

                    override fun onComplete(downloadTask: PKDownloadTask) {
                        toast("下载完成 ${downloadTask.downloadResultFile?.path}")
                    }

                    override fun onFail(reason: String) {
                        toast("下载失败，呵呵")
                    }
                })
                .download()
        }

    }
}