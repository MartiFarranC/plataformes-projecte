package com.example.testposturai.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding

object UiKit {
    val colorBackground: Int = Color.parseColor("#F4F6FA")
    val colorSurface: Int = Color.parseColor("#FFFFFF")
    val colorPrimary: Int = Color.parseColor("#1F5EFF")
    val colorPrimaryDark: Int = Color.parseColor("#1747BF")
    val colorSecondaryText: Int = Color.parseColor("#5B667A")
    val colorPrimaryText: Int = Color.parseColor("#1C2434")
    val colorSuccess: Int = Color.parseColor("#1F9D63")
    val colorDanger: Int = Color.parseColor("#D92D20")
    val colorBorder: Int = Color.parseColor("#D9DFEA")

    fun dp(context: Context, value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    fun styleScreen(container: LinearLayout) {
        container.setBackgroundColor(colorBackground)
        container.setPadding(dp(container.context, 20))
    }

    fun styleCard(container: LinearLayout) {
        container.background = roundedDrawable(colorSurface, colorBorder, 1, 18f)
        container.elevation = dp(container.context, 2).toFloat()
        container.setPadding(dp(container.context, 16))
    }

    fun styleTitle(textView: TextView) {
        textView.setTextColor(colorPrimaryText)
        textView.setTypeface(Typeface.DEFAULT_BOLD)
        textView.textSize = 26f
    }

    fun styleSubtitle(textView: TextView) {
        textView.setTextColor(colorSecondaryText)
        textView.textSize = 15f
    }

    fun styleInput(editText: EditText) {
        editText.setTextColor(colorPrimaryText)
        editText.setHintTextColor(colorSecondaryText)
        editText.textSize = 15f
        editText.setPadding(
            dp(editText.context, 14),
            dp(editText.context, 12),
            dp(editText.context, 14),
            dp(editText.context, 12)
        )
        editText.background = roundedDrawable(colorSurface, colorBorder, 1, 12f)
    }

    fun stylePrimaryButton(button: Button) {
        button.setBackgroundColor(Color.TRANSPARENT)
        button.background = roundedDrawable(colorPrimary, colorPrimary, 0, 12f)
        button.setTextColor(Color.WHITE)
        button.textSize = 15f
        button.setTypeface(Typeface.DEFAULT_BOLD)
        button.minHeight = dp(button.context, 48)
    }

    fun styleSecondaryButton(button: Button) {
        button.setBackgroundColor(Color.TRANSPARENT)
        button.background = roundedDrawable(colorSurface, colorBorder, 1, 12f)
        button.setTextColor(colorPrimaryText)
        button.textSize = 15f
        button.setTypeface(Typeface.DEFAULT_BOLD)
        button.minHeight = dp(button.context, 48)
    }

    fun roundedDrawable(fill: Int, strokeColor: Int, strokeWidthDp: Int, radiusDp: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp
            setColor(fill)
            if (strokeWidthDp > 0) {
                setStroke(strokeWidthDp, strokeColor)
            }
        }
    }
}
