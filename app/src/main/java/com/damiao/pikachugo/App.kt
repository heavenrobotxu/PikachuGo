package com.damiao.pikachugo

import android.app.Application
import com.damiao.pikachu.Pikachu

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        Pikachu.init(this)
    }
}