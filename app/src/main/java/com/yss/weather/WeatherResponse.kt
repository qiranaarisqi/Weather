package com.yss.weather

// ✅ DATA CLASS UNTUK CURRENT WEATHER (API OpenWeatherMap)
data class WeatherResponse(
    val name: String = "",
    val main: Main = Main(),
    val weather: List<WeatherInfo> = emptyList(),
    val coord: Coord = Coord() // Penting untuk lokasi
)

data class Main(
    val temp: Double = 0.0,
    val humidity: Int = 0,
    val feels_like: Double = 0.0 // Untuk "Suhu terasa seperti"
)

data class WeatherInfo(
    val main: String = "",
    val description: String = "",
    val icon: String = ""
)

data class Coord(
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

// ✅ DATA CLASS UNTUK FORECAST RESPONSE
data class WeatherForecastResponse(
    val list: List<ForecastItem> = emptyList()
)

data class ForecastItem(
    val dt: Long = 0,
    val main: ForecastMain = ForecastMain(),
    val weather: List<ForecastWeather> = emptyList()
)

data class ForecastMain(
    val temp: Double = 0.0,
    val temp_min: Double = 0.0,
    val temp_max: Double = 0.0
)

data class ForecastWeather(
    val description: String = "",
    val icon: String = ""
)

// ✅ DATA CLASS UNTUK FORECAST DISPLAY (UI) - TAMBAHKAN FEELSLIKE
data class HourlyForecast(
    val time: String = "",
    val temperature: Int = 0,
    val icon: String = "",
    val feelsLike: Int = 0 // Tambahkan untuk konsistensi dengan main data
)

data class DailyForecast(
    val day: String = "",
    val weather: String = "",
    val icon: String = "",
    val highTemp: Int = 0,
    val lowTemp: Int = 0,
    val feelsLike: Int = 0 // Tambahkan untuk "terasa seperti"
)