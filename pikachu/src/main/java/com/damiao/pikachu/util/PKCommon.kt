package com.damiao.pikachu.util

import okhttp3.internal.closeQuietly
import okio.ByteString.Companion.encodeUtf8
import java.io.Closeable
import java.math.BigDecimal
import java.util.*

const val KB = 1024
const val MB = 1024 * 1024
const val GB = 1024 * 1024 * 1024

fun uuid(): String {
    return UUID.randomUUID().toString()
}

fun String.sha1() : String{
    return this.encodeUtf8().sha1().hex()
}

fun getDownloadFileSizeDescription(contentLength: Long) : String{
    return when {
        contentLength <= KB -> "${contentLength}B"
        contentLength <= MB -> "${contentLength / KB}KB"
        contentLength <= GB -> "${contentLength / MB}MB"
        contentLength >= GB -> "${(contentLength.toDouble() / GB).toBigDecimal()
            .setScale(2, BigDecimal.ROUND_DOWN)}GB"
        else -> "unknown"
    }
}

fun closeAll(vararg close: Closeable?) {
    for (closeable in close) {
        closeable?.closeQuietly()
    }
}