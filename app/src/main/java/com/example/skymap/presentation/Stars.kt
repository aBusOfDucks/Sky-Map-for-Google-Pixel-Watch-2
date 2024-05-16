package com.example.skymap.presentation

import android.util.Log
import androidx.compose.ui.graphics.Color
import equatorialToHorizontal

open class Star(private val mag : Double, azimuth: Double, altitude: Double, id: Int) : SkyPoint(azimuth, altitude) {
    private var size: Int = 1
    val id: Int = id;

    init {
        size = BRIGHTNESS_MAX - mag.toInt()
    }

    open fun getColor(zoom: Float, colorSetting: Int, brightness : Float, scaleFactor : Float): Color {

        val starColor =
            when(colorSetting) {
                WHITE_MODE -> Color.White
                RED_MODE -> Color.Red
                else -> Color.White
            }

        val alpha = saturate(
            scaleFactor * unscaledStarAlpha(brightness, zoom, mag.toFloat())
        )

        return starColor.copy(alpha = alpha)
    }

    /** Makes sure a value doesn't go below 0 or above 1 */
    private fun saturate(v : Float) : Float {
        if (v < 0f)
            return 0f
        if (v > 1f)
            return 1f
        return v
    }
}

private const val BRIGHTNESS_INTERSECTION_POINT = 15f
private const val SLOPE =  1 / (BRIGHTNESS_INTERSECTION_POINT * 4f - 4f)

private fun unscaledStarAlpha(brightness: Float, zoom: Float, mag : Float) : Float {
    /*
    The alpha is calculated using the equation that satisfies the following conditions:
    1. The effective magnitude is equal to mag + brightness / 2.
    2. If the effective magnitude is 0, then alpha is 1.
    3. If the effective magnitude is 4 and zoom is 1, then alpha is 0
    4. If zoom == BRIGHTNESS_INTERSECTION_POINT, then alpha is 1
    5. If zoom is constant, then alpha is a linear function of the effective magnitude.
    6. If the effective magnitude is constant, then alpha is a linear function of zoom.
     */
    val effectiveMagnitude = mag + brightness * 0.5f
    return 1f + (zoom - BRIGHTNESS_INTERSECTION_POINT) * effectiveMagnitude * SLOPE
}

/** Returns the maximum alpha value that any star can have for this brightness setting */
fun maxStarAlpha(brightness: Float) : Float {
    return unscaledStarAlpha(brightness, MAX_ZOOM, 0f)
}

fun calculateStars(latitude: Double, longitude: Double, starsArray: com.google.gson.JsonArray?): HashMap<Int,Star> {
    Log.d("Star", "Calculating stars $latitude $longitude")

    val stars : HashMap<Int,Star> = HashMap()

    starsArray?.forEach { star ->
        val starJsonObject = star.asJsonObject
        val coordinates = starJsonObject.getAsJsonObject("coordinates")
        val dec: Double = coordinates.asJsonObject.getAsJsonPrimitive("dec").asDouble
        val ra: Double = coordinates.asJsonObject.getAsJsonPrimitive("ra").asDouble
        val mag: Double = starJsonObject.getAsJsonPrimitive("vmag").asDouble
        val id: Int = starJsonObject.getAsJsonPrimitive("id").asInt

        val equatorialCoordinates = GeocentricEquatorialCoordinates(
            rightAscension = ra,
            declination = dec
        )

        val horizontalCoordinates = equatorialToHorizontal(latitude, longitude, equatorialCoordinates)

        if (horizontalCoordinates.altitude > 0) {
            stars[id] = Star(mag, horizontalCoordinates.azimuth, horizontalCoordinates.altitude, id)
        }
    }
    return stars
}
fun findStarById(stars: HashMap<Int,Star>, x: Int): Star? {
    return stars[x]
}