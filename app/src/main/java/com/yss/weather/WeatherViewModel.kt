package com.yss.weather

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class WeatherViewModel : ViewModel() {

    private val _weatherData = MutableStateFlow<WeatherResponse?>(null)
    val weatherData: StateFlow<WeatherResponse?> = _weatherData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // State untuk forecast
    private val _hourlyForecast = MutableStateFlow<List<HourlyForecast>>(emptyList())
    val hourlyForecast: StateFlow<List<HourlyForecast>> = _hourlyForecast.asStateFlow()

    private val _dailyForecast = MutableStateFlow<List<DailyForecast>>(emptyList())
    val dailyForecast: StateFlow<List<DailyForecast>> = _dailyForecast.asStateFlow()

    private val apiKey = "2d244dcd8527d37bb89055fa4e4bd444"

    private val weatherApi: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }

    interface WeatherApi {
        @GET("weather")
        suspend fun getWeather(
            @Query("q") city: String,
            @Query("appid") apiKey: String,
            @Query("units") units: String
        ): WeatherResponse

        @GET("weather")
        suspend fun getWeatherByLocation(
            @Query("lat") lat: Double,
            @Query("lon") lon: Double,
            @Query("appid") apiKey: String,
            @Query("units") units: String
        ): WeatherResponse

        @GET("forecast")
        suspend fun getWeatherForecast(
            @Query("q") city: String,
            @Query("appid") apiKey: String,
            @Query("units") units: String
        ): WeatherForecastResponse

        @GET("forecast")
        suspend fun getWeatherForecastByLocation(
            @Query("lat") lat: Double,
            @Query("lon") lon: Double,
            @Query("appid") apiKey: String,
            @Query("units") units: String
        ): WeatherForecastResponse
    }

    // Fetch weather berdasarkan lokasi default (Surakarta)
    fun fetchWeatherByLocation(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                println("DEBUG: Fetching weather by default location")

                // Gunakan lokasi default (Surakarta)
                val defaultLat = -7.5755
                val defaultLon = 110.8243

                // Fetch current weather by location
                val response = weatherApi.getWeatherByLocation(defaultLat, defaultLon, apiKey, "metric")

                if (response.name.isNotEmpty()) {
                    println("DEBUG: Success - ${response.name}, Temp: ${response.main.temp}")
                    _weatherData.value = response

                    // Fetch forecast data by location
                    fetchWeatherForecastByLocation(defaultLat, defaultLon)
                } else {
                    _errorMessage.value = "Gagal mendapatkan data lokasi"
                    _weatherData.value = null
                }

            } catch (e: HttpException) {
                val errorMessage = when (e.code()) {
                    401 -> "Invalid API Key"
                    404 -> "Lokasi tidak ditemukan"
                    429 -> "Terlalu banyak requests. Silakan coba lagi nanti."
                    else -> "HTTP Error: ${e.code()}"
                }
                _errorMessage.value = errorMessage
                _weatherData.value = null
                println("DEBUG: HTTP Error - $errorMessage")

            } catch (e: IOException) {
                _errorMessage.value = "Error jaringan: Periksa koneksi internet Anda"
                _weatherData.value = null
                println("DEBUG: Network Error - ${e.message}")

            } catch (e: Exception) {
                _errorMessage.value = "Error tak terduga: ${e.message}"
                _weatherData.value = null
                println("DEBUG: General Error - ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Fetch forecast berdasarkan lokasi
    private suspend fun fetchWeatherForecastByLocation(lat: Double, lon: Double) {
        try {
            val forecastResponse = weatherApi.getWeatherForecastByLocation(lat, lon, apiKey, "metric")

            // Process HOURLY forecast (5 jam ke depan)
            val hourlyList = mutableListOf<HourlyForecast>()
            forecastResponse.list.take(5).forEachIndexed { index, item ->
                hourlyList.add(HourlyForecast(
                    time = if (index == 0) "Sekarang" else formatTime(item.dt),
                    temperature = item.main.temp.toInt(),
                    icon = getWeatherIcon(item.weather.firstOrNull()?.icon ?: "")
                ))
            }
            _hourlyForecast.value = hourlyList

            // Process DAILY forecast (7 hari)
            val dailyList = mutableListOf<DailyForecast>()
            val groupedByDate = forecastResponse.list.groupBy {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.dt * 1000))
            }

            groupedByDate.entries.take(7).forEachIndexed { index, entry ->
                val date = entry.key
                val dayItems = entry.value

                val maxTemp = dayItems.maxOf { it.main.temp_max }
                val minTemp = dayItems.minOf { it.main.temp_min }
                val weatherCondition = dayItems.first().weather.firstOrNull()

                dailyList.add(DailyForecast(
                    day = if (index == 0) "Hari ini" else formatDay(date),
                    weather = weatherCondition?.description ?: "Tidak diketahui",
                    icon = getWeatherIcon(weatherCondition?.icon ?: ""),
                    highTemp = maxTemp.toInt(),
                    lowTemp = minTemp.toInt()
                ))
            }
            _dailyForecast.value = dailyList

            println("DEBUG: Location forecast loaded - ${hourlyList.size} hours, ${dailyList.size} days")

        } catch (e: Exception) {
            println("DEBUG: Location forecast error - ${e.message}")
            // Fallback ke data berdasarkan current weather
            setupFallbackForecast()
        }
    }

    // Set error message
    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }

    // Fetch weather berdasarkan nama kota
    fun fetchWeather(city: String) {
        if (city.isBlank()) {
            _errorMessage.value = "Nama kota tidak boleh kosong"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                println("DEBUG: Fetching weather for city: $city")

                // Fetch current weather
                val response = weatherApi.getWeather(city, apiKey, "metric")

                if (response.name.isNotEmpty()) {
                    println("DEBUG: Success - ${response.name}, Temp: ${response.main.temp}")
                    _weatherData.value = response

                    // Fetch forecast data
                    fetchWeatherForecast(city)
                } else {
                    _errorMessage.value = "Kota '$city' tidak ditemukan"
                    _weatherData.value = null
                }

            } catch (e: HttpException) {
                val errorMessage = when (e.code()) {
                    401 -> "Invalid API Key"
                    404 -> "Kota '$city' tidak ditemukan"
                    429 -> "Terlalu banyak requests. Silakan coba lagi nanti."
                    else -> "HTTP Error: ${e.code()}"
                }
                _errorMessage.value = errorMessage
                _weatherData.value = null
                println("DEBUG: HTTP Error - $errorMessage")

            } catch (e: IOException) {
                _errorMessage.value = "Error jaringan: Periksa koneksi internet Anda"
                _weatherData.value = null
                println("DEBUG: Network Error - ${e.message}")

            } catch (e: Exception) {
                _errorMessage.value = "Error tak terduga: ${e.message}"
                _weatherData.value = null
                println("DEBUG: General Error - ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Fetch forecast berdasarkan nama kota
    private suspend fun fetchWeatherForecast(city: String) {
        try {
            val forecastResponse = weatherApi.getWeatherForecast(city, apiKey, "metric")

            // Process HOURLY forecast (5 jam ke depan)
            val hourlyList = mutableListOf<HourlyForecast>()
            forecastResponse.list.take(5).forEachIndexed { index, item ->
                hourlyList.add(HourlyForecast(
                    time = if (index == 0) "Sekarang" else formatTime(item.dt),
                    temperature = item.main.temp.toInt(),
                    icon = getWeatherIcon(item.weather.firstOrNull()?.icon ?: "")
                ))
            }
            _hourlyForecast.value = hourlyList

            // Process DAILY forecast (7 hari)
            val dailyList = mutableListOf<DailyForecast>()
            val groupedByDate = forecastResponse.list.groupBy {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.dt * 1000))
            }

            groupedByDate.entries.take(7).forEachIndexed { index, entry ->
                val date = entry.key
                val dayItems = entry.value

                val maxTemp = dayItems.maxOf { it.main.temp_max }
                val minTemp = dayItems.minOf { it.main.temp_min }
                val weatherCondition = dayItems.first().weather.firstOrNull()

                dailyList.add(DailyForecast(
                    day = if (index == 0) "Hari ini" else formatDay(date),
                    weather = weatherCondition?.description ?: "Tidak diketahui",
                    icon = getWeatherIcon(weatherCondition?.icon ?: ""),
                    highTemp = maxTemp.toInt(),
                    lowTemp = minTemp.toInt()
                ))
            }
            _dailyForecast.value = dailyList

            println("DEBUG: Forecast loaded - ${hourlyList.size} hours, ${dailyList.size} days")

        } catch (e: Exception) {
            println("DEBUG: Forecast error - ${e.message}")
            // Fallback ke data berdasarkan current weather
            setupFallbackForecast()
        }
    }

    // Helper functions
    private fun formatTime(timestamp: Long): String {
        val date = Date(timestamp * 1000)
        val formatter = SimpleDateFormat("HH.mm", Locale.getDefault())
        return formatter.format(date)
    }

    private fun formatDay(dateString: String): String {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = formatter.parse(dateString)
            val calendar = Calendar.getInstance()
            calendar.time = date

            when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "Minggu"
                Calendar.MONDAY -> "Senin"
                Calendar.TUESDAY -> "Selasa"
                Calendar.WEDNESDAY -> "Rabu"
                Calendar.THURSDAY -> "Kamis"
                Calendar.FRIDAY -> "Jumat"
                Calendar.SATURDAY -> "Sabtu"
                else -> "Hari ini"
            }
        } catch (e: Exception) {
            "Hari ini"
        }
    }

    private fun getWeatherIcon(iconCode: String): String {
        return when (iconCode) {
            "01d", "01n" -> "☀️"
            "02d", "02n" -> "⛅"
            "03d", "03n", "04d", "04n" -> "☁️"
            "09d", "09n", "10d", "10n" -> "🌧️"
            "11d", "11n" -> "⛈️"
            "13d", "13n" -> "❄️"
            "50d", "50n" -> "🌫️"
            else -> "⛅"
        }
    }

    private fun setupFallbackForecast() {
        val currentTemp = _weatherData.value?.main?.temp?.toInt() ?: 29
        val currentWeather = _weatherData.value?.weather?.firstOrNull()?.description ?: "Berawan"

        // Setup hourly forecast fallback
        val hourlyList = listOf(
            HourlyForecast("Sekarang", currentTemp, getWeatherIconFromDescription(currentWeather)),
            HourlyForecast("13.00", currentTemp + 1, "🌦️"),
            HourlyForecast("14.00", currentTemp + 1, "🌦️"),
            HourlyForecast("15.00", currentTemp + 1, "☀️"),
            HourlyForecast("16.00", currentTemp, "☀️")
        )
        _hourlyForecast.value = hourlyList

        // Setup daily forecast fallback
        val dailyList = listOf(
            DailyForecast("Hari ini", currentWeather, getWeatherIconFromDescription(currentWeather), currentTemp + 2, currentTemp - 6),
            DailyForecast("Besok", "Cerah", "☀️", currentTemp + 3, currentTemp - 5),
            DailyForecast("Rabu", "Hujan ringan", "🌦️", currentTemp + 1, currentTemp - 4),
            DailyForecast("Kamis", "Berawan", "☁️", currentTemp + 2, currentTemp - 5),
            DailyForecast("Jumat", "Cerah", "☀️", currentTemp + 3, currentTemp - 4),
            DailyForecast("Sabtu", "Hujan", "🌧️", currentTemp, currentTemp - 6),
            DailyForecast("Minggu", "Cerah", "☀️", currentTemp + 2, currentTemp - 5)
        )
        _dailyForecast.value = dailyList

        println("DEBUG: Using fallback forecast")
    }

    private fun getWeatherIconFromDescription(description: String): String {
        return when {
            description.contains("clear", ignoreCase = true) -> "☀️"
            description.contains("rain", ignoreCase = true) -> "🌧️"
            description.contains("cloud", ignoreCase = true) -> "☁️"
            description.contains("drizzle", ignoreCase = true) -> "🌦️"
            description.contains("thunder", ignoreCase = true) -> "⛈️"
            description.contains("snow", ignoreCase = true) -> "❄️"
            description.contains("mist", ignoreCase = true) -> "🌫️"
            else -> "⛅"
        }
    }
}