/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.core.plot.base.geom

import org.jetbrains.letsPlot.commons.geometry.DoubleVector
import org.jetbrains.letsPlot.core.plot.base.*
import org.jetbrains.letsPlot.core.plot.base.aes.AesScaling
import org.jetbrains.letsPlot.core.plot.base.geom.util.GeomHelper
import org.jetbrains.letsPlot.core.plot.base.geom.util.GeomUtil
import org.jetbrains.letsPlot.core.plot.base.geom.util.HintColorUtil
import org.jetbrains.letsPlot.core.plot.base.geom.util.LinesHelper
import org.jetbrains.letsPlot.core.plot.base.render.SvgRoot
import org.jetbrains.letsPlot.core.plot.base.render.svg.LinePath
import org.jetbrains.letsPlot.core.plot.base.tooltip.GeomTargetCollector
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class NgonGeom : GeomBase() {
    var sideCount: Int = DEF_SIDE_COUNT
    var sizeUnit: String? = null

    override val geomName: String = "ngon"

    override fun prepareDataPoints(dataPoints: Iterable<DataPointAesthetics>): Iterable<DataPointAesthetics> {
        return GeomUtil.withDefined(dataPoints, Aes.X, Aes.Y, Aes.SIZE)
    }

    override fun buildIntern(
        root: SvgRoot,
        aesthetics: Aesthetics,
        pos: PositionAdjustment,
        coord: CoordinateSystem,
        ctx: GeomContext
    ) {
        val helper = GeomHelper(pos, coord, ctx)
        val linesHelper = LinesHelper(pos, coord, ctx)
        val targetCollector = getGeomTargetCollector(ctx)
        val colorsByDataPoint = HintColorUtil.createColorMarkerMapper(GeomKind.NGON, ctx)

        val dataPoints = dataPoints(aesthetics)
        var goodPointsCount = 0

        for (p in dataPoints) {
            val center = p.finiteVectorOrNull(Aes.X, Aes.Y) ?: continue
            val clientCenter = helper.toClient(center, p) ?: continue

            val scaleFactor = if (sizeUnit.isNullOrBlank()) {
                ctx.getScaleFactor()
            } else {
                AesScaling.sizeUnitRatio(center, coord, sizeUnit, AesScaling.POINT_UNIT_SIZE)
            }

            val radius = AesScaling.circleDiameter(p) * scaleFactor / 2.0
            if (!radius.isFinite() || radius <= 0.0) continue

            val sideCount = p[Aes.SIDECOUNT]
                ?.takeIf { it.isFinite() }
                ?.roundToInt()
                ?: this.sideCount

            val polygonPoints = polygon(clientCenter, radius, sideCount)

            val path = LinePath.polygon(polygonPoints)
            linesHelper.decorate(path, p, filled = true) {
                AesScaling.strokeWidth(it, DataPointAesthetics::stroke)
            }

            root.add(path.rootGroup)
            targetCollector.addPolygon(
                points = polygonPoints,
                index = p.index(),
                tooltipParams = GeomTargetCollector.TooltipParams(
                    markerColors = colorsByDataPoint(p)
                )
            )
            goodPointsCount += 1
        }

        addNulls(dataPoints.count() - goodPointsCount)
    }

    private fun polygon(center: DoubleVector, radius: Double, rawSideCount: Int): List<DoubleVector> {
        val n = rawSideCount.coerceAtLeast(MIN_SIDE_COUNT)
        val startAngle = -PI / 2.0

        return (0..n).map { i ->
            val angle = startAngle + 2.0 * PI * i / n
            DoubleVector(
                center.x + radius * cos(angle),
                center.y + radius * sin(angle)
            )
        }
    }

    companion object {
        const val DEF_SIDE_COUNT = 5
        const val HANDLES_GROUPS = false

        private const val MIN_SIDE_COUNT = 3
    }
}
