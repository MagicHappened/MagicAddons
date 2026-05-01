package org.magic.magicaddons.data.greenhouse

import org.magic.magicaddons.data.greenhouse.elements.basecrop.Wheat
import java.util.ServiceLoader

object CropRegistry {
    val all: List<CropDefinition> by lazy {
        ServiceLoader.load(CropDefinitionProvider::class.java)
            .map { it.definition }
    }
}