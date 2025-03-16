package hu.bme.aut.ijv1hi.NiceWeatherApp
data class WeatherResponse(
    val weather: List<WeatherInfo>,
    val main: MainInfo,
    val wind: WindInfo,
    val sys: SysInfo,
    val timezone: Int,
    val visibility: Int
)

data class WeatherInfo(
    val main: String,
    val description: String,
    val icon: String
)

data class MainInfo(
    val temp: Double,
    val pressure: Int,
    val humidity: Int
)

data class WindInfo(
    val speed: Double
)

data class SysInfo(
    val sunrise: Long,
    val sunset: Long
)
