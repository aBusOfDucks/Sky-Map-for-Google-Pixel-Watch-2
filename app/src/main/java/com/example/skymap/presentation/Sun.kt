package com.example.skymap.presentation

import androidx.compose.ui.graphics.Color

class Sun(azimuth : Double, altitude : Double) : SkyPoint(azimuth,altitude) {
    override val color : Color = Color(255,255,0)
    override val colorRedMode: Color = Color(255, 60, 0)
}

fun calculateSun() : Sun {
    //TODO: Add Sun calculation here
    return Sun(1.0, 0.5)
}