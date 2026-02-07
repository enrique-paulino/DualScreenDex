package com.enrpau.dualscreendex

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import kotlin.random.Random
import androidx.core.graphics.withTranslation
import kotlin.math.cos
import kotlin.math.sin

data class AppTheme(
    val id: String,
    val displayName: String,

    // global colors
    val windowBackground: Int,
    val contentBackground: Int,

    // text colors
    val listTextColor: Int,
    val labelTextColor: Int,

    // search bar
    val searchBoxColor: Int,
    val searchTextColor: Int,
    val searchHintColor: Int,
    val searchCornerRadius: Float,
    val searchMarginHorizontal: Int,
    val searchStrokeColor: Int,
    val searchStrokeWidth: Int,

    // header & content
    val headerTextColor: Int,
    val subTextColor: Int,
    val gridBackgroundColor: Int,
    val cardCornerRadius: Float,

    // flags
    val isRetroScreen: Boolean,
    val isGradientBg: Boolean
)

object ThemeManager {
    var currentTheme: AppTheme = createDynamicTheme()

    fun loadTheme(context: Context) {
        val prefs = context.getSharedPreferences("DualDexPrefs", Context.MODE_PRIVATE)
        val themeId = prefs.getString("SELECTED_THEME_ID", "dynamic") ?: "dynamic"

        currentTheme = when (themeId) {
            "oled" -> AppTheme(
                id = "oled",
                displayName = "OLED Black",
                windowBackground = Color.BLACK,
                contentBackground = Color.BLACK,

                listTextColor = Color.WHITE,
                labelTextColor = Color.WHITE,

                searchBoxColor = Color.BLACK,
                searchTextColor = Color.WHITE,
                searchHintColor = Color.GRAY,
                searchCornerRadius = 12f,
                searchMarginHorizontal = 16,
                searchStrokeColor = Color.WHITE,
                searchStrokeWidth = 2,

                headerTextColor = Color.WHITE,
                subTextColor = Color.LTGRAY,
                gridBackgroundColor = "#121212".toColorInt(),
                cardCornerRadius = 12f,

                isRetroScreen = false,
                isGradientBg = false
            )
            "red" -> AppTheme(
                id = "red",
                displayName = "Classic Red",
                windowBackground = "#DC0A2D".toColorInt(),
                contentBackground = "#98CB98".toColorInt(),

                listTextColor = "#0F380F".toColorInt(),
                labelTextColor = "#306230".toColorInt(),

                searchBoxColor = "#8B0000".toColorInt(),
                searchTextColor = "#98CB98".toColorInt(),
                searchHintColor = "#CC98CB98".toColorInt(),
                searchCornerRadius = 0f,
                searchMarginHorizontal = 24,
                searchStrokeColor = Color.TRANSPARENT,
                searchStrokeWidth = 0,

                headerTextColor = "#0F380F".toColorInt(),
                subTextColor = "#306230".toColorInt(),
                gridBackgroundColor = "#98CB98".toColorInt(),
                cardCornerRadius = 0f,

                isRetroScreen = true,
                isGradientBg = false
            )
            "magical" -> AppTheme(
                id = "magical",
                displayName = "Magical Pastel",
                windowBackground = "#F3E5F5".toColorInt(),
                contentBackground = Color.TRANSPARENT,

                listTextColor = "#4A148C".toColorInt(),
                labelTextColor = "#4A148C".toColorInt(),

                searchBoxColor = Color.WHITE,
                searchTextColor = "#4A148C".toColorInt(),
                searchHintColor = "#884A148C".toColorInt(),
                searchCornerRadius = 50f,
                searchMarginHorizontal = 32,
                searchStrokeColor = Color.TRANSPARENT,
                searchStrokeWidth = 0,

                headerTextColor = "#4A148C".toColorInt(),
                subTextColor = "#7B1FA2".toColorInt(),
                gridBackgroundColor = "#99FFFFFF".toColorInt(),
                cardCornerRadius = 24f,

                isRetroScreen = false,
                isGradientBg = true
            )
            else -> createDynamicTheme()
        }
    }

    private fun createDynamicTheme() = AppTheme(
        id = "dynamic",
        displayName = "Dynamic",
        windowBackground = Color.WHITE,
        contentBackground = Color.WHITE,
        listTextColor = Color.BLACK,
        labelTextColor = Color.LTGRAY,
        searchBoxColor = "#F5F5F5".toColorInt(),
        searchTextColor = Color.BLACK,
        searchHintColor = Color.GRAY,
        searchCornerRadius = 50f,
        searchMarginHorizontal = 16,
        searchStrokeColor = Color.TRANSPARENT,
        searchStrokeWidth = 0,
        headerTextColor = Color.BLACK,
        subTextColor = Color.GRAY,
        gridBackgroundColor = "#FAFAFA".toColorInt(),
        cardCornerRadius = 16f,
        isRetroScreen = false,
        isGradientBg = false
    )

    fun createScanlineDrawable(context: Context, withBorder: Boolean): Drawable {
        val theme = ThemeManager.currentTheme
        val bitmap = createBitmap(1, 4)
        val canvas = Canvas(bitmap)

        canvas.drawColor(theme.contentBackground)

        // paint scan lines
        val paint = Paint()
        paint.color = ColorUtils.blendARGB(theme.contentBackground, Color.BLACK, 0.1f)
        canvas.drawRect(0f, 3f, 1f, 4f, paint)

        // bitmap repeats scan line
        val drawable = bitmap.toDrawable(context.resources)
        drawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        drawable.isFilterBitmap = false

        // bezel
        if (withBorder) {
            val border = android.graphics.drawable.GradientDrawable()
            border.setStroke(16, "#8B0000".toColorInt())
            return android.graphics.drawable.LayerDrawable(arrayOf(drawable, border))
        } else {
            return drawable
        }
    }

    class StarryDrawable : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val stars = ArrayList<Star>()
        private var isInitialized = false

        // gradient colors
        private val colors = intArrayOf(
            "#E1BEE7".toColorInt(), // Purple
            "#F8BBD0".toColorInt(), // Pink
            "#BBDEFB".toColorInt()  // Blue
        )

        private data class Star(
            val x: Float,
            val y: Float,
            val size: Float,
            val alpha: Int,
            val rotation: Float,
            val isOutline: Boolean // New: Some stars are just outlines!
        )

        override fun draw(canvas: Canvas) {
            val width = bounds.width().toFloat()
            val height = bounds.height().toFloat()

            val shader = android.graphics.LinearGradient(
                0f, 0f, width, height,
                colors, null, Shader.TileMode.CLAMP
            )
            paint.shader = shader
            paint.style = Paint.Style.FILL
            paint.alpha = 255
            canvas.drawRect(0f, 0f, width, height, paint)

            // initialize stars
            if (!isInitialized && width > 0 && height > 0) {
                stars.clear()
                for (i in 0..60) { // Slightly fewer stars since they are bigger now
                    val x = Random.nextFloat() * width
                    val y = Random.nextFloat() * height

                    // Size between 10px and 25px
                    val size = Random.nextFloat() * 15f + 10f
                    val a = Random.nextInt(100, 200) // Opacity
                    val rot = Random.nextFloat() * 360f // Random rotation
                    val outline = Random.nextBoolean() // 50/50 chance of being an outline

                    stars.add(Star(x, y, size, a, rot, outline))
                }
                isInitialized = true
            }

            paint.shader = null
            paint.color = Color.WHITE

            for (star in stars) {
                paint.alpha = star.alpha

                // Save canvas state to handle rotation
                canvas.withTranslation(star.x, star.y) {
                    rotate(star.rotation)

                    if (star.isOutline) {
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 3f
                    } else {
                        paint.style = Paint.Style.FILL
                    }

                    // draw star path
                    val path = createStarPath(star.size)
                    drawPath(path, paint)

                }
            }
        }

        private fun createStarPath(radius: Float): android.graphics.Path {
            val path = android.graphics.Path()
            val innerRadius = radius / 2.5f

            for (i in 0 until 10) {
                val angle = Math.toRadians((i * 36 - 90).toDouble())
                val r = if (i % 2 == 0) radius else innerRadius
                val x = (r * cos(angle)).toFloat()
                val y = (r * sin(angle)).toFloat()

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.close()
            return path
        }

        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
    }
}