package io.mikomikocchi.adaptive.core

import kotlinx.serialization.Serializable

/**
 * Learner profile record of a fixed structure.
 *
 * The profile is read and updated as a whole, without touching the answer
 * history: a recommendation needs the skill scores and the practice age marks,
 * not the sequence of interactions.
 *
 * @param mastery skill mastery scores by skill identifier, values in range [0; 1]
 * @param practiceAge number of learning sessions since the skill was last practiced
 */
@Serializable
data class LearnerProfile(
    val learnerId: String,
    val mastery: Map<String, Double>,
    val schemaVersion: Int = SCHEMA_VERSION,
    val lastDiagnosedAt: String? = null,
    val practiceAge: Map<String, Int> = emptyMap()
) {

    fun masteryOf(skill: Skill): Double? = mastery[skill.id]

    /** Validates the record invariants; called by the engine before every computation. */
    fun validate() {
        require(learnerId.isNotBlank()) { "Learner id must not be blank" }
        require(schemaVersion >= 1) { "Schema version must be positive: $schemaVersion" }
        require(mastery.isNotEmpty()) { "The profile contains no skills" }
        mastery.forEach { (rawSkill, value) ->
            Skill.of(rawSkill)
            require(value in 0.0..1.0) {
                "Score for skill $rawSkill is out of range [0; 1]: $value"
            }
        }
        practiceAge.forEach { (rawSkill, age) ->
            Skill.of(rawSkill)
            require(age >= 0) { "Practice age cannot be negative: $age" }
        }
    }

    companion object {
        const val SCHEMA_VERSION: Int = 1
    }
}
