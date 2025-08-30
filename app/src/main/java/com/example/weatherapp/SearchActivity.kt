package com.example.weatherapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.weatherapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import models.WeatherTableRow

import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class SearchActivity : AppCompatActivity() {

    private var locationExists: Boolean = false
    private lateinit var saveFav: FloatingActionButton
    private lateinit var city: String
    private lateinit var state: String
    private lateinit var loader: LinearLayout


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        loader = findViewById(R.id.loadingLayout)

        CoroutineScope(Dispatchers.Main).launch {
            loader.visibility = View.VISIBLE
            delay(1000) // 3 seconds delay
            loader.visibility = View.GONE
        }

        // Retrieve the city and state from the intent
        val location = intent.getStringExtra("LOCATION")
        if (location != null) {
            // Use the location string (e.g., "Los Angeles, CA")
            println("Selected Location: $location") // Replace with actual logic
        }
        if (location != null) {
            fetchWeatherData(location)
        }

        val parts = location?.split(", ") // Split by ", "

        city = parts?.getOrNull(0) ?: "" // First part is the city
        state = parts?.getOrNull(1) ?: "" // Second part is the state
        val locationText = "$city, $state"

        val locationTextView: TextView = findViewById(R.id.searchLocationText)
        locationTextView.setText(locationText)
        val locationHeadingView: TextView = findViewById(R.id.searchLocationHeading)
        locationHeadingView.setText(locationText)


        val backButton = findViewById<ImageButton>(R.id.returnButton)
        backButton.setOnClickListener {
            finish() // Closes the current activity and navigates back
        }

        val linearLayout = findViewById<LinearLayout>(R.id.weatherCard)
        linearLayout.setOnClickListener {
            println("hello!!!!!!!!!!!!!")
            val intent = Intent(this, DetailsActivity::class.java)
            intent.putExtra("date", "2024-01-01")
            intent.putExtra("index", 0)
            intent.putExtra("minTemp", 40)
            intent.putExtra("maxTemp", 40)
            startActivity(intent)

        }






        saveFav = findViewById(R.id.saveFav)

        fetchInitialState()

        saveFav.setOnClickListener {
            saveOrDeleteLocation()
        }
    }

    private fun fetchInitialState() {
        val client = OkHttpClient()
        val url = "http://10.0.2.2:5000/checkLocation?city=${city}&state=${state}"
        val request = Request.Builder()
            .url(url) // Replace with your API endpoint
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("API", "Failed to fetch state: ${e.message}")
                runOnUiThread {
                    Log.e("API", "Failed to fetch state: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseBody ->
                    // Check the response string
                    locationExists = when (responseBody) {
                        "exists" -> true
                        "notExists" -> false
                        else -> {
                            Log.e("API", "Unexpected response: $responseBody")
                            false // Default to inactive if response is unexpected
                        }
                    }

                    // Update the UI on the main thread
                    runOnUiThread {
                        updateFabIcon()
                    }
                }
            }
        })
    }

    private fun toggleFabState() {
        locationExists = !locationExists // Toggle state
        updateFabIcon()

    }

    private fun updateFabIcon() {
        val iconRes = if (locationExists) R.drawable.rem_fav else R.drawable.add_fav
        saveFav.setImageResource(iconRes)
    }

    private fun saveOrDeleteLocation() {
        // Initialize OkHttpClient
        val client = OkHttpClient()

        // Determine the URL based on the isActive state
        val endpoint = if (locationExists) "deleteLocation" else "saveLocation"
        val url = "http://10.0.2.2:5000/$endpoint?city=${city}&state=${state}"

        // Create a request
        val request = Request.Builder()
            .url(url)
            .build()

        // Make the request on a separate thread
        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("HTTP_ERROR", "Unexpected code $response")
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "Error: Unable to ${if (locationExists) "delete" else "save"} location.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // Toggle the isActive state
                        toggleFabState()
                        // Update the FAB icon based on the new state
                        runOnUiThread {
                            updateFabIcon()
                            Toast.makeText(
                                this,
                                city + if (!locationExists) " was removed from favourites" else " was added to favourites",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        // Log the response
                        val responseBody = response.body?.string()
                        Log.d("HTTP_RESPONSE", responseBody ?: "No Response")
                    }
                }
            } catch (e: IOException) {
                Log.e("HTTP_EXCEPTION", "Error: ${e.message}")
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Error: Unable to ${if (locationExists) "delete" else "save"} location.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }


    private fun fetchWeatherData(location: String) {
        lifecycleScope.launch(Dispatchers.Main) { // Start a coroutine on the Main thread
            try {
                // Step 1: Fetch location data (runs on a background thread)




                // Step 2: Fetch weather data using the location (runs after Step 1 completes)
                val weatherApiResponse = withContext(Dispatchers.IO) {
                    getWeatherDataFromApi(state, city) // Another suspend or blocking function
                }

                WeatherApiResponseHolder.apiResponse = weatherApiResponse.toString()


                // Step 3: Parse the weather API response
                val weatherData = parseWeatherApiResponse(weatherApiResponse) // Parsing can be synchronous

                // Step 4: Update the UI (back to the Main thread)
                displayCurrentWeather(weatherData)
                displayWeatherData(weatherData)

            } catch (e: Exception) {
                // Handle errors on the Main thread
                Toast.makeText(this@SearchActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }




    private fun displayCurrentWeather(weatherDataList: List<WeatherData>) {



        val humidityImageView: ImageView = findViewById(R.id.humidityView)
        humidityImageView.setImageResource(getWeatherIcon("humidity"))
        val windSpeedImageView: ImageView = findViewById(R.id.windsSeedView)
        windSpeedImageView.setImageResource(getWeatherIcon("wind_speed"))
        val visibilityImageView: ImageView = findViewById(R.id.visibilityView)
        visibilityImageView.setImageResource(getWeatherIcon("visibility"))
        val pressureImageView: ImageView = findViewById(R.id.pressureView)
        pressureImageView.setImageResource(getWeatherIcon("pressure"))

        val tempCurrentView: TextView = findViewById(R.id.searchCurrentTemp)
        tempCurrentView.setText(weatherDataList.get(0).temp)

        val currentForecastView: TextView = findViewById(R.id.searchCurrentForecast)
        currentForecastView.setText(weatherDataList.get(0).forecast)

        val currentIconImage: ImageView = findViewById(R.id.searchCurrentIcon)
        currentIconImage.setImageResource(weatherDataList.get(0).icon)

        val humidityView: TextView = findViewById(R.id.searchHumidtyValue)
        humidityView.setText(weatherDataList.get(0).humidity)

        val windSpeedView: TextView = findViewById(R.id.searchWindSpeedValue)
        windSpeedView.setText(weatherDataList.get(0).windSpeed)

        val visibilityView: TextView = findViewById(R.id.searchVisibilityValue)
        visibilityView.setText(weatherDataList.get(0).visibility)

        val pressureView: TextView = findViewById(R.id.searchPressureValue)
        pressureView.setText(weatherDataList.get(0).pressure)


    }


    fun parseWeatherApiResponse(response: String): List<WeatherData> {
        val weatherDataList = mutableListOf<WeatherData>()


        val jsonObject = JSONObject(response)
        val nextArray = jsonObject.getJSONArray("next")
        val current = jsonObject.getJSONObject("current")

        val currentValues = current.getJSONObject("values")
        val currentTemp = currentValues.getDouble("temperature").toInt().toString() + "°F"
        val currentHumidity = currentValues.getDouble("humidity").toInt().toString() + "%"
        val currentWindSpeed = currentValues.getDouble("windSpeed").toString() + "mph"
        val currentVisibility = currentValues.getDouble("visibility").toString() + "mi"
        val currentPressure =
            currentValues.getDouble("pressureSeaLevel").toString() + "inHg"
        val currentForecast = current.getString("forecast").lowercase()
        val currentIconName = current.getString("icon").lowercase().substringBeforeLast(".")
        val currentIcon = getWeatherIcon(currentIconName)

        weatherDataList.add(
            WeatherData(
                currentTemp,
                "null",
                currentIcon,
                "null",
                "null",
                currentHumidity,
                currentWindSpeed,
                currentVisibility,
                currentPressure,
                currentForecast,
                currentIconName
            )
        )

        for (i in 0 until nextArray.length()) {
            val dayObject = nextArray.getJSONObject(i)

            val date = dayObject.getString("startTime").substring(0, 10)
            val values = dayObject.getJSONObject("values")
            val temp = values.getDouble("temperature").toInt().toString() + "°F"
            val minTemp = values.getDouble("temperatureMin").toInt().toString()
            val maxTemp = values.getDouble("temperatureMax").toInt().toString()
            val humidity = values.getDouble("humidity").toInt().toString() + "%"
            val windSpeed = values.getDouble("windSpeed").toInt().toString() + "mph"
            val visibility = values.getDouble("visibility").toInt().toString() + "mi"
            val pressure = values.getDouble("pressureSeaLevel").toInt().toString() + "inHg"
            val forecast = dayObject.getString("forecast").lowercase()
            val iconName = dayObject.getString("icon").lowercase().substringBeforeLast(".")


            val icon = getWeatherIcon(iconName)

            weatherDataList.add(
                WeatherData(
                    temp,
                    date,
                    icon,
                    minTemp,
                    maxTemp,
                    humidity,
                    windSpeed,
                    visibility,
                    pressure,
                    forecast,
                    iconName
                )
            )


        }

        return weatherDataList
    }

    fun getWeatherIcon(forecast: String): Int {
        return try {
            // Use reflection to get the drawable resource ID
            val drawableClass = R.drawable::class.java
            val field = drawableClass.getDeclaredField(forecast)
            field.getInt(null) // Retrieve the resource ID
        } catch (e: Exception) {
            // Return a default icon if the resource is not found
            R.drawable.spinner
        }
    }

    private suspend fun getWeatherDataFromApi(state: String, city: String): String {
        return withContext(Dispatchers.IO) { // Ensure this runs on a background thread
            val client = OkHttpClient()

            // Build the URL
            val url = HttpUrl.Builder()
                .scheme("http")
                .host("10.0.2.2") // Localhost in Android emulator
                .port(5000)
                .addPathSegment("getweather")
                .addQueryParameter("state", state)
                .addQueryParameter("city", city)
                .addQueryParameter("autoDetect", "false")
                .build()
                .toString()

            try {
                println("Fetching weather data from $url")

                // Create and execute the request
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                // Handle the response
                if (response.isSuccessful) {
                    response.body?.string() ?: "No response body"
                } else {
                    println("Error: Weather response failed with code ${response.code}")
                    "Error: ${response.message}"
                }
            } catch (e: Exception) {
                println("Exception during weather fetch: ${e.message}")
                "Error: ${e.message}"
            }
        }
    }


    private fun displayWeatherData(weatherDataList: List<WeatherData>) {

        val tableLayout: TableLayout = findViewById(R.id.weatherTable2)
        tableLayout.removeAllViews()

        for (i in 1 until weatherDataList.size) {
            val data = weatherDataList[i]
            val weatherRow = WeatherTableRow(
                index = i,
                context = this,
                date = data.date,
                minTemp = data.minTemp,
                iconResId = data.icon,
                maxTemp = data.maxTemp
            )
            tableLayout.addView(weatherRow)
        }

    }
}
