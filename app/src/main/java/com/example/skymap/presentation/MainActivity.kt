/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.example.skymap.presentation

import android.os.Bundle
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
import com.example.skymap.presentation.theme.SkyMapTheme
import com.google.android.wearable.input.RotaryEncoderHelper
import kotlin.math.max
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

const val WATCHFACE_RADIUS = 225.0


class MainActivity : ComponentActivity() {
    private val stars : ArrayList<Star> = ArrayList()
    private var settingsOpen : Boolean = false
    private val zoom = PackedFloat(1f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Random data for prototype
        for( i in (0..500))
        {
            val temp: Star = Star()
            temp.generate()
            stars.add(temp)
        }
        setCanvas()
    }

    private fun setCanvas() {
        setContent {
            WearApp(stars, zoom) {
                settingsOpen = !settingsOpen
            }
        }
    }

    private fun update() {
        if (!settingsOpen) {
            setCanvas()
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_SCROLL && RotaryEncoderHelper.isFromRotaryEncoder(event)) {
            val delta = RotaryEncoderHelper.getRotaryAxisValue(event)
            zoom.v -= delta * 0.5f
            zoom.v = max(1f, min(zoom.v, 5f))
            update()

            return true
        }
        return super.onGenericMotionEvent(event)
    }
}

// Placeholder for prototype
class Star{
    var position = Offset(0F, 0F)
    var size: Int = 1
    var r: Float = 0.0F
    var alpha: Float = 0.0F

    fun generate() {
        size = (1..BRIGHTNESS_MAX+1).random()
        r = ((WATCHFACE_RADIUS - Math.pow(WATCHFACE_RADIUS, Random.nextDouble())) * (Random.nextDouble() / 2.0 + 0.5)).toFloat()
        alpha = (Random.nextFloat() * 2.0 * Math.PI).toFloat()
    }

    fun calculat_position(user_center : Offset, zoom : Float): Offset {
        var x = zoom * r * cos(alpha)
        var y = zoom * r * sin(alpha)
        return Offset(x, y) + user_center
    }
}

class PackedFloat(var v: Float) {
}


@Composable
fun WearApp(stars: ArrayList<Star>, pZoom : PackedFloat, toggleMenu: () -> Unit) {
    var brightness: Int = 0
    val watchCenter = Offset(WATCHFACE_RADIUS.toFloat(), WATCHFACE_RADIUS.toFloat())
    var positionOffset by remember {
        mutableStateOf(Offset(WATCHFACE_RADIUS.toFloat(), WATCHFACE_RADIUS.toFloat()))
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
                        toggleMenu()
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
                val starColor =
                    if (settingsState[INDEX_COLOR] == 0)
                        Color.White
                    else
                        Color.Red

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput("Drag") {
                            detectDragGestures { _, dragAmount ->
                                var newPosition = positionOffset + dragAmount
                                newPosition -= watchCenter
                                if (newPosition.getDistanceSquared() <= Math.pow((zoom - 1) * WATCHFACE_RADIUS, 2.0))
                                {
                                    positionOffset += dragAmount
                                }
                            }
                        }

                        .pointerInput("Tap") {
                            detectTapGestures(
                                onLongPress = {
                                    settingsOpen = true
                                    toggleMenu()
                                },
                                onDoubleTap = { offset ->
                                    positionOffset += offset - watchCenter
                                    zoom++
                                    if (zoom > 5f) {
                                        zoom = 1f
                                    }
                                    pZoom.v = zoom
                                }
                            )
                        }
                ) {
                    drawCircle(color = backgroundColor, radius = WATCHFACE_RADIUS.toFloat())
                    for(s in stars)
                    {
                        if(s.size > brightness)
                        {
                            var size = 1F
                            var col = starColor
                            val DISPLAY_MODE_TEST = 2

                            when(DISPLAY_MODE_TEST) {
                                0 -> {
                                    size = s.size.toFloat()
                                    col = starColor
                                }
                                1 -> {
                                    size = s.size.toFloat()
                                    val x = s.size.toFloat() / (BRIGHTNESS_MAX+1).toFloat()
                                    col = Color(x * starColor.red, x * starColor.green, x * starColor.blue)
                                }
                                2 -> {
                                    size = 3F
                                    val x = s.size.toFloat() / (BRIGHTNESS_MAX+1).toFloat()
                                    col = Color(x * starColor.red, x * starColor.green, x * starColor.blue)
                                }
                            }
                            drawCircle(
                                color = col,
                                radius = size,
                                center = s.calculat_position(positionOffset, zoom)
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
    WearApp(ArrayList(), PackedFloat(1f)) {

    }
}