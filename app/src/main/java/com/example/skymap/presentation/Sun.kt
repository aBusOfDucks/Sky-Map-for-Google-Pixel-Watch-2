package com.example.skymap.presentation

import Converter
import getPlanetObjects
import androidx.compose.ui.graphics.Color
import calculateGeocentricPositions
import com.google.gson.JsonArray
import getJulianDate

class Sun(azimuth : Double, altitude : Double) : SkyPoint(azimuth,altitude) {
    override val color : Color = Color(255,255,0)
    override val colorRedMode: Color = Color(255, 150, 0)
}

fun calculateSun(
    latitude: Double,
    longitude: Double,
    planetArray: JsonArray?
): Sun {
    val planetObjects = getPlanetObjects(planetArray)
    val earth = planetObjects[2]

    val zonedDateTime = zonedDateTimeNow()

    val converter = Converter(latitude, longitude, zonedDateTime)
    val JED = getJulianDate()

    val earthPositions: HeliocentricEclipticCoordinates = earth.calculateHeliocentricPositions(JED)
    val heliocentricSunPositions = HeliocentricEclipticCoordinates(0.0, 0.0, 0.0)

    val equatorialPositions: GeocentricEquatorialCoordinates = calculateGeocentricPositions(heliocentricSunPositions, earthPositions)
    val horizontalPositions: GeocentricHorizontalCoordinates = converter.equatorialToHorizontal(equatorialPositions)

    return Sun(horizontalPositions.azimuth, horizontalPositions.altitude)
}