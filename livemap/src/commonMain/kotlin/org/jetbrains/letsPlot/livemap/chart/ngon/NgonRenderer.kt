/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.livemap.chart.ngon

import org.jetbrains.letsPlot.core.canvas.Context2d
import org.jetbrains.letsPlot.livemap.chart.ChartElementComponent
import org.jetbrains.letsPlot.livemap.chart.PointComponent
import org.jetbrains.letsPlot.livemap.core.ecs.EcsEntity
import org.jetbrains.letsPlot.livemap.mapengine.RenderHelper
import org.jetbrains.letsPlot.livemap.mapengine.Renderer
import org.jetbrains.letsPlot.livemap.mapengine.placement.WorldOriginComponent
import org.jetbrains.letsPlot.livemap.mapengine.translate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class NgonRenderer(
    private val sideCount: Int,
) : Renderer {
    override fun render(entity: EcsEntity, ctx: Context2d, renderHelper: RenderHelper) {
        val chartElement = entity.get<ChartElementComponent>()
        val pointData = entity.get<PointComponent>()

        ctx.translate(renderHelper.dimToScreen(entity.get<WorldOriginComponent>().origin))

        ctx.beginPath()
        drawNgon(
            ctx = ctx,
            radius = pointData.scaledRadius(chartElement.scalingSizeFactor).value,
            sideCount = sideCount,
        )
        if (chartElement.fillColor != null) {
            ctx.setFillStyle(chartElement.scaledFillColor())
            ctx.fill()
        }
        if (chartElement.strokeColor != null && chartElement.scaledStrokeWidth() > 0.0) {
            ctx.setStrokeStyle(chartElement.scaledStrokeColor())
            ctx.setLineWidth(chartElement.scaledStrokeWidth())
            ctx.stroke()
        }
    }

    private fun drawNgon(ctx: Context2d, radius: Double, sideCount: Int) {
        val startAngle = -PI / 2.0

        for (i in 0..sideCount) {
            val angle = startAngle + 2.0 * PI * i / sideCount
            val x = radius * cos(angle)
            val y = radius * sin(angle)
            if (i == 0) {
                ctx.moveTo(x, y)
            } else {
                ctx.lineTo(x, y)
            }
        }

        ctx.closePath()
    }
}
