
import com.example.skymap.presentation.GeocentricEquatorialCoordinates
import com.example.skymap.presentation.GeocentricHorizontalCoordinates
import com.example.skymap.presentation.toRadians
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

class Converter(
    latitudeDegree: Double, longitudeDegree: Double, zonedTime: ZonedDateTime
) {
    private val latitude: Double
    private val longitude: Double
    private val time: ZonedDateTime

    /**
     * Constructor
     * @param latitude latitude of an observer in degrees [Double]
     * @param longitude longitude of on observer in degrees [Double]
     * @param time Greenwich sidereal time of observation [ZonedDateTime]
     * @return converter
     */
    init {
        latitude = toRadians(latitudeDegree)
        longitude = toRadians(longitudeDegree)
        time = zonedTime
    }

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
        equatorial: GeocentricEquatorialCoordinates
    ): GeocentricHorizontalCoordinates {
        val declination = toRadians(equatorial.declination)

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

}
