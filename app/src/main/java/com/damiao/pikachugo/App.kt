package com.damiao.pikachugo

import android.Manifest
import android.app.Application
import androidx.core.app.ActivityCompat
import com.damiao.pikachu.Pikachu

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        Pikachu.init(this)
    }
}