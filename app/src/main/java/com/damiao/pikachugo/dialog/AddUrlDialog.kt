package com.damiao.pikachugo.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.damiao.pikachugo.R
import com.uuzuche.lib_zxing.activity.CaptureActivity
import com.uuzuche.lib_zxing.activity.CodeUtils
import kotlinx.android.synthetic.main.dialog_add_url.view.*
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.support.v4.intentFor
import org.jetbrains.anko.support.v4.startActivity

class AddUrlDialog(val downloadFun: (String) -> Unit) : DialogFragment() {

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(activity!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = LayoutInflater.from(activity!!)
            .inflate(R.layout.dialog_add_url, container)
        view.btn_scan_code.setOnClickListener {
            startActivityForResult(intentFor<CaptureActivity>(), 101)
        }
        view.btn_download.setOnClickListener {
            val url = view.et_url_inout.text.toString()
            if (url.isNotBlank()) {
                downloadFun(url)
            }
            dismiss()
        }
        view.iv_close.setOnClickListener {
            dismiss()
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        val sh = resources.displayMetrics.heightPixels
        val sw = resources.displayMetrics.widthPixels
        //获取内部Dialog的Window
        val window = dialog!!.window!!
        //获取Window的LayoutParams
        val wlp = window.attributes
        wlp.width = (0.95 * sw).toInt()
        wlp.height = (0.47 * sh).toInt()
        //设置Window显示的位置
        wlp.gravity = Gravity.CENTER
        window.attributes = wlp
        //去除Dialog的背景，方便增加圆角或者阴影
        window.setBackgroundDrawable(ColorDrawable())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 101) {
            data?.extras?.let {
                if (it.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    view?.et_url_inout?.setText(it.getString(CodeUtils.RESULT_STRING))
                }
            }
        }
    }
}