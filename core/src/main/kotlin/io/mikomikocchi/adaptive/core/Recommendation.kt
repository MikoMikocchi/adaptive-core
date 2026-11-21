package io.mikomikocchi.adaptive.core

import kotlinx.serialization.Serializable

/**
 * Recommendation returned by the engine. Contains the priority skill, the
 * difficulty level, the task id and the explanation derived from the same
 * values as the decision itself.
 *
 * @param partial true when no task of the required level exists and the nearest
 *                available one was assigned, or when no task was found at all
 *                (then [taskId] is null)
 */
@Serializable
data class Recommendation(
    val learnerId: String,
    val prioritySkill: String,
    val prioritySkillTitle: String,
    val mastery: Double,
    val difficulty: Difficulty,
    val taskId: String? = null,
    val partial: Boolean = false,
    val reason: String,
    val configVersion: String,
    val schemaVersion: Int
)
