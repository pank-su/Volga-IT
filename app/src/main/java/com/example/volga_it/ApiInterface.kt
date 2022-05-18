package com.example.volga_it

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET

interface ApiInterface {
    @GET("stock/symbol?exchange=US&token=c900veqad3icdhuein80")
    suspend fun getSymbols(): Response<ResponseBody>
}