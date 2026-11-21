package io.mikomikocchi.adaptive.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecommendationEngineTest {

    private fun profile(
        vocabulary: Double = 0.70,
        grammar: Double = 0.70,
        reading: Double = 0.70,
        listening: Double = 0.70,
        ages: Map<String, Int> = emptyMap()
    ) = LearnerProfile(
        learnerId = "user_001",
        mastery = mapOf(
            Skill.VOCABULARY.id to vocabulary,
            Skill.GRAMMAR.id to grammar,
            Skill.READING.id to reading,
            Skill.LISTENING.id to listening
        ),
        practiceAge = ages
    )

    private fun catalog(excluded: Set<Pair<Skill, Difficulty>> = emptySet()): TaskCatalog {
        val tasks = mutableListOf<TaskItem>()
        for (skill in Skill.entries) {
            for (difficulty in Difficulty.entries) {
                if ((skill to difficulty) in excluded) continue
                tasks += TaskItem(
                    id = "${skill.id}-${difficulty.id}",
                    skill = skill.id,
                    difficulty = difficulty
                )
            }
        }
        return TaskCatalog(tasks)
    }

    private fun engine(catalog: TaskCatalog = catalog()) =
        RecommendationEngine(catalog, EngineConfig())

    @Test
    fun `selects the skill with the lowest score`() {
        val result = engine().recommend(profile(0.80, 0.75, 0.70, 0.40))
        assertEquals(Skill.LISTENING.id, result.prioritySkill)
        assertEquals(Difficulty.BASIC, result.difficulty)
        assertEquals("${Skill.LISTENING.id}-${Difficulty.BASIC.id}", result.taskId)
        assertEquals(false, result.partial)
    }

    @Test
    fun `on equal scores selects the skill with the longest rest`() {
        val ages = mapOf(Skill.LISTENING.id to 9, Skill.GRAMMAR.id to 3)
        val result = engine().recommend(
            profile(0.50, 0.50, 0.50, 0.50, ages)
        )
        assertEquals(Skill.LISTENING.id, result.prioritySkill)
    }

    @Test
    fun `on a full tie uses the curriculum order`() {
        val result = engine().recommend(profile(0.60, 0.60, 0.60, 0.60))
        assertEquals(Skill.VOCABULARY.id, result.prioritySkill)
    }

    @Test
    fun `a score just below the basic threshold yields the basic level`() {
        val result = engine().recommend(profile(0.4999, 0.90, 0.90, 0.90))
        assertEquals(Difficulty.BASIC, result.difficulty)
    }

    @Test
    fun `the basic threshold itself yields the consolidation level`() {
        val result = engine().recommend(profile(0.50, 0.90, 0.90, 0.90))
        assertEquals(Difficulty.CONSOLIDATION, result.difficulty)
    }

    @Test
    fun `the development threshold itself yields the development level`() {
        val result = engine().recommend(profile(0.75, 0.90, 0.90, 0.90))
        assertEquals(Difficulty.DEVELOPMENT, result.difficulty)
    }

    @Test
    fun `a score just below the development threshold yields the consolidation level`() {
        val result = engine().recommend(profile(0.7499, 0.90, 0.90, 0.90))
        assertEquals(Difficulty.CONSOLIDATION, result.difficulty)
    }

    @Test
    fun `the same profile yields the same recommendation`() {
        val engine = engine()
        val profile = profile(0.62, 0.62, 0.62, 0.41)
        assertEquals(engine.recommend(profile), engine.recommend(profile))
    }

    @Test
    fun `the reason contains the score and the level`() {
        val result = engine().recommend(profile(0.80, 0.75, 0.70, 0.40))
        assertTrue(result.reason.contains("0.40"), result.reason)
        assertTrue(result.reason.contains("basic"), result.reason)
        assertTrue(result.reason.contains("listening"), result.reason)
    }

    @Test
    fun `when no task of the required level exists the nearest one is used and the answer is partial`() {
        val result = engine(catalog(setOf(Skill.LISTENING to Difficulty.BASIC)))
            .recommend(profile(0.80, 0.75, 0.70, 0.30))
        assertEquals(Difficulty.CONSOLIDATION, result.difficulty)
        assertEquals(true, result.partial)
        assertEquals("${Skill.LISTENING.id}-${Difficulty.CONSOLIDATION.id}", result.taskId)
    }

    @Test
    fun `when the skill has no tasks at all the recommendation comes without a task`() {
        val excluded = Difficulty.entries.map { Skill.GRAMMAR to it }.toSet()
        val result = engine(catalog(excluded)).recommend(profile(0.80, 0.20, 0.70, 0.70))
        assertNull(result.taskId)
        assertEquals(true, result.partial)
        assertTrue(result.reason.contains("no"), result.reason)
    }

    @Test
    fun `a profile with a subset of skills is processed`() {
        val partialProfile = LearnerProfile(
            learnerId = "user_002",
            mastery = mapOf(Skill.VOCABULARY.id to 0.90, Skill.GRAMMAR.id to 0.35)
        )
        val result = engine().recommend(partialProfile)
        assertEquals(Skill.GRAMMAR.id, result.prioritySkill)
        assertEquals(Difficulty.BASIC, result.difficulty)
    }

    @Test
    fun `a score out of range is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            engine().recommend(profile(1.20, 0.70, 0.70, 0.70))
        }
    }

    @Test
    fun `an unknown skill is rejected`() {
        val bad = LearnerProfile(learnerId = "user_003", mastery = mapOf("kanji" to 0.50))
        assertFailsWith<IllegalArgumentException> { engine().recommend(bad) }
    }

    @Test
    fun `an invalid configuration is rejected`() {
        assertFailsWith<IllegalArgumentException> { EngineConfig(alpha = 1.5).validate() }
        assertFailsWith<IllegalArgumentException> {
            EngineConfig(basicThreshold = 0.80, developmentThreshold = 0.60).validate()
        }
    }
}
