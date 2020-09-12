package com.damiao.pikachu

import android.util.Log
import okhttp3.*
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy

class PrintEventListener(private val id: Int) : EventListener() {

    private var callStartTime = 0L

    private fun calculateTime(printName: String) {
        if (callStartTime != 0L) {
            val elapsedMills = System.currentTimeMillis() - callStartTime
            val a = String.format("Id %s %.3fs %s%n",id, elapsedMills / 1000F ,printName)
            Log.d("######", a)
        }
    }

    override fun callStart(call: Call) {
        callStartTime = System.currentTimeMillis()
        calculateTime("callStart")
    }

    override fun dnsStart(call: Call, domainName: String) {
        calculateTime("dnsStart")
    }


    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
        calculateTime("dnsEnd")
    }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        calculateTime("connectStart")
    }

    override fun secureConnectStart(call: Call) {
        calculateTime("secureConnectStart")
    }

    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        calculateTime("secureConnectEnd")
    }

    override fun connectEnd(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?
    ) {
        calculateTime("connectEnd")
    }

    override fun connectionAcquired(call: Call, connection: Connection) {
        calculateTime("connectionAcquired")
    }

    override fun requestHeadersStart(call: Call) {
        calculateTime("requestHeadersStart")
    }

    override fun requestHeadersEnd(call: Call, request: Request) {
        calculateTime("requestHeadersEnd")
    }

    override fun requestBodyStart(call: Call) {
        calculateTime("requestBodyStart")
    }

    override fun requestBodyEnd(call: Call, byteCount: Long) {
        calculateTime("requestBodyEnd")
    }

    override fun responseHeadersStart(call: Call) {
        calculateTime("responseHeadersStart")
    }

    override fun responseHeadersEnd(call: Call, response: Response) {
        calculateTime("responseHeadersEnd")
    }

    override fun responseBodyStart(call: Call) {
        calculateTime("responseBodyStart")
    }

    override fun responseBodyEnd(call: Call, byteCount: Long) {
        calculateTime("responseBodyEnd")
    }

    override fun connectionReleased(call: Call, connection: Connection) {
        calculateTime("connectionReleased")
    }

    override fun callEnd(call: Call) {
        calculateTime("callEnd")
    }

    //Failure
    override fun connectFailed(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?,
        ioe: IOException
    ) {
        calculateTime("connectFailed")
    }

    override fun requestFailed(call: Call, ioe: IOException) {
        calculateTime("requestFailed")
    }

    override fun responseFailed(call: Call, ioe: IOException) {
        calculateTime("responseFailed")
    }

    override fun callFailed(call: Call, ioe: IOException) {
        calculateTime("callFailed")
    }
}