/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.core.plot.base.render.svg

import org.jetbrains.letsPlot.commons.geometry.DoubleVector

interface GeomStyle {
    fun resamplePath(points: List<DoubleVector>): List<DoubleVector>

    object Regular : GeomStyle {
        override fun resamplePath(points: List<DoubleVector>): List<DoubleVector> = points
    }

    object Xkcd : GeomStyle {
        override fun resamplePath(points: List<DoubleVector>): List<DoubleVector> =
            XkcdPathEffect.toHandDrawn(points)
    }
}
