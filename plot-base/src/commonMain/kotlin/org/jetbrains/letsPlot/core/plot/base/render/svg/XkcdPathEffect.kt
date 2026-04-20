/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.core.plot.base.render.svg

import org.jetbrains.letsPlot.commons.geometry.DoubleVector
import kotlin.math.sqrt
import kotlin.random.Random

object XkcdPathEffect {
    fun toHandDrawn(points: List<DoubleVector>): List<DoubleVector> {
        if (points.size < 2) {
            return points
        }

        val result = ArrayList<DoubleVector>(points.size * 4)
        result.add(points[0])

        for (i in 1 until points.size) {
            val start = points[i - 1]
            val end = points[i]
            val dx = end.x - start.x
            val dy = end.y - start.y
            val length = sqrt(dx * dx + dy * dy)

            if (length == 0.0) {
                result.add(end)
                continue
            }

            val normalX = -dy / length
            val normalY = dx / length
            val amplitude = (length * JITTER_RELATIVE).coerceIn(MIN_JITTER_PX, MAX_JITTER_PX)

            for (pointIndex in 1..SUBDIVISIONS) {
                val t = pointIndex.toDouble() / (SUBDIVISIONS + 1.0)
                val middle = DoubleVector(start.x + dx * t, start.y + dy * t)
                val offset = randomOffset(seedFor(start, end, i, pointIndex), amplitude)
                result.add(DoubleVector(middle.x + normalX * offset, middle.y + normalY * offset))
            }

            result.add(end)
        }

        return result
    }

    private fun randomOffset(seed: Long, amplitude: Double): Double {
        return Random(seed).nextDouble(-amplitude, amplitude)
    }

    private fun seedFor(start: DoubleVector, end: DoubleVector, segmentIndex: Int, pointIndex: Int): Long {
        var seed = 1469598103934665603L
        seed = mix(seed, start.x.toBits())
        seed = mix(seed, start.y.toBits())
        seed = mix(seed, end.x.toBits())
        seed = mix(seed, end.y.toBits())
        seed = mix(seed, segmentIndex.toLong())
        seed = mix(seed, pointIndex.toLong())
        return seed
    }

    private fun mix(seed: Long, value: Long): Long {
        return (seed xor value) * 1099511628211L
    }

    private const val SUBDIVISIONS = 3
    private const val JITTER_RELATIVE = 0.2
    private const val MIN_JITTER_PX = 1.1
    private const val MAX_JITTER_PX = 3.4
}
