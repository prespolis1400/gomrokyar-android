package com.example.data

import com.example.data.tariff.TariffApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    private const val BASE_URL = "https://gomrokyar.heidar.workers.dev/"
    private const val TARIFF_BASE_URL = "https://api.tarkhiskala.info/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val currencyApi: CurrencyApi by lazy {
        retrofit.create(CurrencyApi::class.java)
    }

    private val tariffRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(TARIFF_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val tariffApi: TariffApiService by lazy {
        tariffRetrofit.create(TariffApiService::class.java)
    }
}
