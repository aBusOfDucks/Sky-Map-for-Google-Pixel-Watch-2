package com.example.skymap.presentation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.gson.JsonArray

class Constellation (short_name: String, full_name: String, lines: ArrayList<Pair<Int, Int> >) {
    val short_name: String = short_name
    val full_name: String = full_name
    val lines: ArrayList<Pair<Int, Int> > = lines
}

fun calculateConstellations(constellationsArray: com.google.gson.JsonArray?): ArrayList<Constellation> {
    val constellations: ArrayList<Constellation> = ArrayList()
    constellationsArray?.forEach { constellation ->
        val constellationJsonObject = constellation.asJsonObject
        val short_name: String = constellationJsonObject.getAsJsonPrimitive("short_name").asString
        val full_name: String = constellationJsonObject.getAsJsonPrimitive("full_name").asString
        val linesJsonArray: JsonArray = constellationJsonObject.getAsJsonArray("lines")

        val linesList: ArrayList<Pair<Int, Int> > = ArrayList()
        linesJsonArray.forEach { line ->
            val lineJsonObject = line.asJsonObject
            val id_a = lineJsonObject.getAsJsonPrimitive("id_a").asInt
            val id_b = lineJsonObject.getAsJsonPrimitive("id_b").asInt
            linesList.add(Pair(id_a, id_b))
        }
        
        val constellationObj = Constellation(short_name, full_name, linesList)
        constellations.add(constellationObj)
    }

    return constellations
}

fun calculateCenter(points: HashSet<Offset>): Offset {
    val n = points.size
    if (n == 0)
        return Offset(0F, 0F)

    var centerX = 0f
    var centerY = 0f

    for (point in points) {
        centerX += point.x
        centerY += point.y
    }

    centerX /= n
    centerY /= n

    return Offset(centerX, centerY)
}