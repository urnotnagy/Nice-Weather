package hu.bme.aut.ijv1hi.NiceWeatherApp


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


class WeatherFragment : Fragment() {

    private lateinit var recyclerViewWeather: RecyclerView
    private lateinit var weatherAdapter: WeatherAdapter

    // UI Elements for the main weather info
    private lateinit var textViewTemperature: TextView
    private lateinit var textViewDescription: TextView
    private lateinit var imageViewWeatherIcon: ImageView
    private lateinit var textViewDate: TextView
    private lateinit var textViewLocation: TextView

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private fun getColorForWeatherCondition(condition: String): Int {
        return when (condition.toLowerCase()) {
            "clear" -> requireContext().getColor(R.color.clear_sky)
            "clouds" -> requireContext().getColor(R.color.cloudy)
            "rain" -> requireContext().getColor(R.color.rainy)
            "snow" -> requireContext().getColor(R.color.snowy)
            "thunderstorm" -> requireContext().getColor(R.color.thunderstorm)
            "mist", "fog" -> requireContext().getColor(R.color.misty)
            else -> requireContext().getColor(R.color.default_bg)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_weather, container, false)

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        // Set up the refresh listener
        swipeRefreshLayout.setOnRefreshListener {
            if (isInternetAvailable(requireContext())) {
                getCurrentLocationAndWeather() // Trigger weather update if online
            } else {

                Toast.makeText(requireContext(), "No internet connection. You're viewing cached data.", Toast.LENGTH_SHORT).show()

                swipeRefreshLayout.isRefreshing = false
            }
        }


        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize the UI elements
        textViewDate = view.findViewById(R.id.textViewDate)
        textViewLocation = view.findViewById(R.id.textViewLocation)
        textViewTemperature = view.findViewById(R.id.textViewTemperature)
        textViewDescription = view.findViewById(R.id.textViewDescription)
        imageViewWeatherIcon = view.findViewById(R.id.imageViewWeatherIcon)

        // Initialize RecyclerView
        recyclerViewWeather = view.findViewById(R.id.recyclerViewWeather)
        recyclerViewWeather.layoutManager = GridLayoutManager(context, 3)
        weatherAdapter = WeatherAdapter(mutableListOf())
        recyclerViewWeather.adapter = weatherAdapter


        setCurrentDate()


        loadWeatherFromCache()


        checkAndRequestLocationPermissions()

        return view
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }



    private fun setCurrentDate() {
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault())
        textViewDate.text = dateFormat.format(currentDate)
    }

    private fun getCurrentLocationAndWeather() {
        swipeRefreshLayout.isRefreshing = true

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions if not granted
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 100)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                updateLocationName(latitude, longitude)
                fetchCurrentWeather(latitude, longitude)
            } else {
                Log.e("WeatherFragment", "Failed to get location")
            }
        }
    }

    private fun updateLocationName(latitude: Double, longitude: Double) {

        val geocoder = android.location.Geocoder(requireContext(), Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val cityName = addresses[0].locality ?: "Unknown Location"
                textViewLocation.text = cityName
            }
        } catch (e: Exception) {
            e.printStackTrace()
            textViewLocation.text = "Unknown Location"
        }
    }

    private fun fetchCurrentWeather(latitude: Double, longitude: Double) {
        val apiKey = "5f94bfb4972076578cbd37d602c1954a"  // This is my openmaps api key

        val call = RetrofitInstance.api.getCurrentWeather(latitude, longitude, apiKey)
        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(
                call: Call<WeatherResponse>,
                response: Response<WeatherResponse>
            ) {
                if (response.isSuccessful) {
                    val weatherResponse = response.body()
                    weatherResponse?.let {
                        updateMainWeatherInfo(it)
                        updateWeatherDetails(it)


                        saveWeatherToCache(it)

                        swipeRefreshLayout.isRefreshing = false
                    }
                } else {
                    Log.e("WeatherFragment", "Error: ${response.code()}")
                    swipeRefreshLayout.isRefreshing = false
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("WeatherFragment", "Failed to fetch weather data", t)
                swipeRefreshLayout.isRefreshing = false
            }
        })
    }



    private fun saveWeatherToCache(weatherResponse: WeatherResponse) {
        val sharedPreferences = requireContext().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val gson = Gson()
        val weatherJson = gson.toJson(weatherResponse)
        editor.putString("cached_weather", weatherJson)

        // Save weather icon URL
        val iconCode = weatherResponse.weather[0].icon
        val iconUrl = "https://openweathermap.org/img/wn/${iconCode}@2x.png"
        editor.putString("cached_icon_url", iconUrl)

        // Save location name (use a default value if unavailable)
        val locationName = textViewLocation.text.toString()
        editor.putString("cached_location", locationName)

        // Save the current date
        editor.putString("cached_date", textViewDate.text.toString())

        editor.apply()
    }



    private fun loadWeatherFromCache() {
        val sharedPreferences = requireContext().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)


        val weatherJson = sharedPreferences.getString("cached_weather", null)
        val cachedDate = sharedPreferences.getString("cached_date", "Unknown Date")
        val cachedLocation = sharedPreferences.getString("cached_location", "Unknown Location")

        if (weatherJson != null) {

            val gson = Gson()
            val weatherResponse = gson.fromJson(weatherJson, WeatherResponse::class.java)

            // Update UI with cached data
            textViewTemperature.text = "${weatherResponse.main.temp.toInt()}°C"
            textViewDescription.text = weatherResponse.weather[0].description.capitalize()
            textViewDate.text = cachedDate
            textViewLocation.text = cachedLocation

            // Update RecyclerView details
            updateWeatherDetails(weatherResponse)
            updateMainWeatherInfo(weatherResponse)


            if (!isInternetAvailable(requireContext())) {
                showCachedDataBanner()
            }
        }
    }
    private fun showCachedDataBanner() {
        Toast.makeText(requireContext(), "You're viewing cached data. Check your internet connection.", Toast.LENGTH_LONG).show()
    }



    // Update the main weather info (temperature + description)
    private fun updateMainWeatherInfo(weatherResponse: WeatherResponse) {
        val temperature = "${weatherResponse.main.temp.toInt()}°C"
        val description = weatherResponse.weather[0].description.capitalize(Locale.getDefault())

        Log.d("WeatherFragment", "After update - Temperature: ${textViewTemperature.text}, Description: ${textViewDescription.text}")

        textViewTemperature.text = temperature
        textViewDescription.text = description

//        textViewTemperature.setTextColor(Color.BLACK) // Set a visible color
//        textViewDescription.setTextColor(Color.BLACK)


        Log.d("WeatherFragment", "After update - Temperature: ${textViewTemperature.text}, Description: ${textViewDescription.text}")


        Log.d("WeatherFragment", "Temperature: $temperature")
        Log.d("WeatherFragment", "Description: $description")


        val iconCode = weatherResponse.weather[0].icon
        val iconUrl = "https://openweathermap.org/img/wn/${iconCode}@2x.png"
        Glide.with(requireContext()).load(iconUrl).into(imageViewWeatherIcon)

        // Dynamically set the background color based on weather condition
        val condition = weatherResponse.weather.getOrNull(0)?.main ?: "Unknown"
        val backgroundColor = getColorForWeatherCondition(condition)

        // Find the container and set its background color
        val container = view?.findViewById<View>(R.id.container)
        container?.setBackgroundColor(backgroundColor)
    }



    private fun updateWeatherDetails(weatherResponse: WeatherResponse) {
        val newWeatherData = mutableListOf<WeatherItem>()


        newWeatherData.add(WeatherItem(weatherResponse.main.pressure.toString(), "Pressure"))
        newWeatherData.add(WeatherItem(weatherResponse.wind.speed.toString(), "Wind"))
        newWeatherData.add(WeatherItem(convertUnixTimeToString(weatherResponse.sys.sunrise, weatherResponse.timezone), "Sunrise"))
        newWeatherData.add(WeatherItem(weatherResponse.main.humidity.toString(), "Humidity"))
        newWeatherData.add(WeatherItem(weatherResponse.visibility.toString(), "Visibility"))
        newWeatherData.add(WeatherItem(convertUnixTimeToString(weatherResponse.sys.sunset, weatherResponse.timezone), "Sunset"))


        weatherAdapter.updateData(newWeatherData)
    }

    private fun convertUnixTimeToString(unixTime: Long, timezoneOffset: Int): String {
        // The timezone offset is in seconds, so we add it directly to the Unix timestamp (already in UTC).
        val adjustedTimeInMillis = (unixTime + timezoneOffset) * 1000L

        val date = Date(adjustedTimeInMillis)

        // Set up the SimpleDateFormat to use UTC timezone, so that the device's timezone doesn't interfere
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC") // Make sure we use UTC for the time zone

        return format.format(date)
    }



    private fun checkAndRequestLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                100
            )
        } else {
            getCurrentLocationAndWeather()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndWeather()
            } else {
                Log.e("WeatherFragment", "Location permission denied")
            }
        }
    }
}
