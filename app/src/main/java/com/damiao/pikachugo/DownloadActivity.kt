package com.damiao.pikachugo

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.damiao.pikachu.Pikachu
import com.damiao.pikachu.common.PKTaskProcessListener
import com.damiao.pikachugo.dialog.AddUrlDialog
import com.google.android.material.tabs.TabLayoutMediator
import com.uuzuche.lib_zxing.activity.CaptureActivity
import kotlinx.android.synthetic.main.activity_download.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.support.v4.intentFor

class DownloadActivity : AppCompatActivity() {

    private var taskIndex = 0

    private lateinit var taskListener: PKTaskProcessListener

    private val tabTitleMap = mapOf(
        0 to "正在下载", 1 to "已完成",
        2 to "已失败", 3 to "已取消"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)
        //请求存储卡读写权限
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            ), 0
        )
        //添加一个下载任务
        iv_main_add_new_task.setOnClickListener {
            /*Pikachu.with(this)
                .url(DEFAULT_TASK_URL[taskIndex])
                .download()
            taskIndex++*/
            val addUrlDialog = AddUrlDialog {
                Pikachu.with(this)
                    .url(it)
                    .download()
            }
            addUrlDialog.show(supportFragmentManager, "add_url")
        }
        vp_download_detail.adapter = DownloadTaskTabAdapter(
            fragmentManager = supportFragmentManager,
            lifecycle = lifecycle
        )
        vp_download_detail.offscreenPageLimit = 4
        val tabMediator = TabLayoutMediator(tl_download_type, vp_download_detail,
            TabLayoutMediator.TabConfigurationStrategy { tab, position ->
                tab.text = tabTitleMap[position]
            })
        tabMediator.attach()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}