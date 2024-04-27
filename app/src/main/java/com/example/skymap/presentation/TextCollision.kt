package com.example.skymap.presentation

import android.graphics.RectF
import android.util.Log
import androidx.compose.ui.geometry.Offset

private fun collisionRange(l1 : Float, r1 : Float, l2 : Float, r2 : Float) : Boolean {
    return l1 <= r2 && l2 <= r1
}

private fun collisionRect(x : RectF, y : RectF) : Boolean {
    return collisionRange(x.top, x.bottom, y.top, y.bottom) && collisionRange(x.left, x.right, y.left, y.right)
}


private fun collisionRectList(x : RectF, ls : ArrayList<RectF>) : Boolean {
    for (y in ls) {
        if (collisionRect(x, y)) {
            return true
        }
    }
    return false
}

private fun rectFromCenter(center: Offset, width: Float, height: Float) : RectF {
    return RectF(
        center.x - width / 2,
        center.y - height /2,
        center.x + width / 2,
        center.y + height / 2)
}

fun placePlanetText(planetPos : Offset, width : Float, height : Float, takenRectList: ArrayList<RectF>) : Offset {
    val halfWidth = 1f + width / 2
    val halfHeight = 2f + height / 2
    for (dx in arrayOf(0f, halfWidth, -halfWidth, 3 * halfWidth, -3 * halfWidth)) {
        for (dy in arrayOf(halfHeight, - halfHeight, 3 * halfHeight, -3 * halfHeight)) {
            val rect = rectFromCenter(planetPos + Offset(dx,dy), width, height)
            if (!collisionRectList(
                    rect
                    , takenRectList
                )) {
                return Offset(rect.left, rect.top)
            }
        }
    }
    val emergencyRect = rectFromCenter(planetPos + Offset(2 * width,2 * height), width, height)
    return  Offset(emergencyRect.left, emergencyRect.top)
}