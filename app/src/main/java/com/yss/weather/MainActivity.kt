package com.yss.weather

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yss.weather.ui.theme.*
import java.util.*
import com.yss.weather.R
import androidx.compose.ui.platform.LocalConfiguration
import java.text.SimpleDateFormat

// ------------------------------------------------------------
//                     MAIN ACTIVITY
// ------------------------------------------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherTheme {
                WeatherRouteWithLocation()
            }
        }
    }
}

// ------------------------------------------------------------
//                           ROUTE WITH LOCATION
// ------------------------------------------------------------
@Composable
fun WeatherRouteWithLocation(
    viewModel: WeatherViewModel = viewModel()
) {
    val context = LocalContext.current
    var city by remember { mutableStateOf("") }
    val weatherData by viewModel.weatherData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val hourlyForecast by viewModel.hourlyForecast.collectAsState()
    val dailyForecast by viewModel.dailyForecast.collectAsState()

    // Permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    // Auto-fetch location on app start if permission granted
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission && weatherData == null) {
            viewModel.fetchWeatherByLocation(context)
        }
    }

    WeatherRouteContent(
        city = city,
        onCityChange = { city = it },
        onCheckWeather = {
            if (city.isNotBlank()) {
                viewModel.fetchWeather(city)
            }
        },
        onGetCurrentLocation = {
            if (hasLocationPermission) {
                viewModel.fetchWeatherByLocation(context)
            } else {
                viewModel.setErrorMessage(
                    context.getString(R.string.enable_location_permission)
                )
            }
        },
        weatherData = weatherData,
        hourlyForecast = hourlyForecast,
        dailyForecast = dailyForecast,
        isLoading = isLoading,
        errorMessage = errorMessage,
        hasLocationPermission = hasLocationPermission
    )
}

// ------------------------------------------------------------
//                       UI CONTENT
// ------------------------------------------------------------
@Composable
fun WeatherRouteContent(
    city: String,
    onCityChange: (String) -> Unit,
    onCheckWeather: () -> Unit,
    onGetCurrentLocation: (() -> Unit)? = null,
    weatherData: WeatherResponse?,
    hourlyForecast: List<HourlyForecast>,
    dailyForecast: List<DailyForecast>,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    hasLocationPermission: Boolean = false
) {
    val scrollState = rememberScrollState()

    // Deteksi mode malam/siang berdasarkan waktu device
    val isNightMode = isCurrentlyNightTime()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painterResource(id = R.drawable.weatherbkg),
                contentScale = ContentScale.FillBounds
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            Spacer(modifier = Modifier.height(40.dp))

            // Search Section dengan tombol lokasi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = onCityChange,
                    label = { Text(stringResource(R.string.city_hint)) },
                    placeholder = { Text(stringResource(R.string.city_placeholder)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(30.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = BlueJC,
                        unfocusedIndicatorColor = BlueJC,
                        focusedLabelColor = DarkBlueJC
                    )
                )

                // Tombol lokasi saat ini
                if (onGetCurrentLocation != null) {
                    IconButton(
                        onClick = onGetCurrentLocation,
                        modifier = Modifier
                            .size(56.dp)
                            .background(BlueJC, shape = RoundedCornerShape(16.dp)),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = stringResource(R.string.location_icon_desc),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCheckWeather,
                colors = ButtonDefaults.buttonColors(BlueJC),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && city.isNotBlank()
            ) {
                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.loading))
                    }
                } else {
                    Text(stringResource(R.string.check_weather_button))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tampilkan error message jika ada
            errorMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(Color.Red.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(R.string.error_icon_desc),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Info permission lokasi
            if (!hasLocationPermission && onGetCurrentLocation != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(Color.Yellow.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.info_icon_desc),
                            tint = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.location_permission_required),
                            color = Color.DarkGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // BUBBLE PRAKIRAAN CUACA - GUNAKAN DATA REAL DARI VIEWMODEL
            if (weatherData != null) {

                // HEADER CUACA SAAT INI
                WeatherHeaderSection(weatherData)

                Spacer(modifier = Modifier.height(16.dp))

                // BUBBLE SUHU PER JAM - TANPA BUBBLE CARD, LANGSUNG SCROLLABLE
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Judul section
                    Text(
                        text = stringResource(R.string.title_today_temperature),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Horizontal scroll untuk bubble-bubble per jam
                    HourlyTemperatureScrollSection(
                        weatherData = weatherData,
                        hourlyForecast = hourlyForecast,
                        isNightMode = isNightMode
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BUBBLE 2: SUHU TERASA SEPERTI
                WeatherBubbleCard(
                    title = stringResource(R.string.title_feels_like),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val feelsLike = weatherData.main.feels_like
                    val actualTemp = weatherData.main.temp
                    val humidity = weatherData.main.humidity
                    val feelsLikeHotter = stringResource(R.string.feels_like_hotter)
                    val feelsLikeColder = stringResource(R.string.feels_like_colder)

                    Text(
                        text = stringResource(
                            R.string.feels_like_message,
                            feelsLike.toInt(),
                            humidity,
                            if (feelsLike > actualTemp) feelsLikeHotter else feelsLikeColder
                        ),
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BUBBLE 3: PRAKIRAAN 7 HARI - YANG SUDAH DIPERBAIKI
                WeatherBubbleCard(
                    title = stringResource(R.string.seven_day_forecast),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // GUNAKAN DATA REAL, JIKA KOSONG GUNAKAN FALLBACK
                    val displayDailyForecast = if (dailyForecast.isNotEmpty()) {
                        // Jika ada data real dari API, konversi deskripsinya ke bahasa yang sesuai
                        dailyForecast.map { day ->
                            val localizedWeather = getLocalizedWeatherDescription(day.weather)
                            val weatherMain = getMainWeatherFromDescription(day.weather)
                            val weatherIcon = getWeatherIconFromMain(weatherMain)
                            DailyForecast(day.day, localizedWeather, weatherIcon, day.highTemp, day.lowTemp)
                        }
                    } else {
                        // Fallback data berdasarkan current weather - SEMUA GUNAKAN STRING RESOURCE
                        val currentTemp = weatherData.main.temp.toInt()
                        val weatherMain = weatherData.weather.firstOrNull()?.main ?: "Clouds"
                        val currentWeatherText = getLocalizedWeatherDescription(weatherMain)
                        val currentWeatherIcon = getWeatherIconFromMain(weatherMain)

                        listOf(
                            DailyForecast(
                                stringResource(R.string.today),
                                currentWeatherText,
                                currentWeatherIcon,
                                currentTemp + 2,
                                currentTemp - 6
                            ),
                            DailyForecast(
                                stringResource(R.string.tomorrow),
                                stringResource(R.string.weather_clear),
                                "☀️",
                                currentTemp + 3,
                                currentTemp - 5
                            ),
                            DailyForecast(
                                stringResource(R.string.wednesday),
                                stringResource(R.string.weather_drizzle),
                                "🌦️",
                                currentTemp + 1,
                                currentTemp - 4
                            ),
                            DailyForecast(
                                stringResource(R.string.thursday),
                                stringResource(R.string.weather_clouds),
                                "☁️",
                                currentTemp + 2,
                                currentTemp - 5
                            ),
                            DailyForecast(
                                stringResource(R.string.friday),
                                stringResource(R.string.weather_clear),
                                "☀️",
                                currentTemp + 3,
                                currentTemp - 4
                            ),
                            DailyForecast(
                                stringResource(R.string.saturday),
                                stringResource(R.string.weather_rain),
                                "🌧️",
                                currentTemp,
                                currentTemp - 6
                            ),
                            DailyForecast(
                                stringResource(R.string.sunday),
                                stringResource(R.string.weather_clear),
                                "☀️",
                                currentTemp + 2,
                                currentTemp - 5
                            )
                        )
                    }
                    DailyForecastSection(displayDailyForecast)
                }
            }

            // TAMPILAN AWAL SAAT BELUM ADA DATA
            if (weatherData == null && !isLoading && errorMessage == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = stringResource(R.string.no_data_icon_desc),
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (city.isEmpty()) {
                            stringResource(R.string.no_data_message)
                        } else {
                            stringResource(R.string.ready_to_search, city)
                        },
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Tombol gunakan lokasi saya
                    if (onGetCurrentLocation != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onGetCurrentLocation,
                            colors = ButtonDefaults.buttonColors(Color.White),
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            Text(
                                text = stringResource(R.string.use_my_location),
                                color = BlueJC,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // TAMBAHKAN EXTRA SPACER DI BAWAH UNTUK SCROLL YANG LEBIH BAIK
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ------------------------------------------------------------
//                   FUNGSI BARU UNTUK SUHU PER JAM SCROLLABLE
// ------------------------------------------------------------

// Fungsi untuk mendeteksi apakah sekarang malam
private fun isCurrentlyNightTime(): Boolean {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    return hour >= 18 || hour < 6 // Malam dari jam 18:00 sampai 06:00
}

// Fungsi untuk generate forecast per jam
private fun generateHourlyForecast(weatherData: WeatherResponse, isNightMode: Boolean): List<HourlyForecast> {
    val currentTemp = weatherData.main.temp.toInt()
    val currentTime = Calendar.getInstance()

    return listOf(
        HourlyForecast(getFormattedTime(currentTime, 0), currentTemp, getTimeAppropriateIcon(isNightMode, 0)),
        HourlyForecast(getFormattedTime(currentTime, 1), currentTemp + 1, getTimeAppropriateIcon(isNightMode, 1)),
        HourlyForecast(getFormattedTime(currentTime, 2), currentTemp, getTimeAppropriateIcon(isNightMode, 2)),
        HourlyForecast(getFormattedTime(currentTime, 3), currentTemp - 1, getTimeAppropriateIcon(isNightMode, 3)),
        HourlyForecast(getFormattedTime(currentTime, 4), currentTemp - 2, getTimeAppropriateIcon(isNightMode, 4)),
        HourlyForecast(getFormattedTime(currentTime, 5), currentTemp - 1, getTimeAppropriateIcon(isNightMode, 5)),
        HourlyForecast(getFormattedTime(currentTime, 6), currentTemp, getTimeAppropriateIcon(isNightMode, 6)),
        HourlyForecast(getFormattedTime(currentTime, 7), currentTemp + 1, getTimeAppropriateIcon(isNightMode, 7)),
        HourlyForecast(getFormattedTime(currentTime, 8), currentTemp + 2, getTimeAppropriateIcon(isNightMode, 8)),
        HourlyForecast(getFormattedTime(currentTime, 9), currentTemp + 1, getTimeAppropriateIcon(isNightMode, 9)),
        HourlyForecast(getFormattedTime(currentTime, 10), currentTemp, getTimeAppropriateIcon(isNightMode, 10)),
        HourlyForecast(getFormattedTime(currentTime, 11), currentTemp - 1, getTimeAppropriateIcon(isNightMode, 11)),
        HourlyForecast(getFormattedTime(currentTime, 12), currentTemp - 2, getTimeAppropriateIcon(isNightMode, 12))
    )
}

// Fungsi untuk mendapatkan waktu yang diformat
private fun getFormattedTime(currentTime: Calendar, hoursToAdd: Int): String {
    val newTime = currentTime.clone() as Calendar
    newTime.add(Calendar.HOUR, hoursToAdd)
    val hour = newTime.get(Calendar.HOUR_OF_DAY)
    return if (hour == 0) "12 AM"
    else if (hour < 12) "$hour AM"
    else if (hour == 12) "12 PM"
    else "${hour - 12} PM"
}

// Fungsi untuk mendapatkan icon yang sesuai dengan waktu
private fun getTimeAppropriateIcon(isNightMode: Boolean, hourOffset: Int): String {
    val iconsDay = listOf("☀️", "⛅", "🌤️", "🌥️", "☁️", "🌦️", "🌧️", "⛈️", "🌩️", "🌨️", "❄️", "🌫️", "🌬️")
    val iconsNight = listOf("🌙", "☁️", "🌧️", "⛈️", "🌩️", "🌨️", "❄️", "🌫️", "🌬️", "🌕", "🌖", "🌗", "🌘")

    return if (isNightMode) {
        iconsNight[hourOffset % iconsNight.size]
    } else {
        iconsDay[hourOffset % iconsDay.size]
    }
}

// Section untuk suhu per jam dengan horizontal scroll
@Composable
fun HourlyTemperatureScrollSection(
    weatherData: WeatherResponse,
    hourlyForecast: List<HourlyForecast>,
    isNightMode: Boolean
) {
    // GUNAKAN DATA REAL, JIKA KOSONG GUNAKAN FALLBACK
    val displayHourlyForecast = if (hourlyForecast.isNotEmpty()) {
        hourlyForecast.take(12) // Ambil 12 jam ke depan untuk scroll
    } else {
        // Fallback data berdasarkan current weather dan waktu
        generateHourlyForecast(weatherData, isNightMode)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        displayHourlyForecast.forEach { hour ->
            HourlyTemperatureBubble(hour = hour, isNightMode = isNightMode)
        }
    }
}

// Bubble individual untuk setiap jam
@Composable
fun HourlyTemperatureBubble(hour: HourlyForecast, isNightMode: Boolean) {
    val bubbleColor = if (isNightMode) {
        Color(0xFF2D3748) // Warna gelap untuk malam
    } else {
        Color.White // Warna putih untuk siang
    }

    val textColor = if (isNightMode) Color.White else DarkBlueJC

    Card(
        modifier = Modifier
            .width(80.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = bubbleColor),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Waktu
            Text(
                text = hour.time,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Icon cuaca
            Text(
                text = hour.icon,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Temperatur
            Text(
                text = "${hour.temperature}${stringResource(R.string.unit_temperature)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

// ------------------------------------------------------------
//                   HEADER SECTION - YANG SUDAH DIPERBAIKI
// ------------------------------------------------------------
@Composable
fun WeatherHeaderSection(weatherData: WeatherResponse) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Kota dan Temperatur utama
        Text(
            text = weatherData.name,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${String.format(Locale.getDefault(), "%.0f", weatherData.main.temp)}${stringResource(R.string.unit_temperature)}",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Deskripsi cuaca - GUNAKAN YANG SUDAH DILOKALISASI
        val weatherDescription = weatherData.weather.firstOrNull()?.description ?: "unknown"
        val localizedDescription = getLocalizedWeatherDescription(weatherDescription)
        Text(
            text = localizedDescription.replaceFirstChar { it.uppercase() },
            color = Color.White,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Info tambahan dalam row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WeatherHeaderInfoItem(
                label = stringResource(R.string.monday),
                value = "${String.format(Locale.getDefault(), "%.0f", weatherData.main.temp + 3)}${stringResource(R.string.unit_temperature)} / ${String.format(Locale.getDefault(), "%.0f", weatherData.main.temp - 5)}${stringResource(R.string.unit_temperature)}",
                color = Color.White
            )

            WeatherHeaderInfoItem(
                label = stringResource(R.string.air_quality),
                value = "${weatherData.main.humidity}${stringResource(R.string.unit_percentage)} - ${getAirQualityText(weatherData.main.humidity)}",
                color = Color.White
            )
        }
    }
}

// Helper function untuk kualitas udara (diperbaiki)
private fun getAirQualityText(humidity: Int): String {
    return when {
        humidity <= 50 -> "Baik"
        humidity <= 100 -> "Sedang"
        else -> "Tidak Sehat"
    }
}

@Composable
fun WeatherHeaderInfoItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ------------------------------------------------------------
//                   DAILY FORECAST SECTION
// ------------------------------------------------------------
@Composable
fun DailyForecastSection(forecast: List<DailyForecast>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        forecast.forEach { day ->
            DailyForecastItem(day)
        }
    }
}

@Composable
fun DailyForecastItem(day: DailyForecast) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hari
        Text(
            text = day.day,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = DarkBlueJC,
            modifier = Modifier.width(80.dp)
        )

        // Icon dan deskripsi cuaca
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = day.icon,
                fontSize = 16.sp,
                modifier = Modifier.width(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = day.weather,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        // Suhu
        Text(
            text = "${day.highTemp}${stringResource(R.string.unit_temperature)} / ${day.lowTemp}${stringResource(R.string.unit_temperature)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BlueJC
        )
    }
}

// ------------------------------------------------------------
//                     WEATHER BUBBLE CARD
// ------------------------------------------------------------
@Composable
fun WeatherBubbleCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkBlueJC,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

// ------------------------------------------------------------
//                   FUNGSI LOKALISASI CUACA - YANG SUDAH DIPERBAIKI
// ------------------------------------------------------------

// Fungsi untuk mengkonversi deskripsi cuaca dari API ke string resource yang dilokalisasi
@Composable
fun getLocalizedWeatherDescription(description: String): String {
    return when {
        description.contains("clear", ignoreCase = true) -> stringResource(R.string.weather_clear)
        description.contains("cloud", ignoreCase = true) -> stringResource(R.string.weather_clouds)
        description.contains("rain", ignoreCase = true) -> stringResource(R.string.weather_rain)
        description.contains("drizzle", ignoreCase = true) -> stringResource(R.string.weather_drizzle)
        description.contains("thunder", ignoreCase = true) -> stringResource(R.string.weather_thunderstorm)
        description.contains("snow", ignoreCase = true) -> stringResource(R.string.weather_snow)
        description.contains("mist", ignoreCase = true) ||
                description.contains("fog", ignoreCase = true) ||
                description.contains("haze", ignoreCase = true) -> stringResource(R.string.weather_mist)
        else -> stringResource(R.string.weather_unknown)
    }
}

// Fungsi untuk mendapatkan main weather dari deskripsi
private fun getMainWeatherFromDescription(description: String): String {
    return when {
        description.contains("clear", ignoreCase = true) -> "Clear"
        description.contains("cloud", ignoreCase = true) -> "Clouds"
        description.contains("rain", ignoreCase = true) -> "Rain"
        description.contains("drizzle", ignoreCase = true) -> "Drizzle"
        description.contains("thunder", ignoreCase = true) -> "Thunderstorm"
        description.contains("snow", ignoreCase = true) -> "Snow"
        description.contains("mist", ignoreCase = true) ||
                description.contains("fog", ignoreCase = true) ||
                description.contains("haze", ignoreCase = true) -> "Mist"
        else -> "Unknown"
    }
}

// Fungsi untuk mendapatkan icon berdasarkan main weather dari API
private fun getWeatherIconFromMain(main: String): String {
    return when (main.lowercase()) {
        "clear" -> "☀️"
        "clouds" -> "☁️"
        "rain" -> "🌧️"
        "drizzle" -> "🌦️"
        "thunderstorm" -> "⛈️"
        "snow" -> "❄️"
        "mist", "fog", "haze" -> "🌫️"
        else -> "⛅"
    }
}

// Helper function untuk fallback (tetap dipertahankan untuk kompatibilitas)
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

// ------------------------------------------------------------
//                          PREVIEW
// ------------------------------------------------------------
@Preview(showBackground = true)
@Composable
fun WeatherRoutePreview() {
    WeatherTheme {
        WeatherRouteContent(
            city = "",
            onCityChange = {},
            onCheckWeather = {},
            onGetCurrentLocation = null,
            weatherData = null,
            hourlyForecast = emptyList(),
            dailyForecast = emptyList(),
            isLoading = false,
            errorMessage = null
        )
    }
}

@Preview(showBackground = true, name = "With Weather Data")
@Composable
fun WeatherRouteWithDataPreview() {
    WeatherTheme {
        val dummyData = WeatherResponse(
            name = "Kerten",
            main = Main(temp = 29.0, humidity = 77, feels_like = 31.0),
            weather = listOf(WeatherInfo("Clouds", "Mendung", "04d"))
        )

        WeatherRouteContent(
            city = "Kerten",
            onCityChange = {},
            onCheckWeather = {},
            onGetCurrentLocation = {},
            weatherData = dummyData,
            hourlyForecast = listOf(
                HourlyForecast("10.00", 30, "⛅"),
                HourlyForecast("11.00", 31, "🌦️"),
                HourlyForecast("17.39", 32, "☀️")
            ),
            dailyForecast = listOf(
                DailyForecast("Sen", "Mendung", "☁️", 31, 23),
                DailyForecast("Sel", "Cerah", "☀️", 33, 24),
                DailyForecast("Rab", "Hujan ringan", "🌦️", 32, 23),
                DailyForecast("Kam", "Berawan", "☁️", 31, 23),
                DailyForecast("Jum", "Cerah", "☀️", 33, 24),
                DailyForecast("Sab", "Hujan", "🌧️", 30, 23),
                DailyForecast("Min", "Cerah", "☀️", 32, 24)
            ),
            isLoading = false,
            errorMessage = null
        )
    }
}