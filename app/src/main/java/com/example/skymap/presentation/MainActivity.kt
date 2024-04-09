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
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.core.content.ContextCompat
import com.example.skymap.presentation.theme.SkyMapTheme
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
import kotlin.math.cos
import kotlin.math.sin
import com.google.gson.JsonParser

private const val WATCHFACE_RADIUS = 192.0f

private const val LOCK_ANGLE = 1f

const val MAX_ZOOM = 5f

private const val STAR_DISPLAY_SIZE = 2f
private const val PLANET_DISPLAY_SIZE = 4f

class MainActivity : ComponentActivity() {
    private var starsArray: com.google.gson.JsonArray? = null
    private var stars : ArrayList<Star> = ArrayList()
    private var planets : ArrayList<Planet> = ArrayList()
    private var settingsOpen : Boolean = false
    private val zoom = PackedFloat(1f)
    private lateinit var sensorManager: SensorManager
    private var azimuth = 0f
    private var vertAngle = 0f
    private var watchUpsideDown = false
    private var displayedAzimuth = 0f
    private val handler = Handler(Looper.getMainLooper())
    private val updateTask = object : Runnable {
        override fun run() {
            orientationUpdate()
            handler.postDelayed(this, 40)
        }
    }
    private val starTask = object : Runnable {
        override fun run() {
            stars = calculateStars(latitude, longitude, starsArray)
            planets = calculatePlanets()
            update()
            handler.postDelayed(this, 5000)
        }
    }

    private val sensorListener : SensorEventListener = object : SensorEventListener {
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
                azimuth = orientationAngles[0]
                vertAngle = acos(transformedNormal[2])
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            return;
        }

    }

    // Lokalizacja
    // Daje lokalizację z różnych źródeł
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (location in result.locations) {
                latitude = location.latitude
                longitude = location.longitude
                stars = calculateStars(latitude, longitude, starsArray)
                update()
            }
        }
    }

    private var latitude = 0.0
    private var longitude = 0.0

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
                requestLocationPermission()
            }
        }
    }

    private fun requestLocationPermission() {
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

    // Funkcja, która wczytuje plik JSON i zwraca JSON jako String
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val builder = com.google.android.gms.location.LocationRequest.Builder(1000)

        builder.setPriority(Priority.PRIORITY_HIGH_ACCURACY)

        builder.setDurationMillis(Long.MAX_VALUE)

        builder.setMinUpdateDistanceMeters(1000f)
        locationRequest = builder.build()

        requestLocationPermission()

        parseJSONS()

        stars = calculateStars(latitude, longitude, starsArray)

        setCanvas()
    }

    private fun setCanvas() {
        setContent {
            WearApp(stars, planets, zoom, displayedAzimuth, watchUpsideDown) {
                settingsOpen = it
            }
        }
    }

    private fun update() {
        if (!settingsOpen) {
            setCanvas()
        }
    }

    private fun orientationUpdate() {
        if (zoom.v == 1f) {
            if (vertAngle < LOCK_ANGLE) {
                watchUpsideDown = false
                displayedAzimuth = blendAngles(displayedAzimuth, azimuth, 0.2f)
                update()
            }
            else if (vertAngle > PI.toFloat() - LOCK_ANGLE) {
                watchUpsideDown = true
                displayedAzimuth = blendAngles(displayedAzimuth, azimuth, 0.2f)
                update()
            }
        }
    }

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
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        handler.post(updateTask)
        handler.post(starTask)
        requestLocationUpdates()
        super.onResume()
    }

    override fun onPause() {
        sensorManager.unregisterListener(sensorListener)
        handler.removeCallbacks(updateTask)
        handler.removeCallbacks(starTask)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onPause()
    }
}

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

val PROJECTION: Projection = EquidistantAzimuthalProjection()

open class SkyPoint(azimuth : Double, altitude : Double) {
    var r: Float = 0.0f
    private var alpha: Float = 0.0f
    init {
        r = (PROJECTION.convert(altitude) * WATCHFACE_RADIUS).toFloat()
        alpha = azimuth.toFloat()
    }

    fun calculatePosition(userCenter : Offset, zoom : Float, phi : Float, flip: Boolean): Offset {
        val y = - zoom * r * cos(alpha + phi)
        val x = zoom * r * sin(alpha + phi) * if (flip) -1f else 1f
        return Offset(x, y) + userCenter
    }
}

class PackedFloat(var v: Float) {
}

fun calculateNorthPosition(phi: Float) : Offset {
    val r = 0.90f * WATCHFACE_RADIUS
    val x = - r * cos(phi) + WATCHFACE_RADIUS
    val y = r * sin(phi) + WATCHFACE_RADIUS
    return Offset(x, y)
}

@Composable
fun WearApp(stars: ArrayList<Star>, planets: ArrayList<Planet>, pZoom : PackedFloat, azimuth: Float, upsideDown: Boolean, toggleMenu: (Boolean) -> Unit) {
    val watchCenter = Offset(WATCHFACE_RADIUS, WATCHFACE_RADIUS)
    var positionOffset by remember {
        mutableStateOf(Offset(0f, 0f))
    }
    var zoom by remember {
        mutableFloatStateOf(pZoom.v)
    }

    var settingsOpen by remember {
        mutableStateOf(false)
    }

    val settingsState = remember {
        mutableStateListOf(0,0,0,0)
    }

    zoom = pZoom.v

    val screenRadius = WATCHFACE_RADIUS / zoom
    if (positionOffset.getDistance() + screenRadius > WATCHFACE_RADIUS) {
        val target = WATCHFACE_RADIUS - screenRadius
        positionOffset *= (target / positionOffset.getDistance())
    }
    val position = watchCenter + positionOffset * zoom

    Log.d("Disp", "${position.x} ${position.y} $zoom")

    val textMeasurer = rememberTextMeasurer()

    val brightnessFactor = 2f - settingsState[INDEX_BRIGHTNESS].toFloat() * 0.5f

    SkyMapTheme {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if(settingsOpen)
            {
                Menu(
                    settingsState.toTypedArray(),
                    {i,v -> settingsState[i] = v},
                    {
                        settingsOpen = false
                        toggleMenu(false)
                    }
                )
            }
            else
            {
                val backgroundColor =
                    when(settingsState[INDEX_COLOR]) {
                        WHITE_MODE -> Color(0f,0f,0.2f,1f)
                        RED_MODE -> Color.Black
                        else -> Color(0f,0f,0.2f,1f)
                    }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput("Drag") {
                            detectDragGestures { _, dragAmount ->
                                positionOffset += dragAmount / zoom
                            }
                        }

                        .pointerInput("Tap") {
                            detectTapGestures(
                                onLongPress = {
                                    settingsOpen = true
                                    toggleMenu(true)
                                },
                                onDoubleTap = { offset ->
                                    positionOffset -= (offset - watchCenter) / zoom
                                    zoom = min(MAX_ZOOM, zoom + 1)
                                    pZoom.v = zoom
                                }
                            )
                        }
                ) {
                    drawCircle(color = backgroundColor, radius = WATCHFACE_RADIUS)

                    // TODO:
                    // pokazywanie północy działa tylko dla zoom = 1
                    // a powinno tylko dla zoom > 1

                    if(zoom >= 1)
                    {
                        val myTextMeasure = textMeasurer.measure("N")
                        val myTextHeight = myTextMeasure.size.height.toFloat()
                        val myTextWidth = myTextMeasure.size.width.toFloat()
                        drawText(
                            myTextMeasure,
                            color = Color.Red,
                            topLeft = calculateNorthPosition(azimuth) - Offset( myTextWidth * 0.5f,myTextHeight * 0.5f)
                        )
                    }

                    for(star in stars)
                    {
                        val color = star.getColor(zoom, settingsState[INDEX_COLOR], brightnessFactor)
                        if(color.alpha > 0)
                        {
                            drawCircle(
                                color = color,
                                radius = STAR_DISPLAY_SIZE,
                                center = star.calculatePosition(position, zoom, -azimuth, upsideDown)
                            )
                        }
                    }
                    if (settingsState[INDEX_PLANET] == SHOW)
                    {
                        for(planet in planets)
                        {
                            val text : String = if (zoom > 3f) planet.name else planet.symbol.toString()
                            val center = planet.calculatePosition(position, zoom, -azimuth, upsideDown)
                            val measured = textMeasurer.measure(text)
                            val color = when(settingsState[INDEX_COLOR]) {
                                WHITE_MODE -> planet.col
                                RED_MODE -> Color.Red
                                else -> planet.col
                            }

                            drawCircle(
                                color = color,
                                radius = PLANET_DISPLAY_SIZE,
                                center = center
                            )
                            drawText(
                                measured,
                                color = color,
                                topLeft = center + Offset(5f - measured.size.width.toFloat() * 0.5f,5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(ArrayList(), ArrayList(), PackedFloat(1f), 0f, false) {

    }
}