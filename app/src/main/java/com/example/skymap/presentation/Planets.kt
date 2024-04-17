package com.example.skymap.presentation

import Converter
import androidx.compose.ui.graphics.Color
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
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

private fun getPlanetObjects(planetsArray: com.google.gson.JsonArray?) : ArrayList<Planet> {

    var planetsObjects: ArrayList<Planet> = ArrayList()

    planetsArray?.forEach { planet ->
        val planetJsonObject = planet.asJsonObject

        val planetName: String = planetJsonObject.getAsJsonPrimitive("name").asString

        val orbitElements = planetJsonObject.getAsJsonObject("orbit_elements")

        val a_au: Double = orbitElements.asJsonObject.getAsJsonPrimitive("a_au").asDouble
        val e: Double = orbitElements.asJsonObject.getAsJsonPrimitive("e").asDouble
        val I_deg: Double = orbitElements.asJsonObject.getAsJsonPrimitive("I_deg").asDouble
        val L_deg: Double = orbitElements.asJsonObject.getAsJsonPrimitive("L_deg").asDouble
        val long_peri_deg: Double = orbitElements.asJsonObject.getAsJsonPrimitive("long_peri_deg").asDouble
        val long_node_deg: Double = orbitElements.asJsonObject.getAsJsonPrimitive("long_node_deg").asDouble

        val rateOfChange = planetJsonObject.getAsJsonObject("rate_of_change")

        val a_au_per_cy: Double = rateOfChange.asJsonObject.getAsJsonPrimitive("a_au_per_cy").asDouble
        val e_per_cy: Double = rateOfChange.asJsonObject.getAsJsonPrimitive("e_per_cy").asDouble
        val I_deg_per_cy: Double = rateOfChange.asJsonObject.getAsJsonPrimitive("I_deg_per_cy").asDouble
        val L_deg_per_cy: Double = rateOfChange.asJsonObject.getAsJsonPrimitive("L_deg_per_cy").asDouble
        val long_peri_deg_per_cy: Double = rateOfChange.asJsonObject.getAsJsonPrimitive("long_peri_deg_per_cy").asDouble
        val long_node_deg_per_cy: Double = rateOfChange.asJsonObject.getAsJsonPrimitive("long_node_deg_per_cy").asDouble

        var planetObject = Planet(
            name = planetName,
            semiMajorAxis0 = a_au,
            semiMajorAxisRateOfChange = a_au_per_cy,
            eccentricity0 = e,
            eccentricityRateOfChange = e_per_cy,
            inclination0 = I_deg,
            inclinationRateOfChange = I_deg_per_cy,
            meanLongitude0 = L_deg,
            meanLongitudeRateOfChange = L_deg_per_cy,
            longitudeOfPerihelion0 = long_peri_deg,
            longitudeOfPerihelionRateOfChange = long_peri_deg_per_cy,
            longitudeOfAscendingNode0 = long_node_deg,
            longitudeOfAscendingNodeRateOfChange = long_node_deg_per_cy
        )

        planetsObjects.add(planetObject)
    }

    return planetsObjects
}

fun calculatePlanets(
    latitude: Double,
    longitude: Double,
    planetArray: com.google.gson.JsonArray?
): ArrayList<Planet> {

    val planetObjects = getPlanetObjects(planetArray)

    val earth = planetObjects[2]
    planetObjects.removeAt(2)

    val localDateTime = LocalDateTime.now(ZoneOffset.UTC)
    val zoneId = ZoneId.of("GMT")
    val zonedDateTime = ZonedDateTime.of(localDateTime, zoneId)

    val converter = Converter(latitude, longitude, zonedDateTime)
    val JED = converter.getJulianDate()

    val earthPositions: HeliocentricEclipticCoordinates = earth.calculateHeliocentricPositions(JED)

    planetObjects.forEach() { planet ->
        val heliocentricPositions: HeliocentricEclipticCoordinates = planet.calculateHeliocentricPositions(JED)
        val equatorialPositions: GeocentricEquatorialCoordinates = planet.calculateGeocentricPositions(heliocentricPositions, earthPositions)
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

    private fun eclipticToEquatorial(ecliptic: GeocentricEclipticCoordinates)
            : GeocentricEquatorialCoordinates {

        val a = sin(ecliptic.latitude) * cos(ECLIPTIC_ANGLE)
        val b = cos(ecliptic.latitude) * sin(ECLIPTIC_ANGLE) * sin(ecliptic.longitude)
        val dec = asin(a + b)

        val cos_ra = cos(ecliptic.longitude) * cos(ecliptic.latitude) / cos(dec)

        val c = -sin(ecliptic.latitude) * sin(ECLIPTIC_ANGLE)
        val d = cos(ecliptic.latitude) * cos(ECLIPTIC_ANGLE) * sin(ecliptic.longitude)
        val sin_ra = (c + d) / cos(dec)

        var ra = atan(sin_ra / cos_ra)
        if (sin_ra < 0 && cos_ra > 0) {
            ra += 2 * PI
        } else if (!(sin_ra > 0 && cos_ra > 0)) {
            ra += PI
        }

        return GeocentricEquatorialCoordinates(ra, toDegrees(dec))
    }

    fun calculateGeocentricPositions(
        planet: HeliocentricEclipticCoordinates,
        earth: HeliocentricEclipticCoordinates
    )
            : GeocentricEquatorialCoordinates {
        val x = planet.x - earth.x
        val y = planet.y - earth.y
        val z = planet.z - earth.z

        val r = sqrt(x * x + y * y + z * z)
        val l = atan2(y, x)
        val b = asin(z / r)

        val ecliptic = GeocentricEclipticCoordinates(l, b)

        return eclipticToEquatorial(ecliptic)
    }

    fun setHorizontal(horizontalPositions: GeocentricHorizontalCoordinates) {
        azimuth = horizontalPositions.azimuth
        altitude = horizontalPositions.altitude
    }
}


