package io.mikomikocchi.adaptive.core

import java.util.Locale
import kotlin.math.round

/**
 * Recommendation engine.
 *
 * Selection rule: the priority skill is the one with the lowest mastery score;
 * on a tie the skill that has not been practiced for the longest time wins, and
 * on a full tie the first skill in the curriculum order. The difficulty level is
 * derived from the configuration thresholds. The step costs O(K) in the number
 * of skills and does not use the interaction history.
 */
class RecommendationEngine(
    private val catalog: TaskCatalog,
    private val config: EngineConfig = EngineConfig()
) {

    init {
        config.validate()
    }

    fun recommend(profile: LearnerProfile): Recommendation {
        profile.validate()

        val (skill, score) = choosePriority(profile)
        val targetLevel = difficultyFor(score)

        val exact = catalog.tasksFor(skill, targetLevel)
        val fallback = if (exact.isEmpty()) catalog.nearestAvailable(skill, targetLevel) else null
        val task = exact.firstOrNull() ?: fallback
        val level = task?.difficulty ?: targetLevel

        val reason = buildString {
            append(describe(skill, score, level))
            when {
                task == null -> append("; the catalog has no tasks for this skill")
                fallback != null ->
                    append("; no tasks at \"${targetLevel.title.lowercase()}\" level, " +
                        "the nearest available level was assigned")
            }
        }

        return Recommendation(
            learnerId = profile.learnerId,
            prioritySkill = skill.id,
            prioritySkillTitle = skill.title,
            mastery = round4(score),
            difficulty = level,
            taskId = task?.id,
            partial = task == null || task.difficulty != targetLevel,
            reason = reason,
            configVersion = config.configVersion,
            schemaVersion = profile.schemaVersion
        )
    }

    /**
     * Updates the profile with diagnosis results:
     * M(t) = alpha * P(t) + (1 - alpha) * M(t-1).
     * For a skill that is not in the profile yet the score equals P(t).
     */
    fun updateProfile(
        profile: LearnerProfile,
        diagnosis: Map<String, Double>,
        diagnosedAt: String? = null
    ): LearnerProfile {
        profile.validate()
        require(diagnosis.isNotEmpty()) { "Diagnosis results are empty" }

        val updated = profile.mastery.toMutableMap()
        diagnosis.forEach { (rawSkill, observed) ->
            val skill = Skill.of(rawSkill)
            require(observed in 0.0..1.0) {
                "Share of correct answers for skill $rawSkill is out of range [0; 1]: $observed"
            }
            val previous = updated[skill.id]
            updated[skill.id] = if (previous == null) {
                observed
            } else {
                config.alpha * observed + (1.0 - config.alpha) * previous
            }
        }

        return profile.copy(
            mastery = updated,
            lastDiagnosedAt = diagnosedAt ?: profile.lastDiagnosedAt
        )
    }

    /** Records a completed session: the age grows for every skill and resets for the practiced one. */
    fun registerPractice(profile: LearnerProfile, practiced: Skill): LearnerProfile {
        profile.validate()
        val ages = profile.practiceAge.toMutableMap()
        Skill.entries.forEach { skill -> ages[skill.id] = (ages[skill.id] ?: 0) + 1 }
        ages[practiced.id] = 0
        return profile.copy(practiceAge = ages)
    }

    fun diagnosisDue(sessionsSinceLastDiagnosis: Int): Boolean {
        require(sessionsSinceLastDiagnosis >= 0) {
            "Number of sessions since the last diagnosis cannot be negative"
        }
        return sessionsSinceLastDiagnosis >= config.diagnosisIntervalSessions
    }

    fun difficultyFor(score: Double): Difficulty = when {
        score < config.basicThreshold -> Difficulty.BASIC
        score < config.developmentThreshold -> Difficulty.CONSOLIDATION
        else -> Difficulty.DEVELOPMENT
    }

    private fun choosePriority(profile: LearnerProfile): Pair<Skill, Double> {
        val entries = profile.mastery.map { (rawSkill, value) -> Skill.of(rawSkill) to value }
        return entries.minWithOrNull(
            compareBy<Pair<Skill, Double>> { it.second }
                .thenByDescending { profile.practiceAge[it.first.id] ?: 0 }
                .thenBy { it.first.ordinal }
        ) ?: throw IllegalArgumentException("The profile contains no skills")
    }

    private fun describe(skill: Skill, score: Double, difficulty: Difficulty): String {
        val value = String.format(Locale.ROOT, "%.2f", round4(score))
        return "${skill.title.lowercase()} — $value, ${difficulty.title.lowercase()} level"
    }

    private fun round4(value: Double): Double = round(value * 10_000.0) / 10_000.0
}
