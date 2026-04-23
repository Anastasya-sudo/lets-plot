/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.core.plot.base.geom

import org.jetbrains.letsPlot.commons.geometry.DoubleVector
import org.jetbrains.letsPlot.commons.interval.DoubleSpan
import org.jetbrains.letsPlot.commons.values.Color
import org.jetbrains.letsPlot.commons.values.Colors
import org.jetbrains.letsPlot.core.FeatureSwitch
import org.jetbrains.letsPlot.core.plot.base.*
import org.jetbrains.letsPlot.core.plot.base.aes.AesScaling
import org.jetbrains.letsPlot.core.plot.base.aes.AestheticsUtil
import org.jetbrains.letsPlot.core.plot.base.geom.legend.LollipopLegendKeyElementFactory
import org.jetbrains.letsPlot.core.plot.base.geom.util.approximateArc
import org.jetbrains.letsPlot.core.plot.base.geom.util.GeomHelper
import org.jetbrains.letsPlot.core.plot.base.geom.util.GeomUtil.toLocation
import org.jetbrains.letsPlot.core.plot.base.geom.util.HintColorUtil
import org.jetbrains.letsPlot.core.plot.base.render.LegendKeyElementFactory
import org.jetbrains.letsPlot.core.plot.base.render.SvgRoot
import org.jetbrains.letsPlot.core.plot.base.render.point.NamedShape
import org.jetbrains.letsPlot.core.plot.base.render.point.PointShapeSvg
import org.jetbrains.letsPlot.core.plot.base.render.svg.LinePath
import org.jetbrains.letsPlot.core.plot.base.render.svg.XkcdPathEffect
import org.jetbrains.letsPlot.core.plot.base.render.svg.lineString
import org.jetbrains.letsPlot.core.plot.base.tooltip.GeomTargetCollector
import org.jetbrains.letsPlot.datamodel.svg.dom.SvgGElement
import org.jetbrains.letsPlot.datamodel.svg.dom.SvgLineElement
import org.jetbrains.letsPlot.datamodel.svg.dom.SvgNode
import org.jetbrains.letsPlot.datamodel.svg.dom.SvgPathDataBuilder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class LollipopGeom : GeomBase(), WithWidth, WithHeight {
    var fatten: Double = DEF_FATTEN
    var slope: Double = DEF_SLOPE
    var intercept: Double = DEF_INTERCEPT
    var direction: Direction = DEF_DIRECTION

    override val legendKeyElementFactory: LegendKeyElementFactory
        get() = LollipopLegendKeyElementFactory()

    override fun rangeIncludesZero(aes: Aes<*>): Boolean {
        // Pin the lollipops to an axis when baseline coincides with this axis and sticks are perpendicular to it
        return aes == Aes.Y
                && slope == 0.0
                && intercept == 0.0
                && direction != Direction.ALONG_AXIS
    }

    override fun buildIntern(
        root: SvgRoot,
        aesthetics: Aesthetics,
        pos: PositionAdjustment,
        coord: CoordinateSystem,
        ctx: GeomContext
    ) {
        val helper = GeomHelper(pos, coord, ctx)
        val targetCollector = getGeomTargetCollector(ctx)
        val colorsByDataPoint = HintColorUtil.createColorMarkerMapper(GeomKind.LOLLIPOP, ctx)

        val lollipops = mutableListOf<Lollipop>()
        for (p in aesthetics.dataPoints()) {
            val head = p.toLocation(Aes.X, Aes.Y) ?: continue
            val base = getBase(head)
            val stickLength = sqrt((head.x - base.x).pow(2) + (head.y - base.y).pow(2))
            lollipops.add(Lollipop(p, head, base, stickLength))
        }
        // Sort lollipops to better displaying when they are intersects
        for (lollipop in lollipops.sortedByDescending { it.length }) {
            val stick = lollipop.createStick(helper)
            if (stick != null) {
                root.add(stick)
            }
            root.add(lollipop.createCandy(helper))
            buildHint(lollipop, helper, targetCollector, colorsByDataPoint)
        }
    }

    private fun buildHint(
        lollipop: Lollipop,
        helper: GeomHelper,
        targetCollector: GeomTargetCollector,
        colorsByDataPoint: (DataPointAesthetics) -> List<Color>
    ) {
        targetCollector.addPoint(
            lollipop.point.index(),
            helper.toClient(lollipop.head, lollipop.point)!!,
            lollipop.candyRadius,
            GeomTargetCollector.TooltipParams(
                markerColors = colorsByDataPoint(lollipop.point)
            )
        )
    }

    override fun widthSpan(
        p: DataPointAesthetics,
        coordAes: Aes<Double>,
        resolution: Double,
        isDiscrete: Boolean
    ): DoubleSpan? {
        val loc = p.toLocation(Aes.X, Aes.Y) ?: return null

        val head = loc.flipIf(coordAes == Aes.Y)
        return DoubleSpan(getBase(head).x, head.x)
    }

    override fun heightSpan(
        p: DataPointAesthetics,
        coordAes: Aes<Double>,
        resolution: Double,
        isDiscrete: Boolean
    ): DoubleSpan? {
        val loc = p.toLocation(Aes.X, Aes.Y) ?: return null

        val head = loc.flipIf(coordAes == Aes.X)
        return DoubleSpan(getBase(head).y, head.y)
    }

    private fun getBase(head: DoubleVector): DoubleVector {
        return when (direction) {
            Direction.ORTHOGONAL_TO_AXIS -> DoubleVector(head.x, slope * head.x + intercept)
            Direction.ALONG_AXIS -> {
                if (slope == 0.0) {
                    DoubleVector(intercept, head.y)
                } else {
                    DoubleVector((head.y - intercept) / slope, head.y)
                }
            }

            Direction.SLOPE -> {
                val baseX = (head.x + slope * (head.y - intercept)) / (1 + slope.pow(2))
                val baseY = slope * baseX + intercept
                DoubleVector(baseX, baseY)
            }
        }
    }

    private inner class Lollipop(
        val point: DataPointAesthetics,
        val head: DoubleVector,
        val base: DoubleVector,
        val length: Double
    ) {
        val candyRadius: Double
            get() {
                val shape = point.shape()!!
                val shapeCoeff = when (shape) {
                    NamedShape.STICK_PLUS,
                    NamedShape.STICK_STAR,
                    NamedShape.STICK_CROSS -> 0.0

                    else -> 1.0
                }
                return (shape.size(point, fatten) + shapeCoeff * shape.strokeWidth(point)) / 2.0
            }

        fun createCandy(helper: GeomHelper): SvgGElement {
            val location = helper.toClient(head, point)!!
            val shape = point.shape()!!
            if (FeatureSwitch.XKCD_STYLE_ENABLED && shape is NamedShape && shape.isCircleCandyShape()) {
                return createXkcdCircleCandy(shape, location)
            } else {
                val o = PointShapeSvg.create(shape, location, point, fatten)
                return wrap(o)
            }
        }

        fun createStick(helper: GeomHelper): SvgNode? {
            val clientBase = helper.toClient(base, point) ?: return null // base of the lollipop stick
            val clientHead = helper.toClient(head, point) ?: return null // center of the lollipop candy
            val stickLength = sqrt((clientHead.x - clientBase.x).pow(2) + (clientHead.y - clientBase.y).pow(2))
            if (candyRadius > stickLength) {
                return null
            }
            val neck = shiftHeadToBase(clientBase, clientHead, candyRadius) // meeting point of candy and stick
            if (FeatureSwitch.XKCD_STYLE_ENABLED) {
                val svgElementHelper = GeomHelper.SvgElementHelper()
                    .setStrokeAlphaEnabled(true)
                return svgElementHelper.createLine(clientBase, neck, point, strokeScaler = AesScaling::lineWidth)?.first
            } else {
                val line = SvgLineElement(clientBase.x, clientBase.y, neck.x, neck.y)
                GeomHelper.decorate(line, point, applyAlphaToAll = true, strokeScaler = AesScaling::lineWidth)
                return line
            }
        }

        private fun shiftHeadToBase(
            clientBase: DoubleVector,
            clientHead: DoubleVector,
            shiftLength: Double
        ): DoubleVector {
            val x0 = clientBase.x
            val x1 = clientHead.x
            val y0 = clientBase.y
            val y1 = clientHead.y

            if (x0 == x1) {
                val dy = if (y0 < y1) -shiftLength else shiftLength
                return DoubleVector(x1, y1 + dy)
            }
            val dx = sqrt(shiftLength.pow(2) / (1.0 + (y0 - y1).pow(2) / (x0 - x1).pow(2)))
            val x = if (x0 < x1) {
                x1 - dx
            } else {
                x1 + dx
            }
            val y = (x - x1) * (y0 - y1) / (x0 - x1) + y1

            return DoubleVector(x, y)
        }

        private fun createXkcdCircleCandy(shape: NamedShape, center: DoubleVector): SvgGElement {
            val radius = shape.size(point, fatten) / 2.0
            if (!radius.isFinite() || radius <= 0.0) {
                val o = PointShapeSvg.create(shape, center, point, fatten)
                return wrap(o)
            }

            val circlePath = approximateCircle(center, radius)
            val handDrawn = XkcdPathEffect.toHandDrawn(circlePath)

            val path = LinePath(
                SvgPathDataBuilder().apply {
                    lineString(handDrawn)
                    closePath()
                }
            )

            val fill = AestheticsUtil.fill(shape.isFilled, shape.isSolid, point)
            val fillAlpha = if (shape.isFilled || shape.isSolid) {
                AestheticsUtil.alpha(fill, point)
            } else {
                0.0
            }
            path.fill().set(Colors.withOpacity(fill, fillAlpha))

            val stroke = point.color()!!
            val strokeWidth = shape.strokeWidth(point)
            val strokeAlpha = if (strokeWidth > 0.0) {
                AestheticsUtil.alpha(stroke, point)
            } else {
                0.0
            }
            path.color().set(Colors.withOpacity(stroke, strokeAlpha))
            path.width().set(strokeWidth)

            return path.rootGroup
        }

        private fun approximateCircle(center: DoubleVector, radius: Double): List<DoubleVector> {
            val arcPoint = { angle: Double ->
                center.add(DoubleVector(radius * cos(angle), radius * sin(angle)))
            }

            val right = arcPoint(0.0)
            val left = arcPoint(PI)

            val upperArc = approximateArc(
                startPoint = right,
                endPoint = left,
                startAngle = 0.0,
                endAngle = PI,
                arcPoint = arcPoint
            )
            val lowerArc = approximateArc(
                startPoint = left,
                endPoint = right,
                startAngle = PI,
                endAngle = 2.0 * PI,
                arcPoint = arcPoint
            )

            return upperArc + lowerArc.drop(1)
        }

        private fun NamedShape.isCircleCandyShape(): Boolean {
            return this == NamedShape.STICK_CIRCLE
                    || this == NamedShape.SOLID_CIRCLE
                    || this == NamedShape.SOLID_CIRCLE_2
                    || this == NamedShape.BULLET
                    || this == NamedShape.FILLED_CIRCLE
        }
    }

    enum class Direction {
        ORTHOGONAL_TO_AXIS, ALONG_AXIS, SLOPE
    }

    companion object {
        const val DEF_FATTEN = 2.5
        const val DEF_SLOPE = 0.0
        const val DEF_INTERCEPT = 0.0
        val DEF_DIRECTION = Direction.ORTHOGONAL_TO_AXIS

        const val HANDLES_GROUPS = PointGeom.HANDLES_GROUPS
    }
}
