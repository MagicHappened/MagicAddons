package org.magic.magicaddons.features.farming.fortune

data class FortuneSource(val name: String, val value: Double, val detail: String? = null)

data class FortuneSnapshot(val sources: List<FortuneSource>, val unknown: List<String>) {
    val total: Double get() = sources.sumOf { it.value }
}
