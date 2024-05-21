package com.example.skymap.presentation

import android.graphics.RectF
import android.util.Log
import androidx.compose.ui.geometry.Offset

/**
 * Checks whether two ranges: [[l1], [r1]] and [[l2], [r2]] have a point in common
 */
private fun collisionRange(l1 : Float, r1 : Float, l2 : Float, r2 : Float) : Boolean {
    return l1 <= r2 && l2 <= r1
}

/**
 * Checks whether two rectangles collide (have any point in common)
 */
private fun collisionRect(x : RectF, y : RectF) : Boolean {
    return collisionRange(x.top, x.bottom, y.top, y.bottom) && collisionRange(x.left, x.right, y.left, y.right)
}

/**
 * Checks whether [x] collides with any rectangle in [ls]
 */
private fun collisionRectList(x : RectF, ls : ArrayList<RectF>) : Boolean {
    for (y in ls) {
        if (collisionRect(x, y)) {
            return true
        }
    }
    return false
}

/**
 * Creates a rectangle from a center offset and the width and height
 */
private fun rectFromCenter(center: Offset, width: Float, height: Float) : RectF {
    return RectF(
        center.x - width / 2,
        center.y - height /2,
        center.x + width / 2,
        center.y + height / 2)
}

/**
 * Calculates where to place a new name of a planet as to not collide with the previous names.
 * Note: this function is not designed to be used for anything other than planets.
 * @param planetPos the offset representing the position of the planet on screen.
 * @param width the width of the text
 * @param height the height of the text
 * @param takenRectList a list of rectangles that represent regions that are already occupied by text
 * @return the offset representing where to place the top left corner of the text
 */
fun placePlanetText(planetPos : Offset, width : Float, height : Float, takenRectList: ArrayList<RectF>) : Offset {
    val halfWidth = 1f + width / 2
    val halfHeight = 2f + height / 2
    // Try to place the name in 42 different places, prioritize places near the planet
    val grid = arrayOf(
        Offset(0f,1f),
        Offset(1f,1f),
        Offset(-1f,1f),
        Offset(0f,-1f),
        Offset(1f,-1f),
        Offset(-1f,-1f),
    )
    for (d in arrayOf(1f, 1.5f, 2f, 2.5f, 3f, 3.5f, 4f)) {
        for (o in grid) {
            val dx = o.x * d * halfWidth
            val dy = o.y * d * halfHeight
            val rect = rectFromCenter(planetPos + Offset(dx,dy), width, height)
            if (!collisionRectList(
                    rect
                    , takenRectList
                )) {
                return Offset(rect.left, rect.top)
            }
        }
    }
    // If that fails, just place it somewhere far away
    val emergencyRect = rectFromCenter(planetPos + Offset(3 * width,3 * height), width, height)
    return  Offset(emergencyRect.left, emergencyRect.top)
}