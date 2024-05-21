import com.example.skymap.presentation.ECLIPTIC_ANGLE
import com.example.skymap.presentation.GeocentricEclipticCoordinates
import com.example.skymap.presentation.GeocentricEquatorialCoordinates
import com.example.skymap.presentation.GeocentricHorizontalCoordinates
import com.example.skymap.presentation.HeliocentricEclipticCoordinates
import com.example.skymap.presentation.Planet
import com.example.skymap.presentation.toDegrees
import com.example.skymap.presentation.toRadians
import com.example.skymap.presentation.zonedDateTimeNow
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Function calculates Julian Date at a given Gregorian Day
 * @param year - year in a Gregorian calendar [Int]
 * @param month - month in a Gregorian calendar [Int]
 * @param day - day in a Gregorian calendar [Int]
 * @return Julian Date Number at a given day [Int]
 */
private fun calculateJDN(year: Int, month: Int, day: Int): Int {
    val a = (month - 14) / 12
    val y = year + 4800 + a
    val m = month - 12 * a - 2
    return (1461 * y) / 4 + (367 * m) / 12 - (3 * (y + 100) / 100) / 4 + day - 32075
}

/**
 * Function calculates Julian Date at a current time.
 * @return Julian Date at a given time [Double]
 */
fun getJulianDate(): Double {
    val time = zonedDateTimeNow()

    var year = time.year
    var month = time.month.value
    val day = time.dayOfMonth

    // Julian Date Number
    val JDN = calculateJDN(year, month, day)

    val hour = time.hour.toDouble()
    val minute = time.minute.toDouble()
    val second = time.second.toDouble()

    // Julian Date
    var JD = JDN.toDouble() + (hour - 12.0) / 24
    JD += minute / 1440
    JD += second / 86400
    return JD
}

/**
 * Function calculates Greenwich Mean Sidereal Time of observation
 * and then changes unit to radian.
 * @param time Greenwich Mean Time of observation [ZonedDateTime]
 * @return hour angle of Greenwich Mean Sidereal Time at time
 * of an observation in radians [Double]
 */
private fun getGMST(): Double {
    val JD = getJulianDate()
    val time = zonedDateTimeNow()

    val d = JD - 2451545.0
    val T = d / 36525

    var GMST = 24110.54841 + 8640184.812866 * T + 0.093104 * T * T - 0.0000062 * T * T * T

    val midnight = LocalTime.of(0, 0)
    val currentTime = time.toLocalTime()
    val duration = Duration.between(midnight, currentTime)

    GMST += duration.seconds
    GMST %= 86400

    return GMST / 86400 * 2 * PI
}

/**
 * Function that changes equatorial coordinates of an object to horizontal coordinates,
 * dependent on an observer.
 * @param equatorial equatorial coordinates of an object [GeocentricEquatorialCoordinates]
 * @return horizontal coordinates in radians [GeocentricHorizontalCoordinates]
 */
fun equatorialToHorizontal(
    latitudeDegree: Double,
    longitudeDegree: Double,
    equatorial: GeocentricEquatorialCoordinates
): GeocentricHorizontalCoordinates {
    val declination = toRadians(equatorial.declination)
    val latitude = toRadians(latitudeDegree)
    val longitude = toRadians(longitudeDegree)

    val hourAngle = (getGMST() + longitude - equatorial.rightAscension)

    val sinAltitude = sin(declination) * sin(latitude) + cos(declination) * cos(latitude) * cos(hourAngle)

    val altitude = asin(sinAltitude)

    val cosAltitude = cos(altitude)

    val sinAzimuth = - (cos(declination) * sin(hourAngle)) / cosAltitude

    val cosAzimuth = - ((cos(declination) * sin(latitude) * cos(hourAngle)) - sin(declination) * cos(latitude)) / cosAltitude

    val tanAzimuth = sinAzimuth / cosAzimuth

    var azimuth = atan(tanAzimuth)

    if (sinAzimuth < 0 && cosAzimuth > 0) {
        azimuth += 2 * PI
    } else if (!(sinAzimuth > 0 && cosAzimuth > 0)) {
        azimuth += PI
    }

    return GeocentricHorizontalCoordinates(altitude, azimuth)
}

fun getPlanetObjects(planetsArray: com.google.gson.JsonArray?) : ArrayList<Planet> {

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

fun eclipticToEquatorial(ecliptic: GeocentricEclipticCoordinates)
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

    return GeocentricEquatorialCoordinates(ra, toDegrees(dec), ecliptic.r)
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

    val ecliptic = GeocentricEclipticCoordinates(l, b, r)

    return eclipticToEquatorial(ecliptic)
}

