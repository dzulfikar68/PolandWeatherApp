package io.github.dzulfikar68.polandweatherapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherService {
    @GET("data/2.5/onecall")
    fun listWeather(
        @Query("exclude") exclude: String = "minutely,hourly,alerts",
        @Query("lat") lat: String?,
        @Query("lon") lon: String?,
        @Query("appid") appid: String = "164a6368bba755f68fe3107143be0e7f"
    ): Call<OpenWeatherResponse>
}