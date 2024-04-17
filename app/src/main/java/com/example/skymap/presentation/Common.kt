package com.example.skymap.presentation

import kotlin.math.PI

const val ECLIPTIC_ANGLE = 0.40910517667

data class HeliocentricOrbitalCoordinates(val x: Double, val y: Double, val z: Double)

data class HeliocentricEclipticCoordinates(val x: Double, val y: Double, val z: Double)

// radians, degrees
data class GeocentricEquatorialCoordinates(val rightAscension: Double, val declination: Double)

// radians, radians
data class GeocentricHorizontalCoordinates(val altitude: Double, val azimuth: Double)

data class GeocentricEclipticCoordinates(val longitude: Double, val latitude: Double)

fun toDegrees(n: Double) : Double {
    return 180 / PI * n
}

fun toRadians(n: Double) : Double {
    return PI / 180 * n
}
