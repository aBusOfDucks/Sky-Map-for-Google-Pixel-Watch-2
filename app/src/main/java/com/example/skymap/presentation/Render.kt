package com.example.skymap.presentation

import android.graphics.RectF
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.skymap.presentation.theme.SkyMapTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val PROJECTION: Projection = EquidistantAzimuthalProjection()

private const val WATCHFACE_RADIUS = 192.0f

private const val STAR_RADIUS = 2f
private const val PLANET_RADIUS = 4f

// The Sun and the Moon are about the same size in the sky
private const val MOON_RADIUS = 12f
private const val SUN_RADIUS = 12f

// The minimal zoom at which full names of objects are displayed
private const val NAME_CUTOFF_ZOOM = 3.0f

private const val STRUCTURES_FONT_SIZE = 7
private const val STRUCTURES_SHOW_ZOOM = 3
private const val STRUCTURES_SHOW_TEXT_ZOOM = 4.8

private const val CONSTELLATION_FONT_SIZE = 6
private const val CONSTELLATIONS_SHOW_ZOOM = 3

open class SkyPoint(open var azimuth : Double, open var altitude : Double) {
    var r: Float = 0.0f
    private var alpha: Float = 0.0f
    open val color : Color = Color.White
    open val colorRedMode: Color = Color.Red

    init {
        reproject()
    }

    fun reproject() {
        r = (PROJECTION.convert(altitude) * WATCHFACE_RADIUS).toFloat()
        alpha = azimuth.toFloat()
    }

    fun calculatePosition(userCenter : Offset, zoom : Float, phi : Float, flip: Boolean): Offset {
        // Normally, the equation of converting angle and radius to x and y is
        // given by x = r * cos(alpha), y = r * sin(alpha)
        // However:
        // 1. When azimuth == 0, the top of the screen points north
        //    thus, angle equal to 0 should be the top of the screen
        // 2. Azimuth increases clockwise when the device is right side up and anticlockwise when
        //    it is upside down
        // 3. The y axis goes down on the screen, higher y value mean lower on the screen

        val y = - zoom * r * cos(alpha + phi)
        val x = zoom * r * sin(alpha + phi) * if (flip) -1f else 1f
        return Offset(x, y) + userCenter
    }
}

class PackedFloat(var v: Float) {
}

fun calculateNorthPosition(phi: Float, upsideDown: Boolean) : Offset {
    // Normally, the equation of converting angle and radius to x and y is
    // given by x = r * cos(alpha), y = r * sin(alpha)
    // However:
    // 1. When azimuth == 0, the top of the screen points north
    //    thus, angle equal to 0 must mean the top of the screen (hence cos when calculating y)
    // 2. Azimuth increases clockwise when the device is right side up and anticlockwise when
    //    it is upside down
    // 3. The y axis goes down on the screen, higher y value mean lower on the screen
    val flip = if(upsideDown) 1f else -1f
    val r = 0.90f * WATCHFACE_RADIUS
    val x = flip * r * sin(phi) + WATCHFACE_RADIUS
    val y = - r * cos(phi) + WATCHFACE_RADIUS
    return Offset(x, y)
}

@Composable
fun WearApp(
    stars: HashMap<Int,Star>,
    skyStructures: ArrayList<SkyStructures>,
    planets: ArrayList<Planet>,
    moon : Moon,
    sun: Sun,
    constellations: ArrayList<Constellation>,
    pZoom : PackedFloat,
    mapAzimuth: Float,
    realAzimuth: Float,
    upsideDown: Boolean,
    toggleMenu: (Boolean) -> Unit) {
    val watchCenter = Offset(WATCHFACE_RADIUS, WATCHFACE_RADIUS)
    var positionOffset by remember {
        mutableStateOf(Offset(0f, 0f))
    }
    var zoom by remember {
        mutableFloatStateOf(pZoom.v)
    }
    zoom = pZoom.v

    var settingsOpen by remember {
        mutableStateOf(false)
    }

    val settingsState = remember {
        mutableStateListOf(
            CONSTELLATIONS_HIDE,
            PLANET_SHOW,
            0,
            WHITE_MODE,
            FLAG_MOON or FLAG_SUN,
            DEEP_SKY_HIDE
        )
    }


    val screenRadius = WATCHFACE_RADIUS / zoom
    // Detects whether the user tried to drag past the map
    if (positionOffset.getDistance() + screenRadius > WATCHFACE_RADIUS) {
        val target = WATCHFACE_RADIUS - screenRadius
        positionOffset *= (target / positionOffset.getDistance())
    }
    val position = watchCenter + positionOffset * zoom

    val textMeasurer = rememberTextMeasurer()


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
                Menu(settingsState) {
                    settingsOpen = false
                    toggleMenu(false)
                }
            }
            else
            {
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
                    val backgroundColor =
                        when(settingsState[INDEX_COLOR]) {
                            WHITE_MODE -> Color(0f,0f,0.2f,1f)
                            RED_MODE -> Color.Black
                            else -> Color(0f,0f,0.2f,1f)
                        }
                    val lightColor =
                        when(settingsState[INDEX_COLOR]) {
                            WHITE_MODE -> Color.White
                            RED_MODE -> Color.Red
                            else -> Color.White
                        }

                    // Background
                    drawCircle(color = backgroundColor, radius = WATCHFACE_RADIUS)

                    // Sky structures (galaxies, nebulae etc.)
                    if (zoom >= STRUCTURES_SHOW_ZOOM && settingsState[INDEX_DEEP_SKY] != DEEP_SKY_HIDE) {
                        drawSkyStructures(skyStructures, zoom, settingsState, position, mapAzimuth, upsideDown, textMeasurer)
                    }

                    // Stars
                    drawStars(stars, zoom, settingsState, position, mapAzimuth, upsideDown)

                    if (showConstellations(settingsState[INDEX_CONSTELLATION])) {
                        drawConstellations(
                            constellations,
                            stars,
                            zoom,
                            settingsState,
                            position,
                            mapAzimuth,
                            upsideDown
                        )
                    }

                    if (showConstellationsText(settingsState[INDEX_CONSTELLATION])) {
                        drawConstellationsNames(
                            constellations,
                            stars,
                            zoom,
                            settingsState,
                            position,
                            mapAzimuth,
                            upsideDown,
                            textMeasurer
                        )
                    }

                    // Planets
                    if (showPlanets(settingsState[INDEX_PLANET])) {
                        drawPlanets(planets, settingsState, zoom, position, mapAzimuth, upsideDown, textMeasurer)
                    }

                    // The Moon and the Sun
                    if (settingsState[INDEX_SUN_MOON] and FLAG_SUN > 0) {
                        drawSun(sun, settingsState, zoom, position, mapAzimuth, upsideDown)
                    }
                    if (settingsState[INDEX_SUN_MOON] and FLAG_MOON > 0) {
                        drawMoon(moon, backgroundColor, lightColor, zoom, position, mapAzimuth, upsideDown)
                    }

                    // Pointer to North
                    val myTextMeasure = textMeasurer.measure("N")
                    val myTextHeight = myTextMeasure.size.height.toFloat()
                    val myTextWidth = myTextMeasure.size.width.toFloat()
                    drawText(
                        myTextMeasure,
                        color = Color.Red,
                        topLeft = calculateNorthPosition(realAzimuth, upsideDown) - Offset( myTextWidth * 0.5f,myTextHeight * 0.5f)
                    )
                }
            }
        }
    }
}

fun DrawScope.drawStars(
    stars: HashMap<Int, Star>,
    zoom: Float,
    settingsState : SnapshotStateList<Int>,
    position : Offset,
    mapAzimuth: Float,
    upsideDown: Boolean) {
    val brightnessF = settingsState[INDEX_BRIGHTNESS].toFloat()
    val brightnessScaleFactor = 1f / maxStarAlpha(brightnessF)

    for(mEntry in stars)
    {
        val star = mEntry.value
        val color = star.getColor(zoom, settingsState[INDEX_COLOR], brightnessF, brightnessScaleFactor)
        if(color.alpha > 0)
        {
            drawCircle(
                color = color,
                radius = STAR_RADIUS,
                center = star.calculatePosition(position, zoom, -mapAzimuth, upsideDown)
            )
        }
    }
}
fun DrawScope.drawConstellations(
    constellations: ArrayList<Constellation>,
    stars: HashMap<Int, Star>,
    zoom: Float,
    settingsState : SnapshotStateList<Int>,
    position : Offset,
    mapAzimuth: Float,
    upsideDown: Boolean) {
    for (constellation in constellations) {
        for (line in constellation.lines) {
            val a_id = line.first
            val b_id = line.second
            val star_a: Star? = findStarById(stars, a_id);
            val star_b: Star? = findStarById(stars, b_id);
            if (star_a == null || star_b == null) {
                continue
            }

            val color = if (settingsState[INDEX_COLOR] == RED_MODE) Color.Red else Color.White
            val center_a = star_a.calculatePosition(position, zoom, -mapAzimuth, upsideDown)
            val center_b = star_b.calculatePosition(position, zoom, -mapAzimuth, upsideDown)

            drawLine(color.copy(alpha = 0.25F), center_a, center_b);
        }

    }
}

fun DrawScope.drawConstellationsNames(
    constellations: ArrayList<Constellation>,
    stars: HashMap<Int, Star>,
    zoom: Float,
    settingsState : SnapshotStateList<Int>,
    position : Offset,
    mapAzimuth: Float,
    upsideDown: Boolean,
    textMeasurer: TextMeasurer) {
    for (constellation in constellations) {
        val points = HashSet<Offset>()
        for (line in constellation.lines) {
            val a_id = line.first
            val b_id = line.second
            val star_a: Star? = findStarById(stars, a_id);
            val star_b: Star? = findStarById(stars, b_id);
            if (star_a == null || star_b == null) {
                continue
            }
            val center_a = star_a.calculatePosition(position, zoom, -mapAzimuth, upsideDown)
            val center_b = star_b.calculatePosition(position, zoom, -mapAzimuth, upsideDown)

            points.add(center_a)
            points.add(center_b)
        }

        val center_point = calculateCenter(points);

        val text : String = if (zoom > CONSTELLATIONS_SHOW_ZOOM) constellation.full_name else constellation.short_name
        val color = if (settingsState[INDEX_COLOR] == RED_MODE) Color.Red else Color.LightGray
        val textLayoutResult = makeConstellationTextMeasurer(text, color, textMeasurer);
        drawText(
            textLayoutResult,
            topLeft = center_point
        )


    }
}



fun DrawScope.drawPlanets(
    planets: ArrayList<Planet>,
    settingsState: SnapshotStateList<Int>,
    zoom: Float,
    position: Offset,
    mapAzimuth: Float,
    upsideDown: Boolean,
    textMeasurer: TextMeasurer
) {
    val textRectList : ArrayList<RectF> = ArrayList()
    for(planet in planets)
    {
        val center = planet.calculatePosition(position, zoom, -mapAzimuth, upsideDown)
        val color = when(settingsState[INDEX_COLOR]) {
            WHITE_MODE -> planet.color
            RED_MODE -> Color.Red
            else -> planet.color
        }

        drawCircle(
            color = color,
            radius = PLANET_RADIUS,
            center = center
        )
        if(showPlanetsText(settingsState[INDEX_PLANET])) {
            val text : String = if (zoom > NAME_CUTOFF_ZOOM) planet.name else planet.symbol.toString()
            val textLayoutResult = textMeasurer.measure(text)
            val topLeft = placePlanetText(
                center,
                textLayoutResult.size.width.toFloat(),
                textLayoutResult.size.height.toFloat(),
                textRectList
            )
            textRectList.add(RectF(
                topLeft.x,
                topLeft.y,
                topLeft.x + textLayoutResult.size.width.toFloat(),
                topLeft.y + textLayoutResult.size.height.toFloat()
            ))
            drawText(
                textLayoutResult,
                color = color,
                topLeft = topLeft
            )
        }
    }
}

fun DrawScope.drawMoon(
    moon: Moon,
    darkColor: Color,
    lightColor: Color,
    zoom: Float,
    position: Offset,
    mapAzimuth: Float,
    upsideDown: Boolean) {
    val pos = moon.calculatePosition(position, zoom, -mapAzimuth, upsideDown)
    val rotateAngle = moon.angle - mapAzimuth
    // It is easier to transform a set path than to include angles and offsets in the path building
    withTransform({
        scale(if (upsideDown) -1f else 1f, 1f, pos)
        rotateRad(rotateAngle, pos)
        translate(pos.x, pos.y)
    }) {
        drawMoonFace(moon,darkColor, lightColor)
    }
}

fun DrawScope.drawSun(
    sun: Sun,
    settingsState: SnapshotStateList<Int>,
    zoom: Float,
    position: Offset,
    mapAzimuth: Float,
    upsideDown: Boolean,
) {
        val center = sun.calculatePosition(position, zoom, -mapAzimuth, upsideDown)
        val color = when(settingsState[INDEX_COLOR]) {
            WHITE_MODE -> sun.color
            RED_MODE -> sun.colorRedMode
            else -> sun.color
        }

        drawCircle(
            color = color,
            radius = SUN_RADIUS,
            center = center
        )
}

fun DrawScope.drawMoonFace(moon: Moon, darkColor: Color, lightColor : Color) {
    drawCircle(
        color = blendColors(darkColor, MOON_DARK_COLOR),
        radius = MOON_RADIUS,
        center = Offset(0f,0f)
    )
    val path : Path = Path()
    path.moveTo(0f, MOON_RADIUS)

    val waxp = moon.getWaxPoint() * MOON_RADIUS
    val wanp = moon.getWanePoint() * MOON_RADIUS

    path.addMoonArc(waxp, 1f)
    path.addMoonArc(wanp, -1f)
    path.close()
    drawPath(path, lightColor, style = Fill)
}

fun Path.addMoonArc(arcApexX : Float, dir : Float) {
    // In case drawing an oval of width close to 0 causes errors
    if (abs(arcApexX) < 1e-3) {
        lineTo(0f, -MOON_RADIUS * dir)
        return
    }
    var offset = 0f
    if (arcApexX < 0f) {
        // If the arc apex is to negative, the arc needs to go the other way
        offset = PI.toFloat()
    }
    val angle = PI.toFloat() * 0.5f
    val arcLeftX = - abs(arcApexX)
    val arcRightX = abs(arcApexX)
    arcToRad(
        Rect(arcLeftX, -MOON_RADIUS, arcRightX,  MOON_RADIUS),
        -angle * dir + offset,
        2f * angle * dir,
        false
    )
}

fun blendColors(c1: Color, c2: Color): Color{
    val r = (c1.red + c2.red) / 2
    val g = (c1.green + c2.green) / 2
    val b = (c1.blue + c2.blue) / 2
    return Color(r, g, b)
}

fun DrawScope.drawSkyStructures(
    skyStructures: ArrayList<SkyStructures>,
    zoom: Float,
    settingsState : SnapshotStateList<Int>,
    position : Offset,
    mapAzimuth: Float,
    upsideDown: Boolean,
    textMeasurer: TextMeasurer) {
    for(structure in skyStructures)
    {
        // Currently passing dummy values to arguments that are ignored in the overriden function
        val color = structure.getColor(zoom, settingsState[INDEX_COLOR], 0f,1f)
        if(color.alpha > 0)
        {
            val center = structure.calculatePosition(position, zoom, -mapAzimuth, upsideDown)
            if (isCloseToScreen(center)) {
                val symbol : String = "" + structure.symbol
                val symbolMeasured = makeStructureTextMeasurer(symbol, color, textMeasurer)
                drawText(
                    symbolMeasured,
                    topLeft = center
                )

                if (zoom >= STRUCTURES_SHOW_TEXT_ZOOM && settingsState[INDEX_DEEP_SKY] == DEEP_SKY_SHOW_TEXT) {
                    val nameMeasured = makeStructureTextMeasurer(structure.name, color, textMeasurer)
                    val xOffset = (symbolMeasured.size.width.toFloat() - nameMeasured.size.width.toFloat()) * 0.5f
                    val yOffset = symbolMeasured.size.height.toFloat() * 0.8f
                    drawText(
                        nameMeasured,
                        topLeft = center + Offset(xOffset, yOffset)
                    )
                }
            }
        }
    }
}

fun isCloseToScreen(o: Offset) : Boolean {
    return  -10f <= o.x && o.x <= 2f * WATCHFACE_RADIUS + 10f &&
            -10f <= o.y && o.y <= 2f * WATCHFACE_RADIUS + 10f
}

fun makeStructureTextMeasurer(text: String, color: Color, textMeasurer: TextMeasurer) : TextLayoutResult{
    return textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = color,
            fontSize = STRUCTURES_FONT_SIZE.sp
        )
    )
}

fun makeConstellationTextMeasurer(text: String, color: Color, textMeasurer: TextMeasurer) : TextLayoutResult{
    return textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = color,
            fontSize = CONSTELLATION_FONT_SIZE.sp
        )
    )
}

@Composable
fun WaitingScreen() {
    val a = remember {
        Animatable(0f)
    }
    LaunchedEffect("animation") {
        a.animateTo(
            2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOut),
                repeatMode = RepeatMode.Restart
            )
        )
    }
    SkyMapTheme {
        Box (modifier = Modifier
            .fillMaxSize()
            .background(Color(0f,0f,0.2f,1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Waiting for GPS...",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = WATCHFACE_RADIUS * 0.9f
            for (f in arrayOf(1f, 2f, 3f)) {
                val sa = f * a.value
                drawCircle(Color.White, 4f, Offset(
                    WATCHFACE_RADIUS + sin(sa) * r,
                    WATCHFACE_RADIUS - cos(sa) * r
                ))
            }
        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(HashMap(), ArrayList(), ArrayList(), Moon(0f, 0f, 0.0, 0.0), Sun(0.0, 0.0), ArrayList(), PackedFloat(1f), 0f, 0f, false) {

    }
}
