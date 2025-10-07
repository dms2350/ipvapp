package com.dms2350.iptvapp.di

import com.dms2350.iptvapp.data.api.IPTVApi
import com.dms2350.iptvapp.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                println("IPTV: Enviando request a: ${request.url}")
                
                try {
                    val response = chain.proceed(request)
                    println("IPTV: Response code: ${response.code}")
                    println("IPTV: Response message: ${response.message}")
                    
                    if (response.isSuccessful) {
                        val body = response.peekBody(Long.MAX_VALUE).string()
                        println("IPTV: Response body preview: ${body.take(200)}...")
                    }
                    
                    response
                } catch (e: Exception) {
                    println("IPTV: Error en request: ${e.message}")
                    throw e
                }
            }
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideIPTVApi(retrofit: Retrofit): IPTVApi {
        return retrofit.create(IPTVApi::class.java)
    }
}
