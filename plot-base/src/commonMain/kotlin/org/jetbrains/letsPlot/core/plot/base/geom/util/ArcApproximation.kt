/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.core.plot.base.geom.util

import org.jetbrains.letsPlot.commons.geometry.DoubleVector
import org.jetbrains.letsPlot.commons.intern.typedGeometry.algorithms.AdaptiveResampler
import org.jetbrains.letsPlot.commons.intern.typedGeometry.algorithms.AdaptiveResampler.Companion.resample

fun approximateArc(
    startPoint: DoubleVector,
    endPoint: DoubleVector,
    startAngle: Double,
    endAngle: Double,
    arcPoint: (Double) -> DoubleVector,
    precision: Double = AdaptiveResampler.PIXEL_PRECISION
): List<DoubleVector> {
    val segmentLength = startPoint.subtract(endPoint).length()
    if (segmentLength == 0.0 || !segmentLength.isFinite()) {
        return listOf(startPoint, endPoint)
    }

    return resample(startPoint, endPoint, precision) { p ->
        val ratio = p.subtract(startPoint).length() / segmentLength
        if (ratio.isFinite()) {
            arcPoint(startAngle + (endAngle - startAngle) * ratio)
        } else {
            p
        }
    }
}
