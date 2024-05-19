package com.example.skymap.presentation

import getPlanetObjects
import androidx.compose.ui.graphics.Color
import calculateGeocentricPositions
import com.google.gson.JsonArray
import equatorialToHorizontal
import getJulianDate

class Sun(azimuth : Double, altitude : Double) : SkyPoint(azimuth,altitude) {
    override val color : Color = Color(255,255,0)
    override val colorRedMode: Color = Color(255, 150, 0)

    fun getGeocentricHorizontalCoordinates() : GeocentricHorizontalCoordinates {
        return GeocentricHorizontalCoordinates(altitude, azimuth)
    }
}

fun calculateSun(
    latitude: Double,
    longitude: Double,
    planetArray: JsonArray?
): Sun {
    val planetObjects = getPlanetObjects(planetArray)
    val earth = planetObjects[2]

    val JED = getJulianDate()

    val earthPositions: HeliocentricEclipticCoordinates = earth.calculateHeliocentricPositions(JED)
    val heliocentricSunPositions = HeliocentricEclipticCoordinates(0.0, 0.0, 0.0)

    val equatorialPositions: GeocentricEquatorialCoordinates = calculateGeocentricPositions(heliocentricSunPositions, earthPositions)
    val horizontalPositions: GeocentricHorizontalCoordinates = equatorialToHorizontal(latitude, longitude, equatorialPositions)

    return Sun(horizontalPositions.azimuth, horizontalPositions.altitude)
}