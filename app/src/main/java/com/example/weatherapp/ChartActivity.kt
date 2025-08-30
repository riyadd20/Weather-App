package com.example.weatherapp

import android.R
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.core.app.ComponentActivity
import com.highsoft.highcharts.common.HIColor
import com.highsoft.highcharts.common.HIGradient
import com.highsoft.highcharts.common.HIStop
import com.highsoft.highcharts.common.hichartsclasses.HIArearange
import com.highsoft.highcharts.common.hichartsclasses.HIChart
import com.highsoft.highcharts.common.hichartsclasses.HILegend
import com.highsoft.highcharts.common.hichartsclasses.HIOptions
import com.highsoft.highcharts.common.hichartsclasses.HIShadowOptionsObject
import com.highsoft.highcharts.common.hichartsclasses.HITitle
import com.highsoft.highcharts.common.hichartsclasses.HITooltip
import com.highsoft.highcharts.common.hichartsclasses.HIXAxis
import com.highsoft.highcharts.common.hichartsclasses.HIYAxis
import com.highsoft.highcharts.core.HIChartView
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.LinkedList
import java.util.Locale
import java.util.TimeZone


@SuppressLint("RestrictedApi")
class ChartActivity: ComponentActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_item)

    }



    companion object {
        fun convertToUnixTimestamp(dateString: String): Long {
            // Define the date format
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("UTC") // Ensure UTC for consistency

            // Parse the date and return the Unix timestamp
            val date = dateFormat.parse(dateString)
            return date?.time ?: 0L // Return 0L if parsing fails
        }

        fun setupChart(chartView: HIChartView) {
            val options = HIOptions()

            val chart = HIChart()
            chart.type = "arearange"
            chart.height = "300px" // Adjust the height of the graph
            chart.marginTop = 50
            options.chart = chart

            val title = HITitle()
            title.text = "Temperature variation by day"
            options.title = title

            val xaxis = HIXAxis()
            xaxis.type = "datetime"
            options.xAxis = object : ArrayList<HIXAxis?>() {
                init {
                    add(xaxis)
                }
            }

            val yaxis = HIYAxis()
            yaxis.title = HITitle()
            yaxis.title.text = "Values" // Optional label for y-axis
            options.yAxis = object : ArrayList<HIYAxis?>() {
                init {
                    add(yaxis)
                }
            }

            val tooltip = HITooltip()
            tooltip.valueSuffix = "°F"
            options.tooltip = tooltip

            val legend = HILegend()
            legend.enabled = false
            options.legend = legend

            val series = HIArearange()
            series.name = "Temperatures"

            val responseJson = WeatherApiResponseHolder.apiResponse
            val responseJsonObject = JSONObject(responseJson)
            val nextArray = responseJsonObject.getJSONArray("next")

            val seriesData = Array(nextArray.length()) { index ->
                val item = nextArray.getJSONObject(index)

                val timestamp = convertToUnixTimestamp(item.getString("startTime").substring(0, 10))
                val values = item.getJSONObject("values")

                val minTemp = values.getDouble("temperatureMin").toInt()
                val maxTemp = values.getDouble("temperatureMax").toInt()

                arrayOf<Any?>(timestamp, minTemp, maxTemp)
            }

            series.data = ArrayList(Arrays.asList(*seriesData.filterNotNull().toTypedArray()))

            val gradientMap = mapOf(
                "linearGradient" to mapOf(
                    "x1" to 0,
                    "x2" to 0,
                    "y1" to 0,
                    "y2" to 1
                ),
                "stops" to arrayListOf(
                    arrayListOf(0, "#f99b1c"),
                    arrayListOf(1, "#99b4d8")
                )
            )
            series.setProperty("color", gradientMap)

            options.series = ArrayList(Arrays.asList(series))

            chartView.options = options
        }

    }
}