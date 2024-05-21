package com.example.skymap.presentation

import kotlin.math.PI
import kotlin.math.cos

interface Projection {
    // Projections should preserve the azimuth, so only the altitude is given
    fun convert(altitude : Double) : Double
}

// The view of the hemisphere from infinitely far away
class OrthographicProjection : Projection {
    override fun convert(altitude: Double): Double {
        return cos(altitude)
    }
}

// Distance from the zenith to the point (along the sphere) is preserved.
class EquidistantAzimuthalProjection : Projection {
    override fun convert(altitude: Double): Double {
        return 1.0 - 2.0 * altitude / PI
    }
}
