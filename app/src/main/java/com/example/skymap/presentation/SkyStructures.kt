package com.example.skymap.presentation

import android.util.Log
import androidx.compose.ui.graphics.Color
import equatorialToHorizontal
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.random.Random

class SkyStructures(mag : Double, azimuth: Double, altitude: Double, id: Int, name: String) : Star(mag, azimuth, altitude, id) {
    var symbol: Char = Char(0x2728)
    val name = name;

    override fun getColor(zoom: Float, colorSetting: Int, brightness : Float, scaleFactor: Float): Color {
        return when(colorSetting) {
                WHITE_MODE -> Color.White
                RED_MODE -> Color.Red
                else -> Color.White
            }.copy(alpha = saturate(2 * unscaledStarAlpha(brightness, zoom, mag.toFloat())))
    }
}

fun calculateSkyStructures(latitude: Double, longitude: Double, skyStructuresArray: com.google.gson.JsonArray?): ArrayList<SkyStructures> {
    Log.d("Star", "Calculating stars $latitude $longitude")

    val skyStructures : ArrayList<SkyStructures> = ArrayList()

    skyStructuresArray?.forEach { ss ->
        val ssJsonObject = ss.asJsonObject
        val coordinates = ssJsonObject.getAsJsonObject("coordinates")
        val dec: Double = coordinates.asJsonObject.getAsJsonPrimitive("dec").asDouble
        val ra: Double = coordinates.asJsonObject.getAsJsonPrimitive("ra").asDouble
        val mag: Double = ssJsonObject.getAsJsonPrimitive("vmag").asDouble
        val name: String = ssJsonObject.getAsJsonPrimitive("name").asString

        val equatorialCoordinates = GeocentricEquatorialCoordinates(
            rightAscension = ra,
            declination = dec
        )

        val horizontalCoordinates = equatorialToHorizontal(latitude, longitude, equatorialCoordinates)

        if (horizontalCoordinates.altitude > 0) {
            skyStructures.add( SkyStructures(mag * 0.6f, horizontalCoordinates.azimuth, horizontalCoordinates.altitude, 0, name))
        }
    }
    return skyStructures
}