package com.example.skymap.presentation

import androidx.compose.ui.graphics.Color
import com.google.gson.JsonArray

class Constellation (name: String, lines_cnt: Int, lines: ArrayList<Pair<Int, Int> >) {
    val name: String = name
    val lines_cnt: Int = lines_cnt
    val lines: ArrayList<Pair<Int, Int> > = lines
}

fun calculateConstellations(constellationsArray: com.google.gson.JsonArray?): ArrayList<Constellation> {
    val constellations: ArrayList<Constellation> = ArrayList()
    constellationsArray?.forEach { constellation ->
        val constellationJsonObject = constellation.asJsonObject
        val name: String = constellationJsonObject.getAsJsonPrimitive("name").asString
        val lines_cnt: Int = constellationJsonObject.getAsJsonPrimitive("lines_cnt").asInt
        val linesJsonArray: JsonArray = constellationJsonObject.getAsJsonArray("lines")

        val linesList: ArrayList<Pair<Int, Int> > = ArrayList()
        linesJsonArray.forEach { line ->
            val lineJsonObject = line.asJsonObject
            val id_a = lineJsonObject.getAsJsonPrimitive("id_a").asInt
            val id_b = lineJsonObject.getAsJsonPrimitive("id_b").asInt
            linesList.add(Pair(id_a, id_b))
        }
        
        val constellationObj = Constellation(name, lines_cnt, linesList)
        constellations.add(constellationObj)
    }

    return constellations
}