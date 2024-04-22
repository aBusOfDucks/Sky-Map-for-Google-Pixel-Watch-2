/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.example.skymap.presentation

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Matrix
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.wearable.input.RotaryEncoderHelper
import kotlin.math.PI
import kotlin.math.acos
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import com.google.gson.JsonParser

private const val LOCK_ANGLE = 1f

const val MAX_ZOOM = 5f

const val AZIMUTH_INERTIA = 0.5f

class MainActivity : ComponentActivity() {
    // Astronomical objects:

    /** Array with stars as parsed JSON objects */
    private var starsArray: com.google.gson.JsonArray? = null
    /** Array with stars with converted coordinates */
    private var stars : ArrayList<Star> = ArrayList()
    /** Array with sky structures with converted coordinates */
    private var skyStructures : ArrayList<SkyStructures> = ArrayList()
    /** Array with planets as parsed JSON objects */
    private var planetsArray: com.google.gson.JsonArray? = null
    /** Array with planets with converted coordinates */
    private var planets : ArrayList<Planet> = ArrayList()
    /** The Moon with converted coordinates */
    private var moon : Moon = Moon(0f, 0f, 0.0, 0.0)
    /** The Sun with converted coordinates */
    private var sun: Sun = Sun(0.0, 0.0)
    private var constellationsArray: com.google.gson.JsonArray? = null
    private var constellations: ArrayList<Constellation> = ArrayList()

    // Parameters for display:

    private var settingsOpen : Boolean = false
    private val zoom = PackedFloat(1f)
    /** The true azimuth of the watch, from the latest orientation update */
    private var trueAzimuth = 0f
    /** The azimuth of the watch, animated with some inertia so that
     *  it changes in a smooth fation */
    private var smoothAzimuth = 0f
    /** This is the angle that the sky map is rotated by.
     *  Changes smoothly, but also freezes if the watch is too vertical,
     *  or the map is zoomed in */
    private var skyAzimuth = 0f
    /** Angle between the normal vector of the screen and the vertical axis */
    private var vertAngle = 0f
    private var watchUpsideDown = false

    // Tasks:

    /** Handles executing tasks periodically */
    private val handler = Handler(Looper.getMainLooper())
    /** A quick task that runs 25 times a second */
    private val quickTask = object : Runnable {
        override fun run() {
            orientationUpdate()
            handler.postDelayed(this, 40)
        }
    }
    /** A more expensive task that needs to run every few seconds */
    private val longTask = object : Runnable {
        override fun run() {
            calculateObjects()
            update()
            handler.postDelayed(this, 5000)
        }
    }

    // Orientation:

    /** Needed for getting orientation */
    private lateinit var sensorManager: SensorManager

    /** Listens for orientation updates */
    private val sensorListener : SensorEventListener = object : SensorEventListener {
        // Better to create all needed arrays once

        private val rotationVector = FloatArray(4)
        private val rotationMatrix = FloatArray(16)
        private val normalVector = FloatArray(4)
        private val transformedNormal = FloatArray(4)
        private val orientationAngles = FloatArray(3)

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                System.arraycopy(event.values, 0, rotationVector, 0, rotationVector.size)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)
                normalVector[0] = 0f
                normalVector[1] = 0f
                normalVector[2] = 1f
                normalVector[3] = 0f
                Matrix.multiplyMV(
                    transformedNormal, 0,
                    rotationMatrix, 0,
                    normalVector, 0
                )
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                trueAzimuth = orientationAngles[0]
                vertAngle = acos(transformedNormal[2])
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // We don't have any functionality when accuracy of the sensor changes
            // The interface requires this function to be implemented
            return
        }

    }

    // Location:

    /** Can get location from multiple sources */
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    /** Handles location updates */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            // We may receive location updates in batches
            // we only care for the newest one,
            // the locations are ordered oldest to newest
            if (result.locations.isNotEmpty()) {
                locationReceived = true
                val location = result.locations.last()
                latitude = location.latitude
                longitude = location.longitude
                // We want to update immediately, because
                // the next long task might be in 5 seconds
                calculateObjects()
                update()
            }
        }
    }

    private var latitude = 0.0
    private var longitude = 0.0
    private var locationReceived = false

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                requestLocationUpdates()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                requestLocationUpdates()
            }
            else -> {
                // We do not have permissions
                // Let's try to ask for them again
                // This is not a very clean solution,
                // but the application will not work without
                // location updates
                requestLocationPermission()
            }
        }
    }

    // Methods:

    private fun requestLocationPermission() {
        // If we already have some permissions, we do not need to ask for them
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    private fun requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
        else {
            requestLocationPermission()
        }
    }

    /** Reads a JSON file's contents and returns them as a String */
    private fun loadJSONFromAnotherFile(fileName: String): String? {
        var json: String? = null
        try {
            val inputStream = assets.open(fileName)
            json = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return json
    }

    private fun parseJSONS() {
        val starsFile = loadJSONFromAnotherFile("stars.json")
        val jsonStars = JsonParser.parseString(starsFile).asJsonObject
        starsArray = jsonStars.getAsJsonArray("stars")

        val planetsFile = loadJSONFromAnotherFile("planets.json")
        val jsonPlanets = JsonParser.parseString(planetsFile).asJsonObject
        planetsArray = jsonPlanets.getAsJsonArray("planets")

        val constellationsFile = loadJSONFromAnotherFile("constellations.json")
        val jsonConstellations = JsonParser.parseString(constellationsFile).asJsonObject
        constellationsArray = jsonConstellations.getAsJsonArray("constellations")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val builder = LocationRequest.Builder(1000)
        builder.setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        builder.setDurationMillis(Long.MAX_VALUE)
        // We only want to receive an update if we have moved over 1km away from our previous location
        builder.setMinUpdateDistanceMeters(1000f)
        locationRequest = builder.build()

        requestLocationPermission()
        parseJSONS()

        setCanvas()
    }

    private fun calculateObjects() {
        stars = calculateStars(latitude, longitude, starsArray)
        // TODO: add real data to skyStructures
        skyStructures = getSkyStructures()
        planets = calculatePlanets(latitude, longitude, planetsArray)
        moon = calculateMoon()
        sun = calculateSun(latitude, longitude, planetsArray)
        constellations = calculateConstellations(constellationsArray)
    }

    private fun setCanvas() {
        setContent {
            if (locationReceived) {
                WearApp(stars, skyStructures, planets, moon, sun, constellations, zoom, skyAzimuth, smoothAzimuth, watchUpsideDown) {
                    settingsOpen = it
                }
            }
            else {
                WaitingScreen()
            }
        }
    }

    private fun update() {
        // No need in updating the screen if the menu is open
        if (!settingsOpen) {
            setCanvas()
        }
    }

    private fun orientationUpdate() {
        smoothAzimuth = blendAngles(smoothAzimuth, trueAzimuth, AZIMUTH_INERTIA)
        if (zoom.v == 1f) {
            if (vertAngle < LOCK_ANGLE) {
                watchUpsideDown = false
                skyAzimuth = blendAngles(skyAzimuth, trueAzimuth, AZIMUTH_INERTIA)
            }
            else if (vertAngle > PI.toFloat() - LOCK_ANGLE) {
                watchUpsideDown = true
                skyAzimuth = blendAngles(skyAzimuth, trueAzimuth, AZIMUTH_INERTIA)
            }
        }
        update()
    }

    /** Handles rotary input */
    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_SCROLL && RotaryEncoderHelper.isFromRotaryEncoder(event)) {
            val delta = RotaryEncoderHelper.getRotaryAxisValue(event)
            zoom.v -= delta * 0.5f
            zoom.v = max(1f, min(zoom.v, MAX_ZOOM))
            update()
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onResume() {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor != null) {
            sensorManager.registerListener(
                sensorListener,
                sensor,
                SensorManager.SENSOR_DELAY_UI,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        // We want to have an update immediately after resuming,
        // that is why post and not postDelayed is used here
        handler.post(quickTask)
        handler.post(longTask)
        requestLocationUpdates()
        super.onResume()
    }

    override fun onPause() {
        // Cancel updating anything
        sensorManager.unregisterListener(sensorListener)
        handler.removeCallbacks(quickTask)
        handler.removeCallbacks(longTask)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onPause()
    }
}

/** Shifts angle by multiple of 2PI so that it is within (-PI, PI] */
fun shiftAngle(a: Float) : Float {
    var res = a
    while (res > PI.toFloat()) {
        res -= 2 * PI.toFloat()
    }
    while (res <= -PI.toFloat()) {
        res += 2 * PI.toFloat()
    }
    return res
}

fun blendAngles(a1: Float, a2: Float, weight: Float) : Float{
    val diff = shiftAngle(a2 - a1)
    return shiftAngle(a1 + weight * diff)
}

