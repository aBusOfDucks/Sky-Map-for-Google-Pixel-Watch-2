package com.example.skymap.presentation

import Converter
import androidx.compose.ui.graphics.Color
import calculateGeocentricPositions
import getPlanetObjects
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

val colorMap = mapOf(
    "Mercury" to Color(0.5f,0.5f,0.5f,1f),
    "Venus" to Color(0.5f,0.48f,0.45f,1f),
    "Mars" to Color(0.55f,0.42f,0.4f,1f),
    "Earth" to Color(0.55f,0.42f,0.4f,1f),
    "Jupiter" to Color(0.7f,0.65f,0.6f,1f),
    "Saturn" to Color(0.7f,0.7f,0.6f,1f),
    "Uranus" to Color(0.6f,0.65f,0.7f,1f),
    "Neptune" to Color(0.55f,0.6f,0.75f,1f)
)

val symbolMap: Map<String, Char> = mapOf(
    "Mercury" to Char(0x263F),
    "Venus" to Char(0x2640),
    "Mars" to Char(0x2642),
    "Earth" to Char(0x2642),
    "Jupiter" to Char(0x2643),
    "Saturn" to Char(0x2644),
    "Uranus" to Char(0x26E2),
    "Neptune" to Char(0x2646)
)

fun calculatePlanets(
    latitude: Double,
    longitude: Double,
    planetArray: com.google.gson.JsonArray?
): ArrayList<Planet> {

    val planetObjects = getPlanetObjects(planetArray)

    val earth = planetObjects[2]
    planetObjects.removeAt(2)

    val zonedDateTime = zonedDateTimeNow()

    val converter = Converter(latitude, longitude, zonedDateTime)
    val JED = converter.getJulianDate()

    val earthPositions: HeliocentricEclipticCoordinates = earth.calculateHeliocentricPositions(JED)

    planetObjects.forEach() { planet ->
        val heliocentricPositions: HeliocentricEclipticCoordinates = planet.calculateHeliocentricPositions(JED)
        val equatorialPositions: GeocentricEquatorialCoordinates = calculateGeocentricPositions(heliocentricPositions, earthPositions)
        val horizontalPositions: GeocentricHorizontalCoordinates = converter.equatorialToHorizontal(equatorialPositions)

        planet.setHorizontal(horizontalPositions)
    }

    return planetObjects
}

class Planet(
    val name : String,
    val semiMajorAxis0: Double,
    val semiMajorAxisRateOfChange: Double,
    val eccentricity0: Double,
    val eccentricityRateOfChange: Double,
    val inclination0: Double,
    val inclinationRateOfChange: Double,
    val meanLongitude0: Double,
    val meanLongitudeRateOfChange: Double,
    val longitudeOfPerihelion0: Double,
    val longitudeOfPerihelionRateOfChange: Double,
    val longitudeOfAscendingNode0: Double,
    val longitudeOfAscendingNodeRateOfChange: Double,
    val symbol: Char = symbolMap[name]!!,
    override val color: Color = colorMap[name]!!,
    azimuth: Double = 0.0,
    altitude: Double = 0.0
): SkyPoint(azimuth, altitude){

    private fun calculateEccentricAnomaly(M: Double, e: Double, E_n: Double): Double {
        val deltaM = M - (E_n - toDegrees(e) * sin(toRadians(E_n)))
        val deltaE = deltaM / (1 - e * cos(toRadians(E_n)))

        if (abs(deltaE) < 1.0e-6) {
            return E_n
        } else {
            return calculateEccentricAnomaly(M, e, E_n + deltaE)
        }
    }

    private fun calculateEclipticCoordinates(
        omega: Double,
        Omega: Double,
        I: Double,
        helioCoord: HeliocentricOrbitalCoordinates
    )
            : HeliocentricEclipticCoordinates {
        val a = cos(omega) * cos(Omega) - sin(omega) * sin(Omega) * cos(I)
        val b = -sin(omega) * cos(Omega) - cos(omega) * sin(Omega) * cos(I)
        val x = a * helioCoord.x + b * helioCoord.y

        val c = cos(omega) * sin(Omega) + sin(omega) * cos(Omega) * cos(I)
        val d = -sin(omega) * sin(Omega) + cos(omega) * cos(Omega) * cos(I)
        val y = c * helioCoord.x + d * helioCoord.y

        val z = sin(omega) * sin(I) * helioCoord.x + cos(omega) * sin(I) * helioCoord.y

        return HeliocentricEclipticCoordinates(x, y, z)
    }

    fun calculateHeliocentricPositions(JD: Double): HeliocentricEclipticCoordinates {

        val T = (JD - 2451545.0) / 36525

        val a = semiMajorAxis0 + semiMajorAxisRateOfChange * T
        val e = eccentricity0 + eccentricityRateOfChange * T
        val I = inclination0 + inclinationRateOfChange * T
        val L = meanLongitude0 + meanLongitudeRateOfChange * T
        val longitudeOfPerihelion = longitudeOfPerihelion0 + longitudeOfPerihelionRateOfChange * T
        val longitudeOfAscendingNode =
            longitudeOfAscendingNode0 + longitudeOfAscendingNodeRateOfChange * T

        val argumentOfPerihelion = longitudeOfPerihelion - longitudeOfAscendingNode

        var M = L - longitudeOfPerihelion
        M %= 360
        if (M > 180) M -= 360
        if (M < -180) M += 360

        val E_0 = M - toDegrees(e) * sin(toRadians(M))

        // in degrees
        val E = calculateEccentricAnomaly(M, e, E_0)

        // heliocentric coordinates
        val x_h = a * (cos(toRadians(E)) - e)
        val y_h = a * sqrt(1 - e * e) * sin(toRadians(E))

        // odległość od słońca
        //val r_h = sqrt(x_h * x_h + y_h * y_h)

        val heliocentricOrbitalCoordinates = HeliocentricOrbitalCoordinates(x_h, y_h, 0.0)

        // ecliptic coordinates
        val heliocentricEclipticCoordinates = calculateEclipticCoordinates(
            toRadians(argumentOfPerihelion),
            toRadians(longitudeOfAscendingNode),
            toRadians(I),
            heliocentricOrbitalCoordinates
        )

        return heliocentricEclipticCoordinates
    }

    fun setHorizontal(horizontalPositions: GeocentricHorizontalCoordinates) {
        azimuth = horizontalPositions.azimuth
        altitude = horizontalPositions.altitude
        reproject()
    }
}


