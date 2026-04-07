/*
 * Copyright (c) 2026. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.core.plot.builder.scale.mapper

import kotlin.math.roundToInt

object SideCountMapper {
    val NA_VALUE = 5.0

    private val sideCounts = run {
        val bestFirst = listOf(
            3.0,
            4.0,
            5.0,
            6.0,
            8.0,
            10.0,
            12.0
        )

        val rest = (7..30)
            .map(Int::toDouble)
            .filterNot { it in bestFirst }

        bestFirst + rest
    }

    fun allSideCounts(): List<Double> {
        return sideCounts
    }

    fun fromDomainValue(domainValue: Any?, index: Int): Double {
        val numeric = (domainValue as? Number)?.toDouble()
        if (numeric != null && numeric.isFinite()) {
            return numeric.roundToInt().coerceAtLeast(3).toDouble()
        }

        return sideCounts[index % sideCounts.size]
    }
}
