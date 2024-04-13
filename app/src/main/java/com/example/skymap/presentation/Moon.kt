package com.example.skymap.presentation

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

// Note: phase should be in (-PI, PI].
// phase == 0 means new moon
// phase increases over time
// When angle == 0, the waxing crescent points to the right on the sky
// When angle == PI/2, the waxing crescent points up on the sky
class Moon(
    val phase : Float,
    val angle : Float,
    azimuth : Double,
    altitude : Double
) : SkyPoint(azimuth,altitude) {
    val EPS = 1e-4

    // Node: "left" means left means left from the perspective of the northern hemisphere

    // Where on the moon face is the sunrise on the moon, the "leftmost" lit part
    fun getWaxPoint() : Float {
        if (abs(phase) < EPS) {
            // The moon is new
            return 1f
        }
        if (abs(phase) > PI.toFloat() - EPS || phase < 0f) {
            // The moon is full or waning
            return -1f
        }
        // The moon is waxing
        return cos(phase)
    }

    // Where on the moon face is the sunset on the moon, the "rightmost" lit part
    fun getWanePoint() : Float {
        if (abs(phase) < EPS || abs(phase) > PI.toFloat() - EPS || phase >= 0f) {
            // The moon is new, full or waxing
            return 1f
        }
        // The moon is waning
        return -cos(phase)
    }
}

fun calculateMoon() : Moon {
    //TODO: Add moon calculation here
    return Moon(0.8f, 0f, 0.0, 0.5)
}