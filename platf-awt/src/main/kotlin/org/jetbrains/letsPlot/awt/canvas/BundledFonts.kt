/*
 * Copyright (c) 2026. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.awt.canvas

import org.jetbrains.letsPlot.commons.logging.PortableLogging
import java.awt.Font
import java.awt.GraphicsEnvironment

internal object BundledFonts {
    private val LOG = PortableLogging.logger(BundledFonts::class)

    private const val HUMOR_SANS_RESOURCE = "fonts/HumorSans-Regular.ttf"

    @Volatile
    private var isRegistered = false

    @Synchronized
    fun ensureRegistered() {
        if (isRegistered) {
            return
        }

        isRegistered = true
        registerFont(HUMOR_SANS_RESOURCE)
    }

    private fun registerFont(resourceName: String) {
        val resourceStream = BundledFonts::class.java.classLoader.getResourceAsStream(resourceName)
        if (resourceStream == null) {
            LOG.info { "Bundled font resource not found: $resourceName" }
            return
        }

        try {
            resourceStream.use { stream ->
                val font = Font.createFont(Font.TRUETYPE_FONT, stream)
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font)
            }
        } catch (e: Exception) {
            LOG.error(e) { "Failed to register bundled font resource: $resourceName" }
        }
    }
}
