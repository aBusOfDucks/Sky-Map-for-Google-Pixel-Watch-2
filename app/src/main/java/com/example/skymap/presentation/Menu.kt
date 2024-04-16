package com.example.skymap.presentation


import android.util.Log
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.example.skymap.R
import com.example.skymap.presentation.theme.SkyMapTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


const val INDEX_CONSTELLATION = 0
const val INDEX_PLANET = 1
const val INDEX_BRIGHTNESS = 2
const val INDEX_COLOR = 3

const val SHOW = 0
const val DO_NOT_SHOW = 1

const val WHITE_MODE = 0
const val RED_MODE = 1

const val BRIGHTNESS_MAX = 6

@Composable
fun Menu(menuState: Array<Int>, changeState: (Int, Int) -> Unit, menuExit: () -> Unit) {

    // https://developer.android.com/reference/android/util/DisplayMetrics
    // A way to get the display height
    val context = LocalContext.current
    val displayMetrics = context.resources.displayMetrics
    val height = with(LocalDensity.current) {displayMetrics.heightPixels.toDp()}

    val density = LocalDensity.current

    val offsetY = remember {
        Animatable(0f)
    }
    if (abs(offsetY.value) > height.value) {
        menuExit()
    }
    val coroutineScope = rememberCoroutineScope()
    Box(modifier = Modifier
        .fillMaxSize()
        .draggable(
            state = rememberDraggableState(onDelta = { d ->
                // Annoyingly, the drag delta is given in pixels,
                // but offset uses Device Pixels
                val dpDelta = with(density) {d.toDp()}
                // Unfortunately, there is no set value for animatable values
                // We need to snap to them
                coroutineScope.launch {
                    offsetY.snapTo(offsetY.value + dpDelta.value)
                }
            }),
            orientation = Orientation.Vertical,
            onDragStopped = { _ ->
                // If a user stops dragging, we animate the
                // box back to the starting position
                this.launch {
                    offsetY.animateTo(
                        targetValue = if (abs(offsetY.value) > height.value * 0.55) offsetY.value * 2 else 0f,
                        animationSpec = tween(
                            durationMillis = 500,
                            delayMillis = 0,
                            easing = EaseOutBounce
                        )
                    )
                }
            }
        )
        .offset(Dp(0f), Dp(offsetY.value))
    )
    {
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(all = Dp(19.0f))
        )
        {
            item {
                IconChangingButton(
                    icons = arrayOf(
                        R.drawable.constellation_no_lines,
                        R.drawable.constellation_lines,
                        R.drawable.constellation_text
                    ),
                    startState = menuState[INDEX_CONSTELLATION],
                    onClick = { i -> changeState(INDEX_CONSTELLATION, i) },
                    padding = 2.5f
                )
            }
            item {
                IconChangingButton(
                    icons = arrayOf(
                        R.drawable.planet,
                        R.drawable.planet_no
                    ),
                    startState = menuState[INDEX_PLANET],
                    onClick = { i -> changeState(INDEX_PLANET, i) },
                    padding = 7.5f
                )
            }
            item {
                IconChangingButton(
                    icons = arrayOf(
                        R.drawable.brightness_0,
                        R.drawable.brightness_1,
                        R.drawable.brightness_2,
                        R.drawable.brightness_3,
                        R.drawable.brightness_4,
                        R.drawable.brightness_5,
                        R.drawable.brightness_6,
                    ),
                    startState = menuState[INDEX_BRIGHTNESS],
                    onClick = { i -> changeState(INDEX_BRIGHTNESS, i) },
                    padding = 2.5f
                )
            }
            item {
                val cols = arrayOf(Color.White, Color.Red)
                Box(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .aspectRatio(1.0f)
                        .padding(all = Dp(2.5f))
                ) {
                    var index by remember { mutableIntStateOf(menuState[INDEX_COLOR]) }
                    Button(
                        modifier = Modifier
                            .fillMaxSize(),
                        onClick = {
                            index++
                            index %= 2
                            changeState(INDEX_COLOR, index)
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.DarkGray,
                            contentColor = cols[index],
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.eye),
                            "",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(all = Dp(5f))
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun IconChangingButton(icons: Array<Int>, startState: Int, onClick : (Int) -> Unit, padding : Float) {
    Box(modifier =
    Modifier
        .fillMaxSize()
        .aspectRatio(1.0f)
        .padding(all = Dp(2.5f))
    ) {
        var index by remember { mutableIntStateOf(startState)}
        Button(
            modifier = Modifier
                .fillMaxSize(),
            onClick = {
                index++
                index %= icons.size
                onClick(index)
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.DarkGray,
                contentColor = Color.White,
            )) {
            Icon(
                painter = painterResource(id = icons[index]),
                "",
                modifier = Modifier
                    .padding(all = Dp(padding))
                    .fillMaxSize())
        }
    }
}