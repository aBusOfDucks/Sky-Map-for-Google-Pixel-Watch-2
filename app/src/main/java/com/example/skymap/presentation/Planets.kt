package com.example.skymap.presentation

import androidx.compose.ui.graphics.Color

fun calculatePlanets() : ArrayList<Planet>{
    //TODO: Add planet calculation here
    val planets : ArrayList<Planet> = ArrayList()
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
    return planets
}

class Planet(
    val name : String,
    val symbol : Char,
    val col : Color,
    azimuth: Double,
    altitude: Double)
    : SkyPoint(azimuth, altitude) {
}