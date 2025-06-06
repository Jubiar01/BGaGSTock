package com.example.growagarden.network

import com.example.growagarden.data.StockResponse
import com.example.growagarden.data.WeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.HeaderMap
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

interface GardenApiService {

    @GET("/api/ws/stocks.getAll")
    suspend fun getStockData(
        @Query("batch") batch: Int = 1,
        @Query("input") input: String = "%7B%220%22%3A%7B%22json%22%3Anull%2C%22meta%22%3A%7B%22values%22%3A%5B%22undefined%22%5D%7D%7D%7D",
        @Query("_") timestamp: Long,
        @Query("r") randomParam: String,
        @HeaderMap headers: Map<String, String>
    ): Response<List<StockResponse>>

    @GET("/api/v1/weather/gag")
    suspend fun getWeatherData(
        @Query("_") timestamp: Long,
        @Query("r") randomParam: String,
        @HeaderMap headers: Map<String, String>
    ): Response<WeatherResponse>
}

object ApiClient {
    private const val BASE_URL = "https://growagarden.gg"

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(createOkHttpClient())
            .build()
    }

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                response
            }
            .build()
    }

    val apiService: GardenApiService by lazy {
        retrofit.create(GardenApiService::class.java)
    }
}