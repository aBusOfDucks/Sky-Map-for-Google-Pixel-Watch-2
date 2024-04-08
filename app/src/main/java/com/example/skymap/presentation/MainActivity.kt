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
import Converter
import EquatorialCoordinates
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.sin
import com.google.gson.JsonParser
import java.time.ZoneOffset

const val WATCHFACE_RADIUS = 192.0

const val LOCK_ANGLE = 1f

const val MAX_ZOOM = 5f

class MainActivity : ComponentActivity() {
    private var starsArray: com.google.gson.JsonArray? = null
    private val stars : ArrayList<Star> = ArrayList()
    private val planets : ArrayList<Planet> = ArrayList()
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
            calculateStars()
            calculatePlanets()
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
            // Dokładność czujnika się zmieniła
            // Interfejs tego wymaga
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
                calculateStars()
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
                // Nie mamy uprawnień
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

    private fun calculateStars() {
        Log.d("Star", "Calculating stars $latitude $longitude")

        val localDateTime = LocalDateTime.now(ZoneOffset.UTC)
        val zoneId = ZoneId.of("GMT")
        val zonedDateTime = ZonedDateTime.of(localDateTime, zoneId)

        val converter = Converter(latitude, longitude, zonedDateTime)


        stars.clear()




        starsArray?.forEach { star ->

            val starJsonObject = star.asJsonObject
            //val name: String = starJsonObject.getAsJsonPrimitive("name").asString
            val coordinates = starJsonObject.getAsJsonObject("coordinates")
            val dec: Double = coordinates.asJsonObject.getAsJsonPrimitive("dec").asDouble
            val ra: Double = coordinates.asJsonObject.getAsJsonPrimitive("ra").asDouble
            val mag: Double = starJsonObject.getAsJsonPrimitive("vmag").asDouble

            val name = starJsonObject.getAsJsonPrimitive("name").asString

            val equatorialCoordinates = EquatorialCoordinates(
                rightAscension = ra,
                declination = dec
            )

            val horizontalCoordinates = converter.equatorialToHorizontal(equatorialCoordinates)

            if (horizontalCoordinates.altitude > 0) {
                stars.add(Star(mag, horizontalCoordinates.azimuth, horizontalCoordinates.altitude))
            }
        }
    }

    private fun calculatePlanets() {
        planets.clear()

        planets.addAll(arrayOf(
            Planet(
                "Mercury",
                Char(0x263F),
                Color(0.5f,0.5f,0.5f,1f),
                0.8,
                0.5
            ),
            Planet(
                "Venus",
                Char(0x2640),
                Color(0.5f,0.48f,0.45f,1f),
                1.6,
                0.5
            ),
            Planet(
                "Mars",
                Char(0x2642),
                Color(0.55f,0.42f,0.4f,1f),
                2.4,
                0.5
            ),
            Planet(
                "Jupiter",
                Char(0x2643),
                Color(0.7f,0.65f,0.6f,1f),
                3.2,
                0.5
            ),
            Planet(
                "Saturn",
                Char(0x2644),
                Color(0.7f,0.7f,0.6f,1f),
                4.0,
                0.5
            ),
            Planet(
                "Uranus",
                Char(0x26E2),
                Color(0.6f,0.65f,0.7f,1f),
                4.8,
                0.5
            ),
            Planet(
                "Neptune",
                Char(0x2646),
                Color(0.55f,0.6f,0.75f,1f),
                5.6,
                0.5
            ),
        ))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val builder = com.google.android.gms.location.LocationRequest.Builder(1000)
        // Na emulatorze działa tylko HIGH_ACCURACY
        builder.setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        // To znaczy że chcemy prosić o lokalizację w nieskończoność
        builder.setDurationMillis(Long.MAX_VALUE)
        // Musimy się przesunąć o 1km
        builder.setMinUpdateDistanceMeters(1000f)
        locationRequest = builder.build()

        requestLocationPermission()

        parseJSONS()

        calculateStars()

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
    var r: Float = 0.0F
    var alpha: Float = 0.0F
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

class Star(mag : Double, azimuth: Double, altitude: Double) : SkyPoint(azimuth, altitude) {
    var position = Offset(0F, 0F)
    var size: Int = 1
    var minimumZoom: Int = 0

    init {
        size = BRIGHTNESS_MAX - mag.toInt()
        minimumZoom = mag.toInt() - 1
    }

    fun isVisible(brightness: Int, zoom: Float): Boolean {
        //return brightness < size && minimumZoom <= zoom
        return true
    }

    fun getColor(zoom: Float, colorSetting: Int, brightnessFactor : Float): Color {

        if(!isVisible(0, zoom))
            return Color.Black

        val starColor =
            if (colorSetting == 0)
                Color.White
            else
                Color.Red

        val alpha = saturate((zoom + brightnessFactor - minimumZoom) / (MAX_ZOOM - minimumZoom))

     // Uncomment to disable star changing color from background to red / white
     //   alpha = size.toFloat() / (BRIGHTNESS_MAX + 1).toFloat()
        return Color(starColor.red, starColor.green, starColor.blue, alpha)
    }

    private fun saturate(v : Float) : Float {
        if (v < 0f)
            return 0f
        if (v > 1f)
            return 1f
        return v
    }
}

class Planet(
    val name : String,
    val symbol : Char,
    val col : Color,
    azimuth: Double,
    altitude: Double)
    : SkyPoint(azimuth, altitude) {
}

class PackedFloat(var v: Float) {
}


@Composable
fun WearApp(stars: ArrayList<Star>, planets: ArrayList<Planet>, pZoom : PackedFloat, azimuth: Float, upsideDown: Boolean, toggleMenu: (Boolean) -> Unit) {
    var brightness: Int = 0
    val watchCenter = Offset(WATCHFACE_RADIUS.toFloat(), WATCHFACE_RADIUS.toFloat())
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

    if (zoom != pZoom.v) {
        zoom = pZoom.v
    }

    val screenRadius = WATCHFACE_RADIUS / zoom
    if (positionOffset.getDistance() + screenRadius > WATCHFACE_RADIUS) {
        val target = WATCHFACE_RADIUS - screenRadius
        positionOffset *= (target / positionOffset.getDistance()).toFloat()
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
                brightness = settingsState[INDEX_BRIGHTNESS]
                val backgroundColor =
                    if (settingsState[INDEX_COLOR] == 0)
                        Color(0f,0f,0.2f,1f)
                    else
                        Color.Black

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
                    drawCircle(color = backgroundColor, radius = WATCHFACE_RADIUS.toFloat())
                    for(s in stars)
                    {
                        if(s.isVisible(brightness, zoom))
                        {
                            val size = 3F
                            val x = s.size.toFloat() / (BRIGHTNESS_MAX + 1).toFloat()
                            val col = s.getColor(zoom, settingsState[INDEX_COLOR], brightnessFactor)
                            drawCircle(
                                color = col,
                                radius = size,
                                center = s.calculatePosition(position, zoom, -azimuth, upsideDown)
                            )
                        }
                    }
                    if (settingsState[INDEX_PLANET] == 0)
                    {
                        for(p in planets)
                        {
                            val text : String = if (zoom > 3f) p.name else p.symbol.toString()
                            val pos = p.calculatePosition(position, zoom, -azimuth, upsideDown)
                            val measured = textMeasurer.measure(text)
                            val col = if (settingsState[INDEX_COLOR] == 0) p.col else Color.Red

                            drawCircle(
                                color = col,
                                radius = 4f,
                                center = pos
                            )
                            drawText(
                                measured,
                                color = col,
                                topLeft = pos + Offset(5f - measured.size.width.toFloat() * 0.5f,5f)
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