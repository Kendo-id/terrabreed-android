package com.terrabreed.app.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

object ApiClient {

    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String = ""

    fun getApi(context: Context): TerraBreedApi {
        val prefs   = context.getSharedPreferences("terrabreed_prefs", Context.MODE_PRIVATE)
        val ip      = prefs.getString("server_ip", "10.10.1.1") ?: "10.10.1.1"
        val port    = prefs.getString("server_port", "") ?: ""
        val useHttps = prefs.getBoolean("use_https", true)
        val scheme  = if (useHttps) "https" else "http"
        val baseUrl = if (port.isNotBlank()) "$scheme://$ip:$port/" else "$scheme://$ip/"

        if (retrofit == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl
            retrofit = buildRetrofit(baseUrl, useHttps)
        }
        return retrofit!!.create(TerraBreedApi::class.java)
    }

    fun buildRetrofit(baseUrl: String, trustAll: Boolean = true): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)

        // Trust all certs for local server (self-signed)
        if (trustAll) {
            try {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())
                clientBuilder
                    .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                    .hostnameVerifier { _, _ -> true }
            } catch (_: Exception) {}
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun invalidate() { retrofit = null }
}
