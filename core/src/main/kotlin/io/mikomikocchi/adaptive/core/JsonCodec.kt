package io.mikomikocchi.adaptive.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Single serialization setup for the core contract. */
object JsonCodec {

    val json: Json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun encodeProfile(profile: LearnerProfile): String = json.encodeToString(profile)

    fun decodeProfile(text: String): LearnerProfile = json.decodeFromString(text)

    fun encodeRecommendation(recommendation: Recommendation): String =
        json.encodeToString(recommendation)
}
