package com.example.weatherapp


import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ComponentActivity
import com.highsoft.highcharts.common.HIColor
import com.highsoft.highcharts.common.hichartsclasses.HIArearange
import com.highsoft.highcharts.common.hichartsclasses.HIBackground
import com.highsoft.highcharts.common.hichartsclasses.HICSSObject
import com.highsoft.highcharts.common.hichartsclasses.HIChart
import com.highsoft.highcharts.common.hichartsclasses.HIData
import com.highsoft.highcharts.common.hichartsclasses.HIDataLabels
import com.highsoft.highcharts.common.hichartsclasses.HIEvents
import com.highsoft.highcharts.common.hichartsclasses.HILegend
import com.highsoft.highcharts.common.hichartsclasses.HIOptions
import com.highsoft.highcharts.common.hichartsclasses.HIPane
import com.highsoft.highcharts.common.hichartsclasses.HIPlotOptions
import com.highsoft.highcharts.common.hichartsclasses.HISeries
import com.highsoft.highcharts.common.hichartsclasses.HIShadowOptionsObject
import com.highsoft.highcharts.common.hichartsclasses.HISolidgauge
import com.highsoft.highcharts.common.hichartsclasses.HITitle
import com.highsoft.highcharts.common.hichartsclasses.HITooltip
import com.highsoft.highcharts.common.hichartsclasses.HIXAxis
import com.highsoft.highcharts.common.hichartsclasses.HIYAxis
import com.highsoft.highcharts.core.HIChartView
import com.highsoft.highcharts.core.HIFunction
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Locale
import java.util.TimeZone

@SuppressLint("RestrictedApi")
class GaugeChartActivity : ComponentActivity() {

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gaugechart)


    }

    companion object {

        fun setupChart(chartView: HIChartView, index: Int) {

            val responseJson: JSONObject =  JSONObject(WeatherApiResponseHolder.apiResponse)
            val nextArray = responseJson.getJSONArray("next")
            val populateData = nextArray.getJSONObject(index);
            val values = populateData.getJSONObject("values")
            val humidity = values.getDouble("humidity")
            val cloudCover = values.getDouble("cloudCover")
            val precipitation = values.getDouble("precipitationProbability")
            val options = HIOptions()

            val chart = HIChart().apply {
                type = "solidgauge"
                events = HIEvents().apply {
                    render = HIFunction(renderIconsString)
                }
            }
            options.chart = chart

            val title = HITitle().apply {
                text = "Stat Summary"
                style = HICSSObject().apply { fontSize = "24px" }
            }
            options.title = title

            val tooltip = HITooltip().apply {
                borderWidth = 0
                backgroundColor = HIColor.initWithName("none")
                shadow = null
                style = HICSSObject().apply { fontSize = "16px" }
                pointFormat = "{series.name}<br><span style=\"font-size:2em; color: {point.color}; font-weight: bold\">{point.y}%</span>"
                positioner = HIFunction(
                    "function (labelWidth) {" +
                            "   return {" +
                            "       x: (this.chart.chartWidth - labelWidth) /2," +
                            "       y: (this.chart.plotHeight / 2) + 15" +
                            "   };" +
                            "}"
                )
            }
            options.tooltip = tooltip

            val pane = HIPane().apply {
                startAngle = 0
                endAngle = 360
                background = ArrayList(
                    listOf(
                        HIBackground().apply {
                            outerRadius = "112%"
                            innerRadius = "88%"
                            backgroundColor = HIColor.initWithRGBA(130,238,106, 0.35)
                            borderWidth = 0
                        },
                        HIBackground().apply {
                            outerRadius = "87%"
                            innerRadius = "63%"
                            backgroundColor = HIColor.initWithRGBA(106,165,231, 0.35)
                            borderWidth = 0
                        },
                        HIBackground().apply {
                            outerRadius = "62%"
                            innerRadius = "38%"
                            backgroundColor = HIColor.initWithRGBA(255,129,93, 0.35)
                            borderWidth = 0
                        }
                    )
                )
            }
            options.pane = ArrayList(listOf(pane))

            val yAxis = HIYAxis().apply {
                min = 0
                max = 100
                lineWidth = 0
                tickPositions = ArrayList<Number>() // Remove ticks
            }
            options.yAxis = ArrayList(listOf(yAxis))

            val plotOptions = HIPlotOptions().apply {
                solidgauge = HISolidgauge().apply {
                    dataLabels = ArrayList(listOf(HIDataLabels().apply { enabled = false }))
                    linecap = "round"
                    stickyTracking = false
                    rounded = true
                }
            }
            options.plotOptions = plotOptions

            val solidgauge1 = HISolidgauge().apply {
                name = "Move"
                data = ArrayList(
                    listOf(
                        HIData().apply {
                            color = HIColor.initWithRGB(130,238,106)
                            radius = "112%"
                            innerRadius = "88%"
                            y = cloudCover
                        }
                    )
                )
            }

            val solidgauge2 = HISolidgauge().apply {
                name = "Exercise"
                data = ArrayList(
                    listOf(
                        HIData().apply {
                            color = HIColor.initWithRGB(106,165,231)
                            radius = "87%"
                            innerRadius = "63%"
                            y = precipitation
                        }
                    )
                )
            }

            val solidgauge3 = HISolidgauge().apply {
                name = "Stand"
                data = ArrayList(
                    listOf(
                        HIData().apply {
                            color = HIColor.initWithRGB(255,129,93)
                            radius = "62%"
                            innerRadius = "38%"
                            y = humidity
                        }
                    )
                )
            }

            options.series = ArrayList(
                listOf<HISeries>(
                    solidgauge1,
                    solidgauge2,
                    solidgauge3
                )
            )

            chartView.options = options

        }
        private val renderIconsString = "function renderIcons() {" +
                "                            if(!this.series[0].icon) {" +
                "                               this.series[0].icon = this.renderer.path(['M', -8, 0, 'L', 8, 0, 'M', 0, -8, 'L', 8, 0, 0, 8]).attr({'stroke': '#303030','stroke-linecap': 'round','stroke-linejoin': 'round','stroke-width': 2,'zIndex': 10}).add(this.series[2].group);}this.series[0].icon.translate(this.chartWidth / 2 - 10,this.plotHeight / 2 - this.series[0].points[0].shapeArgs.innerR -(this.series[0].points[0].shapeArgs.r - this.series[0].points[0].shapeArgs.innerR) / 2); if(!this.series[1].icon) {this.series[1].icon = this.renderer.path(['M', -8, 0, 'L', 8, 0, 'M', 0, -8, 'L', 8, 0, 0, 8,'M', 8, -8, 'L', 16, 0, 8, 8]).attr({'stroke': '#ffffff','stroke-linecap': 'round','stroke-linejoin': 'round','stroke-width': 2,'zIndex': 10}).add(this.series[2].group);}this.series[1].icon.translate(this.chartWidth / 2 - 10,this.plotHeight / 2 - this.series[1].points[0].shapeArgs.innerR -(this.series[1].points[0].shapeArgs.r - this.series[1].points[0].shapeArgs.innerR) / 2); if(!this.series[2].icon) {this.series[2].icon = this.renderer.path(['M', 0, 8, 'L', 0, -8, 'M', -8, 0, 'L', 0, -8, 8, 0]).attr({'stroke': '#303030','stroke-linecap': 'round','stroke-linejoin': 'round','stroke-width': 2,'zIndex': 10}).add(this.series[2].group);}this.series[2].icon.translate(this.chartWidth / 2 - 10,this.plotHeight / 2 - this.series[2].points[0].shapeArgs.innerR -(this.series[2].points[0].shapeArgs.r - this.series[2].points[0].shapeArgs.innerR) / 2);}"
    }

}
