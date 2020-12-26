package ru.skillbranch.skillarticles.di.modules

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.skillbranch.skillarticles.AppConfig
import ru.skillbranch.skillarticles.data.local.PrefManager
import ru.skillbranch.skillarticles.data.remote.NetworkMonitor
import ru.skillbranch.skillarticles.data.remote.RestService
import ru.skillbranch.skillarticles.data.remote.adapters.DateAdapter
import ru.skillbranch.skillarticles.data.remote.interceptors.ErrorStatusInterceptor
import ru.skillbranch.skillarticles.data.remote.interceptors.NetworkStatusInterceptor
import ru.skillbranch.skillarticles.data.remote.interceptors.TokenAuthenticator
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
object NetworkModule {
    @Provides
    @Singleton
    fun provideLoggingInterceptor() = HttpLoggingInterceptor()
        .apply { level = HttpLoggingInterceptor.Level.BODY }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        // convert long timestamp to date
        .add(DateAdapter())
        // convert json to class by reflection
        .add(KotlinJsonAdapterFactory()) // lecture 11, time code 01:58:07
        .build()

    @Provides
    @Singleton
    fun provideTokenAuthenticator(prefs: PrefManager, lazyApi: Lazy<RestService>) =
        TokenAuthenticator(prefs, lazyApi) // Класс Lazy из даггера, а не котлина

    @Provides
    @Singleton
    fun provideStatusInterceptor(monitor: NetworkMonitor) =
        NetworkStatusInterceptor(monitor)

    @Provides
    @Singleton
    fun provideErrorInterceptor(moshi: Moshi) = ErrorStatusInterceptor(moshi)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authenticator: TokenAuthenticator,
        logging: HttpLoggingInterceptor,
        statusInterceptor: NetworkStatusInterceptor,
        errorInterceptor: ErrorStatusInterceptor
    ) = OkHttpClient().newBuilder()
        // socket timeout (GET)
        .readTimeout(2, TimeUnit.SECONDS)
        // socket timeout (POST, PUT, etc)
        .writeTimeout(5, TimeUnit.SECONDS)
        .addInterceptor(statusInterceptor)
        .addInterceptor(errorInterceptor)
        // intercept req/res for logging
        .addInterceptor(logging)
        // refresh token if response status code -> 401
        .authenticator(authenticator)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        moshi: Moshi
    ): Retrofit = Retrofit.Builder()
        // set http client
        .client(client)
        // set json converter/parser
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .baseUrl(AppConfig.BASE_URL)
        .build()

    // CREATE API SERVICE INTERFACE
    @Provides
    @Singleton
    fun provideRestService(retrofit: Retrofit): RestService =
        retrofit.create(RestService::class.java)
}