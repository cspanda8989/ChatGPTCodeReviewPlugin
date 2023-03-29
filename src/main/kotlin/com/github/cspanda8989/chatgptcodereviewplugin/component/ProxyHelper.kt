package com.github.cspanda8989.chatgptcodereviewplugin.component

import com.theokanning.openai.service.OpenAiService
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy
import java.time.Duration


object ProxyHelper {
    private var httpClient: OkHttpClient? = null

    fun getOkHttpClient(apiKey: String): OkHttpClient {
        if (httpClient == null) {
            val host = System.getenv("LOCAL_PROXY_HOST")
            val port = System.getenv("LOCAL_PROXY_PORT")

            httpClient = if (host != null && port != null) {
                val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port.toInt()))
                OpenAiService.defaultClient(apiKey, Duration.ofMinutes(1))
                    .newBuilder()
                    .proxy(proxy)
                    .build()
            } else {
                OpenAiService.defaultClient(apiKey, Duration.ofMinutes(1))
                    .newBuilder()
                    .build()
            }
        }
        return httpClient!!
    }
}