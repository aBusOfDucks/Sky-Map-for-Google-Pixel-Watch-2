package com.example.skymap.presentation

import Converter
import androidx.compose.ui.graphics.Color
import eclipticToEquatorial
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

private const val EPS = 1e-4

private const val e = 0.054881 // rad
private const val n1 = 13.17639646 // deg / day
private const val n2 = 13.06499295 // deg / day
private const val n3 = 13.22935027 // deg / day
private const val lambda0 = 218.322 // deg
private const val M0 = 134.916 // deg
private const val F0 = 93.284 // deg
private const val i = 5.161 // deg

val MOON_DARK_COLOR = Color(40, 40, 40)

// Note: phase should be in (-PI, PI].
// phase == 0 means new Moon
// phase increases over time
// When angle == 0, the waxing crescent points to the east (on the skymap)
// When angle == PI/2, the waxing crescent points to the south (on the skymap)
class Moon(
    val phase : Float,
    val angle : Float,
    azimuth : Double,
    altitude : Double
) : SkyPoint(azimuth,altitude) {

    // Note: "left" means left from the perspective of the northern hemisphere

    /** Where on the moon face is the sunrise on the moon, the "leftmost" lit part */
    fun getWaxPoint() : Float {
        if (abs(phase) < EPS) {
            // The Moon is new
            return 1f
        }
        if (abs(phase) > PI.toFloat() - EPS || phase < 0f) {
            // The Moon is full or waning
            return -1f
        }
        // The Moon is waxing
        return cos(phase)
    }

    /** Where on the Moon face is the sunset on the Moon, the "rightmost" lit part */
    fun getWanePoint() : Float {
        if (abs(phase) < EPS || abs(phase) > PI.toFloat() - EPS || phase >= 0f) {
            // The Moon is new, full or waxing
            return 1f
        }
        // The Moon is waning
        return -cos(phase)
    }
}

private fun calculateMoonPosition(latitude : Double, longitude : Double) : GeocentricEclipticCoordinates {
    val time = zonedDateTimeNow()
    val converter = Converter(latitude, longitude, time)

    val JED = converter.getJulianDate()
    val deltaT = JED - J2000 // days

    var lambda = lambda0 + n1 * deltaT // degrees
    lambda %= 360
    lambda = toRadians(lambda)

    var M = M0 + n2 * deltaT // degrees
    M %= 360
    M = toRadians(M)

    var F1 = F0 + n3 * deltaT // degrees
    F1 %= 360
    F1 = toRadians(F1)

    var lambdaS = 280.459 + 0.98564736 * deltaT // degrees
    lambdaS %= 360
    lambdaS = toRadians(lambdaS)

    val D = lambda - lambdaS // rad

    // radians
    val q1 = 2 * e * sin(M) + 1.430 * e * e * sin (2 * M)
    val q2 = 0.422 * e * sin(2 * D - M)
    val q3 = 0.211 * e * (sin(2 * D) - 0.066 * sin(D))

    var MS = 357.529 + 0.98560028 * deltaT // degrees
    MS %= 360
    MS = toRadians(MS)

    val q4 = - 0.051 * e * sin(MS)
    val q5 = - 0.038 * e * sin(2 * F1)

    // ecliptic longitude
    lambda += (q1 + q2 + q3 + q4 + q5) // radians

    // mean argument of latitude
    val F = F1 + q1 + q2 + q3 + q4 + q5 // radians

    val sinBeta = sin(toRadians(i)) * sin(F)
    val beta = asin(sinBeta)

    return GeocentricEclipticCoordinates(lambda, beta)
}

fun calculateMoon(latitude : Double, longitude : Double) : Moon {
    val moonEclipicCoordinates = calculateMoonPosition(latitude, longitude)
    val moonEquatorialCoordinates = eclipticToEquatorial(moonEclipicCoordinates)
    val converter = Converter(latitude, longitude, zonedDateTimeNow())
    val horizontalPositions: GeocentricHorizontalCoordinates = converter.equatorialToHorizontal(moonEquatorialCoordinates)

    var moon = Moon(PI.toFloat() / 4, 0f, horizontalPositions.azimuth, horizontalPositions.altitude)

    return moon
}