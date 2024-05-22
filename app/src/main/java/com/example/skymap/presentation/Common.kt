package com.example.skymap.presentation

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.PI

const val ECLIPTIC_ANGLE = 0.40910517667
const val J2000 = 2451545.0

data class HeliocentricOrbitalCoordinates(val x: Double, val y: Double, val z: Double)

data class HeliocentricEclipticCoordinates(val x: Double, val y: Double, val z: Double)

// radians, degrees, au
data class GeocentricEquatorialCoordinates(val rightAscension: Double, val declination: Double, val r: Double = 0.0)

// radians, radians
data class GeocentricHorizontalCoordinates(val altitude: Double, val azimuth: Double)

// radians, radians, au
data class GeocentricEclipticCoordinates(val longitude: Double, val latitude: Double, val r: Double = 0.0)

fun toDegrees(n: Double) : Double {
    return 180 / PI * n
}

fun toRadians(n: Double) : Double {
    return PI / 180 * n
}

fun zonedDateTimeNow() : ZonedDateTime {
    val localDateTime = LocalDateTime.now(ZoneOffset.UTC)
    val zoneId = ZoneId.of("GMT")

    return ZonedDateTime.of(localDateTime, zoneId)
}