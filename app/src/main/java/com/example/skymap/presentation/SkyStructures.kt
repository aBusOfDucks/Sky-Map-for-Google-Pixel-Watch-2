package com.example.skymap.presentation

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

class SkyStructures(mag : Double, azimuth: Double, altitude: Double) : Star(mag, azimuth, altitude) {
    // TODO: change default emojis
    var symbol: Char = Char(0x2728)
    var name: String = ""

    // placeholder to test rendering
    // TODO: change / delete it and replace with real data
    init {
            if(Random.nextBoolean())
               symbol = Char(0x2B50)
        for(i in (0..4)) {
            name += Char(Random.nextInt(48, 120))
        }
    }

    override fun getColor(zoom: Float, colorSetting: Int, brightnessFactor : Float): Color {
        return when(colorSetting) {
                WHITE_MODE -> Color.White
                RED_MODE -> Color.Red
                else -> Color.White
            }.copy(alpha = 0.7f)
    }
}

fun getSkyStructures() : ArrayList<SkyStructures> {

    // placeholder to test rendering
    // TODO: change / delete it and replace with real data
    val ans = ArrayList<SkyStructures>()
    for(i in (0..10)) {
        val m = Random.nextDouble(3.14)
        val az = Random.nextDouble(3.14)
        val al = Random.nextDouble(3.14)
        val temp = SkyStructures(m, az, al)
        ans.add(temp)
    }
    return ans
}