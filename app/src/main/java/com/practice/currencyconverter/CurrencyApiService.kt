package com.practice.currencyconverter

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface CurrencyApiService {
    @GET("v6/{apiKey}/latest/{baseCurrency}")
    suspend fun getExchangeRates(
        @Path("apiKey") apiKey: String,
        @Path("baseCurrency") baseCurrency: String
    ): Response<ExchangeRateResponse>
}