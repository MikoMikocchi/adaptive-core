package io.mikomikocchi.adaptive.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Task difficulty level. Declaration order defines the difficulty scale, so the
 * "nearest available level" is measured as the distance on this scale.
 * In JSON the level is written with the identifier from the core contract.
 */
@Serializable
enum class Difficulty(val id: String, val title: String) {
    @SerialName("basic")
    BASIC("basic", "Basic"),

    @SerialName("consolidation")
    CONSOLIDATION("consolidation", "Consolidation"),

    @SerialName("development")
    DEVELOPMENT("development", "Development")
}
