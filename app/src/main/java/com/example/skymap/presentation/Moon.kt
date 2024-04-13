package com.example.skymap.presentation

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

private const val EPS = 1e-4

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

fun calculateMoon() : Moon {
    //TODO: Add Moon calculation here
    return Moon(PI.toFloat() / 4, 0f, 0.0, 0.5)
}