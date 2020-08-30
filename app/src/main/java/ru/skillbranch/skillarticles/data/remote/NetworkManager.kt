package ru.skillbranch.skillarticles.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.skillbranch.skillarticles.AppConfig
import ru.skillbranch.skillarticles.data.JsonConverter.moshi
import ru.skillbranch.skillarticles.data.remote.interceptors.ErrorStatusInterceptor
import ru.skillbranch.skillarticles.data.remote.interceptors.NetworkStatusInterceptor
import ru.skillbranch.skillarticles.data.remote.interceptors.TokenAuthenticator
import java.util.concurrent.TimeUnit

object NetworkManager {
    val api: RestService by lazy {

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // client
        val client = OkHttpClient().newBuilder()
            // socket timeout (GET)
            .readTimeout(2, TimeUnit.SECONDS)
            // socket timeout (POST, PUT, etc)
            .writeTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(NetworkStatusInterceptor())
            .addInterceptor(ErrorStatusInterceptor())
            // intercept req/res for logging
            .addInterceptor(logging)
            // refresh token if response status code 401
            .authenticator(TokenAuthenticator())
            .build()

        // retrofit
        val retrofit = Retrofit.Builder()
            // set http client
            .client(client)
            // set json converter/parser
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(AppConfig.BASE_URL)
            .build()

        retrofit.create(RestService::class.java)
    }
}


