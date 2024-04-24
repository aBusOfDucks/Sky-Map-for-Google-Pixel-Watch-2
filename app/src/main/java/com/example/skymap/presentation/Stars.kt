package com.example.skymap.presentation

import android.util.Log
import androidx.compose.ui.graphics.Color
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import Converter

open class Star(mag : Double, azimuth: Double, altitude: Double, id: Int) : SkyPoint(azimuth, altitude) {
    private var size: Int = 1
    private var minimumZoom: Int = 0
    val id: Int = id;

    init {
        size = BRIGHTNESS_MAX - mag.toInt()
        minimumZoom = mag.toInt() - 1
    }

    open fun getColor(zoom: Float, colorSetting: Int, brightnessFactor : Float): Color {

        val starColor =
            when(colorSetting) {
                WHITE_MODE -> Color.White
                RED_MODE -> Color.Red
                else -> Color.White
            }

        val alpha = saturate((zoom + brightnessFactor - minimumZoom) / (MAX_ZOOM - minimumZoom))

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
fun calculateStars(latitude: Double, longitude: Double, starsArray: com.google.gson.JsonArray?): HashMap<Int,Star> {
    Log.d("Star", "Calculating stars $latitude $longitude")

    val localDateTime = LocalDateTime.now(ZoneOffset.UTC)
    val zoneId = ZoneId.of("GMT")
    val zonedDateTime = ZonedDateTime.of(localDateTime, zoneId)
    val converter = Converter(latitude, longitude, zonedDateTime)
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

        val horizontalCoordinates = converter.equatorialToHorizontal(equatorialCoordinates)

        if (horizontalCoordinates.altitude > 0) {
            stars[id] = Star(mag, horizontalCoordinates.azimuth, horizontalCoordinates.altitude, id)
        }
    }
    return stars
}
fun findStarById(stars: HashMap<Int,Star>, x: Int): Star? {
    return stars[x]
}