package com.example.scms.data.network
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // ⚠️ UPDATE THIS IP if you change Wi-Fi networks (run: ipconfig getifaddr en0 in terminal)
    private const val BASE_URL = "http://10.35.109.89:3000/"

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
        UserSession.currentUser?.let { user ->
            requestBuilder.addHeader("X-User-Id", user.id.toString())
        }
        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .cache(okhttp3.Cache(
            directory = java.io.File(UserSession.context?.cacheDir, "http_cache"),
            maxSize = 10L * 1024L * 1024L // 10 MB
        ))
        .addInterceptor(authInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
