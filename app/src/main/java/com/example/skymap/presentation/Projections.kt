package com.example.skymap.presentation

import kotlin.math.PI
import kotlin.math.cos

interface Projection {
    fun convert(altitude : Double) : Double
}

class OrthographicProjection : Projection {
    override fun convert(altitude: Double): Double {
        return cos(altitude)
    }
}

class EquidistantAzimuthalProjection : Projection {
    override fun convert(altitude: Double): Double {
        return 1.0 - 2.0 * altitude / PI
    }
}
