/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.livemap.api

import org.jetbrains.letsPlot.commons.intern.spatial.LonLat
import org.jetbrains.letsPlot.commons.intern.typedGeometry.Scalar
import org.jetbrains.letsPlot.commons.intern.typedGeometry.Vec
import org.jetbrains.letsPlot.commons.values.Color
import org.jetbrains.letsPlot.livemap.chart.ChartElementComponent
import org.jetbrains.letsPlot.livemap.chart.IndexComponent
import org.jetbrains.letsPlot.livemap.chart.LocatorComponent
import org.jetbrains.letsPlot.livemap.chart.PointComponent
import org.jetbrains.letsPlot.livemap.chart.ngon.NgonRenderer
import org.jetbrains.letsPlot.livemap.chart.point.PointLocator
import org.jetbrains.letsPlot.livemap.core.ecs.EcsEntity
import org.jetbrains.letsPlot.livemap.core.ecs.addComponents
import org.jetbrains.letsPlot.livemap.core.layers.LayerKind
import org.jetbrains.letsPlot.livemap.mapengine.LayerEntitiesComponent
import org.jetbrains.letsPlot.livemap.mapengine.MapProjection
import org.jetbrains.letsPlot.livemap.mapengine.RenderableComponent
import org.jetbrains.letsPlot.livemap.mapengine.placement.ScreenDimensionComponent
import org.jetbrains.letsPlot.livemap.mapengine.placement.WorldOriginComponent

@LiveMapDsl
class NgonLayerBuilder(
    val factory: FeatureEntityFactory,
    val mapProjection: MapProjection,
)

fun FeatureLayerBuilder.ngons(block: NgonLayerBuilder.() -> Unit) {
    val layerEntity = myComponentManager
        .createEntity("map_layer_ngon")
        .addComponents {
            +layerManager.addLayer("geom_ngon", LayerKind.FEATURES)
            +LayerEntitiesComponent()
        }

    NgonLayerBuilder(
        FeatureEntityFactory(layerEntity, panningPointsMaxCount = 200),
        mapProjection,
    ).apply(block)
}

fun NgonLayerBuilder.ngon(block: NgonEntityBuilder.() -> Unit) {
    NgonEntityBuilder(factory)
        .apply(block)
        .build()
}

@LiveMapDsl
class NgonEntityBuilder(
    private val myFactory: FeatureEntityFactory,
) {
    var sizeScalingRange: ClosedRange<Int>? = null
    var alphaScalingEnabled: Boolean = false
    var layerIndex: Int? = null
    var radius: Double = 4.0
    var point: Vec<LonLat> = LonLat.ZERO_VEC

    var strokeColor: Color = Color.BLACK
    var strokeWidth: Double = 1.0

    var index: Int? = null
    var fillColor: Color = Color.WHITE
    var label: String = ""
    var sideCount: Int = 5

    fun build(nonInteractive: Boolean = false): EcsEntity {
        return myFactory.createStaticFeatureWithLocation("map_ent_s_ngon", point)
            .run {
                myFactory.incrementLayerPointsTotalCount(1)
                setInitializer { worldPoint ->
                    if (layerIndex != null && index != null) {
                        +IndexComponent(layerIndex!!, index!!)
                    }
                    +RenderableComponent().apply {
                        renderer = NgonRenderer(sideCount)
                    }
                    +ChartElementComponent().apply {
                        sizeScalingRange = this@NgonEntityBuilder.sizeScalingRange
                        alphaScalingEnabled = this@NgonEntityBuilder.alphaScalingEnabled
                        fillColor = this@NgonEntityBuilder.fillColor
                        strokeColor = this@NgonEntityBuilder.strokeColor
                        strokeWidth = this@NgonEntityBuilder.strokeWidth
                    }
                    +PointComponent().apply {
                        size = Scalar(radius * 2.0)
                    }

                    +WorldOriginComponent(worldPoint)
                    +ScreenDimensionComponent()

                    if (!nonInteractive) {
                        +LocatorComponent(PointLocator)
                    }
                }
            }
    }
}
