package com.example.weatherapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.highsoft.highcharts.core.HIChartView
import org.json.JSONObject
import android.graphics.Color
import android.net.Uri
import org.w3c.dom.Text

class DetailsActivity : AppCompatActivity() {
    private var tweetText: String = ""
    private var address:String = ""
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details) // Ensure you have activity_details.xml

        val index = intent.getIntExtra("index", 0)
        address = intent.getStringExtra("address").toString()


        val frameLayout = findViewById<FrameLayout>(R.id.fragmentContainer)

        val tab1: LinearLayout = findViewById(R.id.tab1)
        val tab2: LinearLayout = findViewById(R.id.tab2)
        val tab3: LinearLayout = findViewById(R.id.tab3)

        val tab1Image: ImageView = findViewById(R.id.tab1Image)
        val tab2Image: ImageView = findViewById(R.id.tab2Image)
        val tab3Image: ImageView = findViewById(R.id.tab3Image)

        val tab1Text: TextView = findViewById(R.id.tab1Text)
        val tab2Text: TextView = findViewById(R.id.tab2Text)
        val tab3Text: TextView = findViewById(R.id.tab3Text)



        val dayViewLayoutDefault = layoutInflater.inflate(R.layout.activity_dayview, frameLayout, false)
        frameLayout.addView(dayViewLayoutDefault)
        setDayDetails(index)

        var todayBtn: LinearLayout = findViewById(R.id.todayColumnComponent)
        var weeklyBtn: LinearLayout = findViewById(R.id.weeklyColumnComponent)
        var weatherDataBtn: LinearLayout = findViewById(R.id.weatherDataColumnComponent)
        var xbutton: ImageButton = findViewById(R.id.XButtonComponent)

        todayBtn.setOnClickListener {
            tab1.setBackgroundColor(Color.parseColor("#ffffff"))
            tab2.setBackgroundColor(Color.parseColor("#121212"))
            tab3.setBackgroundColor(Color.parseColor("#121212"))

            tab1Image.setColorFilter(Color.parseColor("#ffffff"))
            tab2Image.setColorFilter(Color.parseColor("#9F9F9F"))
            tab3Image.setColorFilter(Color.parseColor("#9F9F9F"))

            tab1Text.setTextColor(Color.parseColor("#ffffff"))
            tab2Text.setTextColor(Color.parseColor("#9F9F9F"))
            tab3Text.setTextColor(Color.parseColor("#9F9F9F"))
            // Inflate the dayViewLayout and add it to the FrameLayout
            frameLayout.removeAllViews() // Remove existing views
            val dayViewLayout = layoutInflater.inflate(R.layout.activity_dayview, frameLayout, false)
            frameLayout.addView(dayViewLayout)
            setDayDetails(index)
        }

        //Toast.makeText(this, WeatherApiResponseHolder.apiResponse, Toast.LENGTH_LONG).show()



        weeklyBtn.setOnClickListener {
            tab1.setBackgroundColor(Color.parseColor("#121212"))
            tab2.setBackgroundColor(Color.parseColor("#ffffff"))
            tab3.setBackgroundColor(Color.parseColor("#121212"))

            tab1Image.setColorFilter(Color.parseColor("#9F9F9F"))
            tab2Image.setColorFilter(Color.parseColor("#ffffff"))
            tab3Image.setColorFilter(Color.parseColor("#9F9F9F"))

            tab1Text.setTextColor(Color.parseColor("#9F9F9F"))
            tab2Text.setTextColor(Color.parseColor("#ffffff"))
            tab3Text.setTextColor(Color.parseColor("#9F9F9F"))
            // Inflate the chartViewLayout and add it to the FrameLayout
            frameLayout.removeAllViews() // Remove existing views
            val chartViewLayout = layoutInflater.inflate(R.layout.activity_tempchart, frameLayout, false)
            frameLayout.addView(chartViewLayout)
            val chartView: HIChartView = findViewById(R.id.chartView)
            ChartActivity.setupChart(chartView)
        }

        weatherDataBtn.setOnClickListener {
            tab1.setBackgroundColor(Color.parseColor("#121212"))
            tab2.setBackgroundColor(Color.parseColor("#121212"))
            tab3.setBackgroundColor(Color.parseColor("#ffffff"))

            tab1Image.setColorFilter(Color.parseColor("#9F9F9F"))
            tab2Image.setColorFilter(Color.parseColor("#9F9F9F"))
            tab3Image.setColorFilter(Color.parseColor("#ffffff"))

            tab1Text.setTextColor(Color.parseColor("#9F9F9F"))
            tab2Text.setTextColor(Color.parseColor("#9F9F9F"))
            tab3Text.setTextColor(Color.parseColor("#ffffff"))
            // Inflate the dayViewLayout and add it to the FrameLayout
            frameLayout.removeAllViews() // Remove existing views
            val gaugeChartLayout = layoutInflater.inflate(R.layout.activity_gaugechart, frameLayout, false)
            frameLayout.addView(gaugeChartLayout)
            val gaugeChartView: HIChartView = findViewById(R.id.gaugeChartView)
            GaugeChartActivity.setupChart(gaugeChartView, index)
        }

        xbutton.setOnClickListener {
            val tweetUrl = "https://twitter.com/intent/tweet?text=${Uri.encode(tweetText)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tweetUrl))
            startActivity(intent)
        }





        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Closes the current activity and navigates back
        }
    }
    fun extractCityAndState(input: String): String {
        val parts = input.split(",").map { it.trim() } // Split by comma and trim whitespace
        return if (parts.size >= 2) {
            "${parts[0]}, ${parts[1]}" // Combine the first two parts
        } else {
            input // Return original input if the format is unexpected
        }
    }

    @SuppressLint("WrongViewCast")
    private fun setDayDetails(index: Int) {
        // Parse the response JSON
        val responseJson = JSONObject(WeatherApiResponseHolder.apiResponse)

        // Fetch the "current" object from the JSON
        val current = responseJson.getJSONObject("current")
        val values = current.getJSONObject("values")
        val coordinates = responseJson.getJSONObject("coordinates")

        // Get the address
        val locationAddress = if (coordinates.has("address")) {
            coordinates.getString("address") // Use the address from JSON if it exists
        } else {
            address
        }

        // Extract city and state from the address
        val location = extractCityAndState(locationAddress)

        // Build the tweet text
        tweetText = "Check out ${locationAddress}'s weather! It is ${values.getDouble("temperature")}°F! #CSCI571WeatherSearch"

        // Update the UI with current weather details
        val locationText: TextView = findViewById(R.id.locationText)
        locationText.text = location

        // Set weather icons
        val windSpeedImage: ImageView = findViewById(R.id.windSpeedComponent)
        windSpeedImage.setImageResource(getWeatherIcon("wind_speed"))

        val pressureImage: ImageView = findViewById(R.id.pressureComponent)
        pressureImage.setImageResource(getWeatherIcon("pressure"))

        val precipitationImage: ImageView = findViewById(R.id.precipitationComponent)
        precipitationImage.setImageResource(getWeatherIcon("pouring"))

        val tempImage: ImageView = findViewById(R.id.temperatureComponent)
        tempImage.setImageResource(getWeatherIcon("ic_thermometer"))

        val forecastIconName = current.getString("icon").lowercase().substringBeforeLast(".")
        val forecastImage: ImageView = findViewById(R.id.forecastComponent)
        forecastImage.setImageResource(getWeatherIcon(forecastIconName))

        val humidityImage: ImageView = findViewById(R.id.humidityComponent)
        humidityImage.setImageResource(getWeatherIcon("humidity"))

        val visibilityImage: ImageView = findViewById(R.id.visibilityComponent)
        visibilityImage.setImageResource(getWeatherIcon("visibility"))

        val cloudCoverImage: ImageView = findViewById(R.id.cloudCoverComponent)
        cloudCoverImage.setImageResource(getWeatherIcon("cloud_cover"))

        val uvImage: ImageView = findViewById(R.id.uvComponent)
        uvImage.setImageResource(getWeatherIcon("uv"))

        // Set weather values
        val windSpeedView: TextView = findViewById(R.id.windSpeedValue)
        windSpeedView.text = "${values.getDouble("windSpeed")} mph"

        val pressureView: TextView = findViewById(R.id.pressureValue)
        pressureView.text = "${values.getDouble("pressureSeaLevel")} inHg"

        val precipitationView: TextView = findViewById(R.id.precipitationValue)
        precipitationView.text = "${values.getDouble("precipitationProbability").toInt()}%"

        val tempView: TextView = findViewById(R.id.temperatureValue)
        tempView.text = "${values.getDouble("temperature").toInt()}°F"

        val forecastView: TextView = findViewById(R.id.forecastValue)
        forecastView.text = current.getString("forecast")

        val humidityView: TextView = findViewById(R.id.humidityValue)
        humidityView.text = "${values.getDouble("humidity").toInt()}%"

        val visibilityView: TextView = findViewById(R.id.visibilityValue)
        visibilityView.text = "${values.getDouble("visibility")} mi"

        val cloudCoverView: TextView = findViewById(R.id.cloudCoverValue)
        cloudCoverView.text = "${values.getDouble("cloudCover").toInt()}%"

        val uvView: TextView = findViewById(R.id.uvValue)
        uvView.text = "${values.getDouble("uvIndex").toInt()}"
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
}