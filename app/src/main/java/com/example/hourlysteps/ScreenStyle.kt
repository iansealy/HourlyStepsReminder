package com.example.hourlysteps

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.roundToInt

internal object Palette {
    val background = Color.rgb(245, 247, 252)
    val surface = Color.WHITE
    val ink = Color.rgb(22, 32, 58)
    val muted = Color.rgb(77, 89, 115)
    val border = Color.rgb(221, 228, 241)
    val blue = Color.rgb(45, 69, 175)
    val paleBlue = Color.rgb(232, 237, 255)
    val navy = Color.rgb(27, 43, 95)
    val navyLight = Color.rgb(43, 64, 131)
    val heroText = Color.rgb(225, 233, 255)
    val lime = Color.rgb(218, 250, 147)
    val green = Color.rgb(42, 102, 58)
}

internal fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

@Suppress("DEPRECATION")
internal fun Activity.screen(contentId: Int): LinearLayout {
    if (Build.VERSION.SDK_INT in 30..34) {
        window.setDecorFitsSystemWindows(false)
    }

    val content = column().apply {
        setPadding(dp(20), dp(14), dp(20), dp(28))
        isFocusableInTouchMode = true
    }
    val scroll = ScrollView(this).apply {
        id = contentId
        isFillViewport = true
        isVerticalScrollBarEnabled = false
        setBackgroundColor(Palette.background)
        addView(content, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
        ))
        if (Build.VERSION.SDK_INT >= 30) {
            setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }
    }
    setContentView(scroll)
    return content
}

internal fun Context.column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

internal fun Context.row() = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }

internal fun LinearLayout.section(view: View, top: Int = 0) {
    val params = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = context.dp(top) }
    addView(view, params)
}

internal fun LinearLayout.stretch(view: View) {
    addView(view, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
}

internal fun Context.label(
    value: CharSequence,
    size: Float,
    color: Int = Palette.ink,
    weight: Int = 400
) = TextView(this).apply {
    text = value
    textSize = size
    setTextColor(color)
    typeface = Typeface.create(Typeface.DEFAULT, weight, false)
}

internal fun Context.shape(color: Int, radius: Int, stroke: Int? = null) =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        stroke?.let { setStroke(dp(1), it) }
    }

internal fun Context.card(padding: Int = 18) = column().apply {
    background = shape(Palette.surface, 22, Palette.border)
    setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
}

internal fun Context.action(title: CharSequence, onClick: () -> Unit) = Button(this).apply {
    text = title
    textSize = 14f
    isAllCaps = false
    typeface = Typeface.create(Typeface.DEFAULT, 700, false)
    setTextColor(Palette.blue)
    stateListAnimator = null
    minimumHeight = dp(48)
    minimumWidth = dp(48)
    setPadding(dp(14), dp(8), dp(14), dp(8))
    background = ripple(Palette.paleBlue, 16)
    setOnClickListener { onClick() }
}

internal fun Context.ripple(color: Int, radius: Int) = RippleDrawable(
    ColorStateList.valueOf(0x264360CB), shape(color, radius), shape(Color.WHITE, radius)
)

internal fun Context.meter(fill: Int, track: Int): ProgressBar {
    val layers = LayerDrawable(arrayOf(
        shape(track, 12), ClipDrawable(shape(fill, 12), Gravity.START, ClipDrawable.HORIZONTAL)
    )).apply {
        setId(0, android.R.id.background)
        setId(1, android.R.id.progress)
        setPaddingMode(LayerDrawable.PADDING_MODE_STACK)
    }
    return ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
        progressDrawable = layers
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
}

internal fun LinearLayout.addMeter(meter: ProgressBar, height: Int, top: Int) {
    addView(meter, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dp(height)).apply {
        topMargin = context.dp(top)
    })
}

internal fun LinearLayout.divider() {
    addView(View(context).apply { setBackgroundColor(Palette.border) },
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dp(1)))
}
