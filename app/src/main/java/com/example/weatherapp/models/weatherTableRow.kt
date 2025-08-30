package models

import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import com.example.weatherapp.DetailsActivity
import com.example.weatherapp.R

class WeatherTableRow(
    index: Int,
    context: Context,
    date: String,
    minTemp: String,
    iconResId: Int,
    maxTemp: String
) : TableRow(context) {

    init {
        this.setBackgroundColor(android.graphics.Color.parseColor("#272727"))
        this.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )

        val parentLinearLayout = LinearLayout(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }

        val dateLinearLayout = createTextView(context, date, 82)
        parentLinearLayout.addView(dateLinearLayout)

        val iconLinearLayout = createImageView(context, iconResId, 85, 34)
        parentLinearLayout.addView(iconLinearLayout)

        val minTempLinearLayout = createTextView(context, minTemp, 80)
        parentLinearLayout.addView(minTempLinearLayout)

        val maxTempLinearLayout = createTextView(context, maxTemp, 100)
        parentLinearLayout.addView(maxTempLinearLayout)

        this.addView(parentLinearLayout)

        // Row-level click listener
    }

    private fun createTextView(context: Context, text: String, widthDp: Int): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.HORIZONTAL

            val textView = TextView(context).apply {
                this.text = text
                this.layoutParams = LayoutParams(
                    widthDp.dpToPx(),
                    LayoutParams.WRAP_CONTENT
                )
                this.setPadding(8, 8, 8, 8)
                this.gravity = Gravity.CENTER
                this.setTextColor(android.graphics.Color.WHITE)
            }
            this.addView(textView)
        }
    }

    private fun createImageView(context: Context, iconResId: Int, widthDp: Int, heightDp: Int): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.HORIZONTAL

            val imageView = ImageView(context).apply {
                this.setImageResource(iconResId)
                this.layoutParams = LayoutParams(
                    widthDp.dpToPx(),
                    heightDp.dpToPx()
                )
            }
            this.addView(imageView)
        }
    }

    private fun Int.dpToPx(): Int {
        val density = context.resources.displayMetrics.density
        return (this * density).toInt()
    }
}
