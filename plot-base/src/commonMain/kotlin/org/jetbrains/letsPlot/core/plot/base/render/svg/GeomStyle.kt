/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.core.plot.base.render.svg

import org.jetbrains.letsPlot.commons.geometry.DoubleRectangle
import org.jetbrains.letsPlot.commons.geometry.DoubleVector
import org.jetbrains.letsPlot.core.plot.base.geom.util.approximateArc
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

interface GeomStyle {
    fun path(points: List<DoubleVector>): List<DoubleVector>

    fun rectangle(rect: DoubleRectangle): List<DoubleVector> =
        path(rectanglePoints(rect))

    fun circle(center: DoubleVector, radius: Double): List<DoubleVector> =
        path(approximateCircle(center, radius))

    fun fillScribble(boundary: List<DoubleVector>): List<List<DoubleVector>> = emptyList()

    object Regular : GeomStyle {
        override fun path(points: List<DoubleVector>): List<DoubleVector> = points
    }

    class Xkcd(private val fillPattern: FillPattern = FillPattern.CrossHatch) : GeomStyle {
        override fun path(points: List<DoubleVector>): List<DoubleVector> =
            XkcdPathEffect.toHandDrawn(points)

        override fun fillScribble(boundary: List<DoubleVector>): List<List<DoubleVector>> =
            fillPattern.generate(boundary).map { path(it) }
    }
}

private fun rectanglePoints(rect: DoubleRectangle): List<DoubleVector> = listOf(
    DoubleVector(rect.left, rect.top),
    DoubleVector(rect.right, rect.top),
    DoubleVector(rect.right, rect.bottom),
    DoubleVector(rect.left, rect.bottom),
    DoubleVector(rect.left, rect.top)
)

private fun approximateCircle(center: DoubleVector, radius: Double): List<DoubleVector> {
    val arcPoint = { angle: Double ->
        center.add(DoubleVector(radius * cos(angle), radius * sin(angle)))
    }
    val right = arcPoint(0.0)
    val left = arcPoint(PI)
    val upperArc = approximateArc(right, left, 0.0, PI, arcPoint)
    val lowerArc = approximateArc(left, right, PI, 2.0 * PI, arcPoint)
    return upperArc + lowerArc.drop(1)
}
