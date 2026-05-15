package com.example.testposturai.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding

object UiKit {
    val colorBackground: Int = Color.parseColor("#F3F5F9")
    val colorBackgroundTop: Int = Color.parseColor("#FAFBFF")
    val colorSurface: Int = Color.parseColor("#FFFFFF")
    val colorSurfaceSoft: Int = Color.parseColor("#F7F9FC")
    val colorPrimary: Int = Color.parseColor("#1D4ED8")
    val colorPrimaryDark: Int = Color.parseColor("#1E3A8A")
    val colorSecondaryText: Int = Color.parseColor("#5F6B81")
    val colorPrimaryText: Int = Color.parseColor("#182033")
    val colorSuccess: Int = Color.parseColor("#1F9D63")
    val colorDanger: Int = Color.parseColor("#D92D20")
    val colorBorder: Int = Color.parseColor("#D6DEEA")
    val colorWarning: Int = Color.parseColor("#B54708")

    fun dp(context: Context, value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    fun styleScreen(container: LinearLayout) {
        container.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(colorBackgroundTop, colorBackground)
        )
        container.setPadding(dp(container.context, 20))
    }

    fun styleCard(container: LinearLayout) {
        container.background = roundedDrawable(colorSurface, colorBorder, 1, 22f)
        container.elevation = dp(container.context, 3).toFloat()
        container.setPadding(dp(container.context, 20))
    }

    fun styleTopBar(topBar: LinearLayout) {
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setPadding(0, 0, 0, dp(topBar.context, 2))
    }

    fun styleStatus(textView: TextView) {
        textView.setTextColor(colorSecondaryText)
        textView.textSize = 13f
        textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        textView.setPadding(
            dp(textView.context, 10),
            dp(textView.context, 6),
            dp(textView.context, 10),
            dp(textView.context, 6)
        )
        textView.background = roundedDrawable(colorSurfaceSoft, colorBorder, 1, 12f)
    }

    fun styleTitle(textView: TextView) {
        textView.setTextColor(colorPrimaryText)
        textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        textView.textSize = 24f
    }

    fun styleSubtitle(textView: TextView) {
        textView.setTextColor(colorSecondaryText)
        textView.textSize = 16f
        textView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL))
    }

    fun styleHeroTitle(textView: TextView) {
        styleTitle(textView)
        textView.textSize = 32f
    }

    fun styleInput(editText: EditText) {
        editText.setTextColor(colorPrimaryText)
        editText.setHintTextColor(colorSecondaryText)
        editText.textSize = 16f
        editText.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL))
        editText.setPadding(
            dp(editText.context, 14),
            dp(editText.context, 12),
            dp(editText.context, 14),
            dp(editText.context, 12)
        )
        editText.background = roundedDrawable(colorSurface, colorBorder, 1, 14f)
    }

    fun stylePrimaryButton(button: Button) {
        button.setBackgroundColor(Color.TRANSPARENT)
        button.background = roundedDrawable(colorPrimary, colorPrimary, 0, 14f)
        button.setTextColor(Color.WHITE)
        button.textSize = 16f
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        button.minHeight = dp(button.context, 52)
    }

    fun styleGhostButton(button: Button) {
        button.setBackgroundColor(Color.TRANSPARENT)
        button.background = roundedDrawable(colorSurfaceSoft, colorBorder, 1, 14f)
        button.setTextColor(colorPrimary)
        button.textSize = 16f
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        button.minHeight = dp(button.context, 52)
    }

    fun styleSecondaryButton(button: Button) {
        button.setBackgroundColor(Color.TRANSPARENT)
        button.background = roundedDrawable(colorSurface, colorBorder, 1, 14f)
        button.setTextColor(colorPrimaryText)
        button.textSize = 16f
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        button.minHeight = dp(button.context, 52)
    }

    fun styleDangerButton(button: Button) {
        button.setBackgroundColor(Color.TRANSPARENT)
        button.background = roundedDrawable(colorDanger, colorDanger, 0, 14f)
        button.setTextColor(Color.WHITE)
        button.textSize = 16f
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        button.minHeight = dp(button.context, 52)
    }

    fun styleBody(textView: TextView) {
        textView.setTextColor(colorPrimaryText)
        textView.textSize = 16f
        textView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL))
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
