/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.core.plot.base.render.svg

import org.jetbrains.letsPlot.commons.geometry.DoubleRectangle
import org.jetbrains.letsPlot.commons.geometry.DoubleVector
import org.jetbrains.letsPlot.commons.values.Color
import org.jetbrains.letsPlot.core.plot.base.geom.util.approximateArc
import org.jetbrains.letsPlot.datamodel.svg.dom.SvgGElement
import org.jetbrains.letsPlot.datamodel.svg.dom.SvgNode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

interface GeomStyle {
    fun path(points: List<DoubleVector>): List<DoubleVector>

    fun rectangle(rect: DoubleRectangle): List<DoubleVector> =
        path(rectanglePoints(rect))

    fun circle(center: DoubleVector, radius: Double): List<DoubleVector> =
        path(approximateCircle(center, radius))

    /**
     * Fills a shape and returns the node to put into the SVG tree.
     *
     * The default just paints the shape with a solid color. A style that fills with its own
     * geometry (e.g. xkcd hatching) overrides this and builds its own node.
     *
     * @param solid paints the shape with a solid color and returns it.
     * @param outline paints only the shape's border (no fill) and returns it, or null if the
     *                shape draws its border separately.
     */
    fun fill(
        boundary: List<DoubleVector>,
        fillColor: () -> Color,
        solid: () -> SvgNode,
        outline: () -> SvgNode?
    ): SvgNode = solid()

    object Regular : GeomStyle {
        override fun path(points: List<DoubleVector>): List<DoubleVector> = points
    }

    class Xkcd(private val fillPattern: FillPattern = FillPattern.CrossHatch) : GeomStyle {
        override fun path(points: List<DoubleVector>): List<DoubleVector> =
            XkcdPathEffect.toHandDrawn(points)

        override fun fill(
            boundary: List<DoubleVector>,
            fillColor: () -> Color,
            solid: () -> SvgNode,
            outline: () -> SvgNode?
        ): SvgNode {
            val scribble = fillPattern.generate(boundary).map { path(it) }
            if (scribble.isEmpty()) {
                return solid()  // FillPattern.None: fall back to a solid fill
            }
            return scribbleGroup(outline(), scribble, fillColor())
        }
    }
}

private const val SCRIBBLE_STROKE_WIDTH = 1.0

// Packs the optional border node and the scribble strokes into a single group.
private fun scribbleGroup(
    border: SvgNode?,
    scribble: List<List<DoubleVector>>,
    fillColor: Color
): SvgGElement {
    val group = SvgGElement()
    border?.let { group.children().add(it) }
    for (stroke in scribble) {
        val strokePath = LinePath.line(stroke)
        strokePath.color().set(fillColor)
        strokePath.width().set(SCRIBBLE_STROKE_WIDTH)
        group.children().add(strokePath.rootGroup)
    }
    return group
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
