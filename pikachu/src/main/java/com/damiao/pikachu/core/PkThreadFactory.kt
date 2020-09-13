package com.damiao.pikachu.core

import java.util.concurrent.ThreadFactory

class PkThreadFactory : ThreadFactory {

    private var count = 0

    override fun newThread(r: Runnable): Thread {
        val thread = Thread(r, "Pikachu-Task-Thread ${count++}")
        thread.isDaemon = false
        return thread
    }

}