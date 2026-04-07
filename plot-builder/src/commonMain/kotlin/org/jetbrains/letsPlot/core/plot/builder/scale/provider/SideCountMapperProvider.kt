/*
 * Copyright (c) 2026. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.core.plot.builder.scale.provider

import org.jetbrains.letsPlot.core.plot.base.DiscreteTransform
import org.jetbrains.letsPlot.core.plot.base.ScaleMapper
import org.jetbrains.letsPlot.core.plot.builder.scale.DiscreteOnlyMapperProvider
import org.jetbrains.letsPlot.core.plot.builder.scale.mapper.GuideMappers
import org.jetbrains.letsPlot.core.plot.builder.scale.mapper.SideCountMapper

class SideCountMapperProvider : DiscreteOnlyMapperProvider<Double>() {
    override fun createDiscreteMapper(discreteTransform: DiscreteTransform): ScaleMapper<Double> {
        val outputValues = discreteTransform.effectiveDomain.mapIndexed { index, domainValue ->
            SideCountMapper.fromDomainValue(domainValue, index)
        }

        return GuideMappers.discreteToDiscrete(discreteTransform, outputValues, SideCountMapper.NA_VALUE)
    }

    companion object {
        val DEFAULT: DiscreteOnlyMapperProvider<Double> = SideCountMapperProvider()
    }
}
