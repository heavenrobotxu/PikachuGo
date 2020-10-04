package com.damiao.pikachugo

import android.app.Application
import com.damiao.pikachu.Pikachu
import com.uuzuche.lib_zxing.activity.ZXingLibrary

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        Pikachu.init(this)

        ZXingLibrary.initDisplayOpinion(this)
    }
}