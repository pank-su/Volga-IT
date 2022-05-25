package com.example.volga_it

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiInterface {
    // метод для получения всех символов
    @GET("stock/symbol?exchange=US&token=c900veqad3icdhuein80&securityType=Common%20Stock&currency=USD")
    suspend fun getSymbols(): Response<ResponseBody>

    // метод для получения профиля компании
    @GET("stock/profile2")
    suspend fun getCompanyProfile(@Query("symbol") symbol: String, @Query("token") token: String = "c900veqad3icdhuein80"): Response<ResponseBody>

    // Метод для получения цены акции
    @GET("quote")
    suspend fun getStockPrice(@Query("symbol") symbol: String, @Query("token") token: String = "c900veqad3icdhuein80"): Response<ResponseBody>
}