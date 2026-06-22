/*
 * Copyright (c) 2026. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.core.plot.builder.defaultTheme.values

internal object ThemeStyleOverrides {
    val XKCD: Map<String, Any> = mapOf(
        ThemeOption.STYLE to mapOf(
            ThemeOption.Style.FILL to ThemeOption.Style.CROSS
        ),
        ThemeOption.TEXT to mapOf(
            ThemeOption.Elem.FONT_FAMILY to "Humor Sans"
        ),
        ThemeOption.AXIS_TICKS_LENGTH to 15.0,
        ThemeOption.AXIS_TEXT to mapOf(
            ThemeOption.Elem.SIZE to 20.0
        ),
        ThemeOption.PLOT_TITLE to mapOf(
            ThemeOption.Elem.SIZE to 24.0
        )
    )
}
