package com.example.skymap.presentation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.getValue
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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.example.skymap.R

const val INDEX_CONSTELLATION = 0
const val INDEX_PLANET = 1
const val INDEX_BRIGHTNESS = 2
const val INDEX_COLOR = 3
const val INDEX_SUN_MOON = 4
const val INDEX_DEEP_SKY = 5

const val PLANET_SHOW = 0
const val PLANET_SHOW_NO_TEXT = 1
const val PLANET_HIDE = 2

const val FLAG_SUN = 1
const val FLAG_MOON = 2

const val CONSTELLATIONS_HIDE = 0
const val CONSTELLATIONS_SHOW_NO_TEXT = 1
const val CONSTELLATIONS_SHOW = 2

const val DEEP_SKY_HIDE = 0
const val DEEP_SKY_SHOW = 1
const val DEEP_SKY_SHOW_TEXT = 2

const val WHITE_MODE = 0
const val RED_MODE = 1

fun showPlanets(state: Int): Boolean{
    return when(state) {
        PLANET_SHOW -> true
        PLANET_SHOW_NO_TEXT -> true
        PLANET_HIDE -> false
        else -> false
    }
}

fun showPlanetsText(state: Int): Boolean{
    return when(state) {
        PLANET_SHOW -> true
        PLANET_SHOW_NO_TEXT -> false
        PLANET_HIDE -> false
        else -> false
    }
}


fun showConstellations(state: Int) : Boolean {
    return when(state) {
        CONSTELLATIONS_SHOW -> true
        CONSTELLATIONS_SHOW_NO_TEXT -> true
        CONSTELLATIONS_HIDE-> false
        else -> false
    }
}

fun showConstellationsText(state: Int) : Boolean {
    return when(state) {
        CONSTELLATIONS_SHOW -> true
        CONSTELLATIONS_SHOW_NO_TEXT -> false
        CONSTELLATIONS_HIDE-> false
        else -> false
    }
}


fun showStructures(state: Int) : Boolean {
    return when(state) {
        DEEP_SKY_HIDE -> false
        DEEP_SKY_SHOW -> true
        DEEP_SKY_SHOW_TEXT -> true
        else -> false
    }
}

fun showStructuresText(state: Int) : Boolean {
    return when(state) {
        DEEP_SKY_HIDE -> false
        DEEP_SKY_SHOW -> false
        DEEP_SKY_SHOW_TEXT -> true
        else -> false
    }
}



const val BRIGHTNESS_MAX = 6

/**
 * The menu of the application.
 * @param menuState a list containing the current selected settings
 * @param menuExit will be called when the user exits from the menu
 */
@Composable
fun Menu(menuState: SnapshotStateList<Int>, menuExit: () -> Unit) {

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
    Column(modifier = Modifier
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
                // If a user stops dragging, we animate the box,
                // either to the starting position, or outside the screen,
                // depending on how far the screen was dragged
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
        .offset(Dp(0f), Dp(offsetY.value)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .padding(all = Dp(5.0f)),
            horizontalArrangement = Arrangement.Center
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
                    onClick = { i -> menuState[INDEX_CONSTELLATION] = i },
                    padding = 2.5f
                )
            }
            item {
                IconChangingButton(
                    icons = arrayOf(
                        R.drawable.planet_text,
                        R.drawable.planet,
                        R.drawable.planet_no
                    ),
                    startState = menuState[INDEX_PLANET],
                    onClick = { i -> menuState[INDEX_PLANET] = i },
                    padding = 5.0f
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
                    onClick = { i -> menuState[INDEX_BRIGHTNESS] = i },
                    padding = 2.5f
                )
            }
            // Special case: this button changes the color of the icon, not the icon itself
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
                            menuState[INDEX_COLOR] = index
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
            item {
                IconChangingButton(
                    icons = arrayOf(
                        R.drawable.no_sun_moon,
                        R.drawable.sun,
                        R.drawable.moon,
                        R.drawable.sun_moon,
                    ),
                    startState = menuState[INDEX_SUN_MOON],
                    onClick = { i -> menuState[INDEX_SUN_MOON] = i },
                    padding = 5.0f
                )
            }
            item {
                IconChangingButton(
                    icons = arrayOf(
                        R.drawable.galaxy_no,
                        R.drawable.galaxy,
                        R.drawable.galaxy_text
                    ),
                    startState = menuState[INDEX_DEEP_SKY],
                    onClick = { i -> menuState[INDEX_DEEP_SKY] = i },
                    padding = 5.0f
                )
            }
        }
    }
}

/**
 * A simple composable for a button that cycles through different icons on each click
 * @param icons an array of IDs of icons to cycle through
 * @param startState which icon to start on
 * @param onClick will be called with each click of the button
 * @param padding padding of the icon
 */
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