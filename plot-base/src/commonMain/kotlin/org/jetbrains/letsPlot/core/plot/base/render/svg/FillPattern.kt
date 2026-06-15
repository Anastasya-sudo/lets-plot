/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.core.plot.base.render.svg

import org.jetbrains.letsPlot.commons.geometry.DoubleVector
import kotlin.math.PI

/**
 * Turns a closed shape boundary into the polylines that fill it with a marker scribble
 * (an alternative to a solid `fill` color).
 *
 * Each returned element is one stroke (a list of points). An empty result means "no scribble"
 */
interface FillPattern {
    fun generate(boundary: List<DoubleVector>): List<List<DoubleVector>>

    object None : FillPattern {
        override fun generate(boundary: List<DoubleVector>): List<List<DoubleVector>> = emptyList()
    }

    object CrossHatch : FillPattern {
        override fun generate(boundary: List<DoubleVector>): List<List<DoubleVector>> =
            hatchLines(boundary, PI / 4) + hatchLines(boundary, -PI / 4)
    }
}

private const val HATCH_SPACING = 6.0

/**
 * Parallel lines at the given [angle], spaced [HATCH_SPACING] apart, clipped to the [boundary].
 *
 * The shape is rotated so the lines become horizontal scan-lines; for each scan-line the spans
 * lying inside the shape are found by edge crossings; then the spans are rotated back.
 */
private fun hatchLines(boundary: List<DoubleVector>, angle: Double): List<List<DoubleVector>> {
    if (boundary.size < 3) return emptyList()

    val rotated = boundary.map { it.rotate(-angle) }
    val top = rotated.minOf { it.y }
    val bottom = rotated.maxOf { it.y }

    val strokes = ArrayList<List<DoubleVector>>()
    var y = top + HATCH_SPACING
    while (y < bottom) {
        val crossings = edgeCrossings(rotated, y).sorted()
        // Consecutive crossings bound the inside spans: [0,1], [2,3], ...
        var i = 0
        while (i + 1 < crossings.size) {
            val start = DoubleVector(crossings[i], y).rotate(angle)
            val end = DoubleVector(crossings[i + 1], y).rotate(angle)
            strokes.add(listOf(start, end))
            i += 2
        }
        y += HATCH_SPACING
    }
    return strokes
}

/** X-coordinates where the horizontal line at [y] crosses the polygon edges. */
private fun edgeCrossings(polygon: List<DoubleVector>, y: Double): List<Double> {
    val crossings = ArrayList<Double>()
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[(i + 1) % polygon.size]
        // Half-open interval [min, max) counts a shared vertex once.
        if (y >= minOf(a.y, b.y) && y < maxOf(a.y, b.y)) {
            val t = (y - a.y) / (b.y - a.y)
            crossings.add(a.x + t * (b.x - a.x))
        }
    }
    return crossings
}
