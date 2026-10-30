package com.example.data.tariff

import retrofit2.http.GET
import retrofit2.http.Query

interface TariffApiService {
    @GET("api/v1/HSCodes/Search")
    suspend fun searchHSCodes(
        @Query("phrase") phrase: String,
        @Query("pagination.offset") offset: Int = 0,
        @Query("pagination.limit") limit: Int = 50,
        @Query("lang") lang: String = "fa"
    ): TariffSearchResponse
}
