package com.example.weatherapp


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import androidx.viewpager2.widget.ViewPager2
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.*
import models.WeatherTableRow
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException


data class WeatherData(
    val temp: String,
    val date: String,
    val icon: Int,
    val minTemp: String,
    val maxTemp: String,
    val humidity: String,
    val windSpeed: String,
    val visibility: String,
    val pressure: String,
    val forecast: String,
    val iconName: String
)

private val pageDataList = MutableList<List<WeatherData?>?>(10) { null }

data class Favourites(
    val city: String,
    val state: String
)


class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: MultiLayoutPagerAdapter
    private val favouritesList = mutableListOf<Favourites>()
    private lateinit var loader: LinearLayout


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
        setContentView(R.layout.activity_main)
        loader = findViewById(R.id.loadingLayout)

        CoroutineScope(Dispatchers.Main).launch {
            loader.visibility = View.VISIBLE
            delay(1000) // 3 seconds delay
            loader.visibility = View.GONE
        }


        fetchWeatherData()
        favouritesList.addAll(fetchFavourites())


        viewPager = findViewById(R.id.viewPager)

        val tabDots: TabLayout = findViewById(R.id.tabDots)
        setupViewPager()

        TabLayoutMediator(tabDots, viewPager) { _, _ ->
            // Custom tab logic if needed
        }.attach()

        if (!Places.isInitialized()) {
            Places.initialize(
                applicationContext,
                "AIzaSyBnMrVRvk8KfRdGEbb-cmekg3I4xBxpi8U"
            )
        }


        setupPlacesAPI()





    }

    override fun onResume() {
        super.onResume()
        val heading: LinearLayout = findViewById(R.id.heading)
        heading.visibility = View.VISIBLE
        val searchBar: LinearLayout = findViewById(R.id.searchInputLayout)
        searchBar.visibility = View.GONE
        fetchAndSetUpData()
        fetchWeatherData()


    }

    private fun setupPlacesAPI() {
        val placeholderImage = findViewById<LinearLayout>(R.id.heading)
        val searchInputLayout = findViewById<TextInputLayout>(R.id.searchInputLayout)
        val imageButton = findViewById<ImageButton>(R.id.imageButton)
        val searchEditText = findViewById<AutoCompleteTextView>(R.id.searchEditText)

        if (!Places.isInitialized()) {
            Places.initialize(
                applicationContext,
                "AIzaSyBnMrVRvk8KfRdGEbb-cmekg3I4xBxpi8U"
            )
        }
        val placesClient = Places.createClient(this)

        imageButton.setOnClickListener {
            toggleSearchVisibility(placeholderImage, searchInputLayout, searchEditText)
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (query.isNotEmpty()) {
                    getPlacePredictions(placesClient, query) { predictions ->
                        val suggestions = predictions.map { prediction ->
                            formatPlacePrediction(prediction)
                        }
                        val adapter = ArrayAdapter(
                            this@MainActivity,
                            R.layout.dropdown_item,
                            suggestions
                        )
                        searchEditText.setAdapter(adapter)
                        searchEditText.showDropDown()
                    }
                }
            }
        })

        searchEditText.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = searchEditText.adapter.getItem(position) as String
            searchEditText.setText(selectedItem)
            searchEditText.dismissDropDown()
            startSearchActivity(selectedItem)
        }
    }

    private fun toggleSearchVisibility(
        placeholderImage: LinearLayout,
        searchInputLayout: TextInputLayout,
        searchEditText: AutoCompleteTextView
    ) {
        if (searchInputLayout.visibility == View.GONE) {
            placeholderImage.visibility = View.GONE
            searchInputLayout.visibility = View.VISIBLE
            searchEditText.requestFocus() // Show keyboard
        } else {
            searchInputLayout.visibility = View.GONE
            placeholderImage.visibility = View.VISIBLE
        }
    }

    private fun formatPlacePrediction(prediction: AutocompletePrediction): String {
        val city = prediction.getPrimaryText(null).toString()
        val parts = prediction.getSecondaryText(null)?.split(", ")
        val state = if (parts != null && parts.size > 1) parts[parts.size - 2] else ""
        return "$city, $state"
    }

    private fun startSearchActivity(selectedItem: String) {
        val intent = Intent(this, SearchActivity::class.java).apply {
            putExtra("LOCATION", selectedItem)
        }
        startActivity(intent)
    }



    private fun fetchAndSetUpData() {
        favouritesList.clear()
        favouritesList.addAll(fetchFavourites())
        setupViewPager()
    }

    private fun fetchFavourites(): List<Favourites> {
        val favouritesList = mutableListOf<Favourites>()

        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("http://10.0.2.2:5000/getFavourites")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()

                if (!responseBody.isNullOrEmpty()) {
                    val jsonArray = JSONArray(responseBody)
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val city = jsonObject.getString("city")
                        val state = jsonObject.getString("state")

                        favouritesList.add(Favourites(city, state))
                    }
                }
            } else {
                Log.e("API_ERROR", "Failed to fetch data: ${response.message}")
            }

            response.close()
        } catch (e: Exception) {
            Log.e("API_ERROR", "Error occurred", e)
        }

        return favouritesList

    }

    private fun setupViewPager() {
        adapter = MultiLayoutPagerAdapter(this ,favouritesList, pageDataList) { position ->
            removePage(position)
        }
        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            private var previousPage: Int = -1

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)

                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    val currentPage = viewPager.currentItem // Get the current page index
                    if (currentPage != previousPage && currentPage > 0 && currentPage <= favouritesList.size) {
                        // Trigger API call only if the page has changed
                        fetchFavouriteWeather(currentPage - 1)
                        previousPage = currentPage // Update previousPage
                    }
                }
            }
        })
    }

    private fun fetchFavouriteWeather(position: Int) {
        val favourite = favouritesList[position] ?: return


        adapter.notifyItemChanged(position + 1)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = HttpUrl.Builder()
                    .scheme("http")
                    .host("10.0.2.2")
                    .port(5000)
                    .addPathSegment("getweather")
                    .addQueryParameter("city", favourite.city)
                    .addQueryParameter("state", favourite.state)
                    .addQueryParameter("autoDetect", "false")
                    .build()
                    .toString()
                println("url!!!!!!!!!!!!!!!!!!")
                println(url)
                println(url)

                var weatherData: List<WeatherData>
                weatherData = apiFetchFavourite(url, favourite.city, favourite.state, position) //this is the one

                withContext(Dispatchers.Main) {
                    adapter.updatePageData(position+1, weatherData)

                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    // Show an error message if needed
                }
            }
        }
    }
    private suspend fun apiFetchFavourite(url: String, city: String, state: String, position: Int): List<WeatherData> {

        var weatherData: List<WeatherData> = emptyList()


        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val weatherApiResponse = response.body?.string() ?: "No response body"

                val weatherData = parseWeatherApiResponse(weatherApiResponse)
                WeatherApiResponseHolder.apiResponse = weatherApiResponse.toString()
                return weatherData

            } else {
                println("Error: Weather response failed with code ${response.code}")
                "Error: ${response.message}"
            }
        } catch (e: IOException) {
            println("Exception occurred while fetching weather data: ${e.message}")
        }
        return weatherData


    }

    private fun removePage(position: Int) {
        if (position > 0 && position <= favouritesList.size) {
            deleteFavourite(favouritesList.get(position - 1))
            favouritesList.removeAt(position - 1)
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, favouritesList.size)

        }
    }

    private fun deleteFavourite(favouriteLocation: Favourites) {
        val client = OkHttpClient()

        try {
            val url = HttpUrl.Builder()
                .scheme("http")
                .host("10.0.2.2")
                .port(5000)
                .addPathSegment("deleteLocation")
                .addQueryParameter("city", favouriteLocation.city)
                .addQueryParameter("state", favouriteLocation.state)
                .build()

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                Toast.makeText(
                    this,
                    favouriteLocation.city + " was removed from favourites",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                println("Failed to delete favourite location: ${response.message}")
            }

            response.close()
        } catch (e: Exception) {
            println("Error occurred while deleting favourite location: ${e.message}")
        }
    }


    class MultiLayoutPagerAdapter(
        private val context: Context,
        private val favouritesList: List<Favourites>,
        private val pageDataList: MutableList<List<WeatherData?>?>,
        private val removePageCallback: (Int) -> Unit
    ) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_MAIN = 0
        private val TYPE_WEATHER = 1

        override fun getItemViewType(position: Int): Int {
            return if (position == 0) TYPE_MAIN else TYPE_WEATHER
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == TYPE_MAIN) {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.activity_weatherdata, parent, false)
                MainViewHolder(view)
            } else {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.activity_weatherdatafavourites, parent, false)
                WeatherViewHolder(view)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is MainViewHolder){
                holder.weatherCardMain.setOnClickListener{
                    println("hello Main!!!!!!!!!!!!!")
                    val intent = Intent(context, DetailsActivity::class.java)
                    intent.putExtra("date", "2024-01-01")
                    intent.putExtra("index", 0)
                    intent.putExtra("minTemp", 40)
                    intent.putExtra("maxTemp", 40)
                    intent.putExtra("address", "Los Angeles, CA")
                    context.startActivity(intent)

                }
            }
            if (holder is WeatherViewHolder) {

                CoroutineScope(Dispatchers.Main).launch {
                    holder.favouraitsLoader.visibility = View.VISIBLE
                    holder.pageView.visibility = View.GONE
                    delay(1000) // 3 seconds delay
                    holder.favouraitsLoader.visibility = View.GONE
                    holder.pageView.visibility = View.VISIBLE
                }

                val favourite = favouritesList[position - 1]
                val data = pageDataList[position]
                holder.favouriteLocationTextView.text = "${favourite.city}, ${favourite.state}"
                holder.deleteFavourite.setOnClickListener {
                    removePageCallback(position)
                }

                holder.weatherCard.setOnClickListener{
                    println("hello!!!!!!!!!!!!!")
                    val intent = Intent(context, DetailsActivity::class.java)
                    intent.putExtra("date", "2024-01-01")
                    intent.putExtra("index", 0)
                    intent.putExtra("minTemp", 40)
                    intent.putExtra("maxTemp", 40)
                    intent.putExtra("address", "${favourite.city}, ${favourite.state}")
                    context.startActivity(intent)
                }



                holder.humidityView.text = data?.get(0)?.humidity ?: "N/A"
                holder.visibilityView.text = data?.get(0)?.visibility ?: "N/A"
                holder.tempCurrentView.text = data?.get(0)?.temp ?: "N/A"
                holder.currentForecastView.text = data?.get(0)?.forecast ?: "N/A"
                val icon = data?.get(0)?.icon ?: R.drawable.clear_day
                holder.currentIconImage.setImageResource(icon)
                holder.windSpeedView.text = data?.get(0)?.windSpeed ?: "N/A"
                holder.pressureView.text = data?.get(0)?.pressure ?: "N/A"


                holder.tableLayout.removeAllViews()

                if (data != null) {
                    for (i in 1 until data.size-1) {
                        val data = data[i]
                        val weatherRow = data?.let {
                            WeatherTableRow(
                                index = i,
                                context = context,
                                date = it.date,
                                minTemp = data.minTemp,
                                iconResId = data.icon,
                                maxTemp = data.maxTemp
                            )
                        }
                        holder.tableLayout.addView(weatherRow)
                    }
                }
            }
        }

        override fun getItemCount(): Int = favouritesList.size + 1

        fun updatePageData(position: Int, data: List<WeatherData>) {
            pageDataList[position] = data
            notifyItemChanged(position) // Refresh the specific page
        }

        class MainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
            val weatherCardMain: LinearLayout = itemView.findViewById(R.id.weatherCardFavourites)
        }

        class WeatherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val favouriteLocationTextView: TextView =
                itemView.findViewById(R.id.favouritesLocationText)
            val deleteFavourite: FloatingActionButton = itemView.findViewById(R.id.deleteFavourites)
            val weatherCard: LinearLayout = itemView.findViewById(R.id.weatherCardFavourites)

            var favouraitsLoader: LinearLayout = itemView.findViewById(R.id.favouriteLoadingLayout)
            var pageView: FrameLayout = itemView.findViewById(R.id.pageView)


            val tempCurrentView: TextView = itemView.findViewById(R.id.currentTemp)

            val currentForecastView: TextView = itemView.findViewById(R.id.currentForecast)

            val currentIconImage: ImageView = itemView.findViewById(R.id.currentIcon)

            var humidityView: TextView = itemView.findViewById(R.id.humidtyValue)

            val windSpeedView: TextView = itemView.findViewById(R.id.windSpeedValue)

            var visibilityView: TextView = itemView.findViewById(R.id.visibilityValue)

            val pressureView: TextView = itemView.findViewById(R.id.pressureValue)

            val tableLayout: TableLayout = itemView.findViewById(R.id.weatherTable2)


        }
    }

    private fun getPlacePredictions(
        placesClient: PlacesClient,
        query: String,
        callback: (List<AutocompletePrediction>) -> Unit
    ) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                callback(response.autocompletePredictions)
            }
            .addOnFailureListener { exception ->
                Log.e("PlacesAPI", "Error getting predictions: ${exception.message}")
            }
    }

    private fun fetchWeatherData() {

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val locationData = withContext(Dispatchers.IO) {
                    getLocationData()
                }


                val jsonObject = JSONObject(locationData)
                val location = jsonObject.getString("loc")
                val city = jsonObject.getString("city")
                val state = jsonObject.getString("region")
                val locationText = "$city, $state"


                val weatherApiResponse = withContext(Dispatchers.IO) {
                    getWeatherDataFromApi(location)
                }

                WeatherApiResponseHolder.apiResponse = weatherApiResponse.toString()

                val weatherData =
                    parseWeatherApiResponse(weatherApiResponse)

                displayCurrentWeather(weatherData, locationText)
                displayWeatherData(weatherData)

            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    private suspend fun getLocationData(): String {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val ipUrl = "https://ipinfo.io?token=1b3c40a62a2191"

            try {
                println("Thread before location request: ${Thread.currentThread().name}")

                val locationRequest = Request.Builder().url(ipUrl).build()
                val locationResponse = client.newCall(locationRequest).execute()
                if (locationResponse.isSuccessful) {
                    locationResponse.body?.string() ?: "No response body"
                } else {
                    println("Error: Location response failed with code ${locationResponse.code}")
                    "Error: ${locationResponse.message}"
                }
            } catch (e: Exception) {
                println("Exception during location fetch: ${e.message}")
                "Error: ${e.message}"
            }
        }
    }


    private fun displayCurrentWeather(weatherDataList: List<WeatherData>, locationText: String) {

        val locationTextView: TextView = findViewById(R.id.locationText)
        locationTextView.setText(locationText)

        val humidityImageView: ImageView = findViewById(R.id.humidityView)
        humidityImageView.setImageResource(getWeatherIcon("humidity"))
        val windSpeedImageView: ImageView = findViewById(R.id.windsSeedView)
        windSpeedImageView.setImageResource(getWeatherIcon("wind_speed"))
        val visibilityImageView: ImageView = findViewById(R.id.visibilityView)
        visibilityImageView.setImageResource(getWeatherIcon("visibility"))
        val pressureImageView: ImageView = findViewById(R.id.pressureView)
        pressureImageView.setImageResource(getWeatherIcon("pressure"))

        val tempCurrentView: TextView = findViewById(R.id.currentTemp)
        tempCurrentView.setText(weatherDataList.get(0).temp)

        val currentForecastView: TextView = findViewById(R.id.currentForecast)
        currentForecastView.setText(weatherDataList.get(0).forecast)

        val currentIconImage: ImageView = findViewById(R.id.currentIcon)
        currentIconImage.setImageResource(weatherDataList.get(0).icon)

        val humidityView: TextView = findViewById(R.id.humidtyValue)
        humidityView.setText(weatherDataList.get(0).humidity)

        val windSpeedView: TextView = findViewById(R.id.windSpeedValue)
        windSpeedView.setText(weatherDataList.get(0).windSpeed)

        val visibilityView: TextView = findViewById(R.id.visibilityValue)
        visibilityView.setText(weatherDataList.get(0).visibility)

        val pressureView: TextView = findViewById(R.id.pressureValue)
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
            val drawableClass = R.drawable::class.java
            val field = drawableClass.getDeclaredField(forecast)
            field.getInt(null)
        } catch (e: Exception) {
            R.drawable.spinner
        }
    }

    private suspend fun getWeatherDataFromApi(location: String): String {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val url = HttpUrl.Builder()
                .scheme("http")
                .host("10.0.2.2")
                .port(5000)
                .addPathSegment("getweather")
                .addQueryParameter("location", location)
                .addQueryParameter("autoDetect", "true")
                .build()
                .toString()

            println("url!!!!!!!!!!!!!!!!!!")
            println(url)

            try {
                println("Fetching weather data from $url")
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

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

        for (i in 1 until weatherDataList.size-1) {
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
