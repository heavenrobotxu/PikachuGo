package com.damiao.pikachu.core

import com.damiao.pikachu.common.PKTask

interface PKTaskPersistent {

    fun writeTask(pkTask: PKTask)

    fun getTask(uuid: String): PKTask

    fun getTaskList(): List<PKTask>
}