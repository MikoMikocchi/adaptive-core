package io.mikomikocchi.adaptive.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileUpdateTest {

    private val engine = RecommendationEngine(
        TaskCatalog.of(
            TaskItem("listening-basic", Skill.LISTENING.id, Difficulty.BASIC),
            TaskItem("listening-consolidation", Skill.LISTENING.id, Difficulty.CONSOLIDATION)
        ),
        EngineConfig()
    )

    private fun profile() = LearnerProfile(
        learnerId = "user_001",
        mastery = mapOf(
            Skill.VOCABULARY.id to 0.90,
            Skill.GRAMMAR.id to 0.85,
            Skill.READING.id to 0.80,
            Skill.LISTENING.id to 0.40
        )
    )

    @Test
    fun `a repeated diagnosis applies exponential smoothing`() {
        val updated = engine.updateProfile(profile(), mapOf(Skill.LISTENING.id to 0.70))
        assertEquals(0.58, updated.masteryOf(Skill.LISTENING)!!, 1e-9)
    }

    @Test
    fun `the other profile components are unchanged`() {
        val updated = engine.updateProfile(profile(), mapOf(Skill.LISTENING.id to 0.70))
        assertEquals(0.90, updated.masteryOf(Skill.VOCABULARY)!!, 1e-12)
        assertEquals(0.85, updated.masteryOf(Skill.GRAMMAR)!!, 1e-12)
        assertEquals(0.80, updated.masteryOf(Skill.READING)!!, 1e-12)
    }

    @Test
    fun `the first diagnosis sets the score directly`() {
        val withoutListening = LearnerProfile(
            learnerId = "user_002",
            mastery = mapOf(Skill.VOCABULARY.id to 0.90)
        )
        val updated = engine.updateProfile(withoutListening, mapOf(Skill.LISTENING.id to 0.40))
        assertEquals(0.40, updated.masteryOf(Skill.LISTENING)!!, 1e-12)
    }

    @Test
    fun `the recommended difficulty changes after the update`() {
        val before = engine.recommend(profile())
        assertEquals(Difficulty.BASIC, before.difficulty)

        val updated = engine.updateProfile(profile(), mapOf(Skill.LISTENING.id to 0.70))
        val after = engine.recommend(updated)
        assertEquals(0.58, after.mastery, 1e-9)
        assertEquals(Difficulty.CONSOLIDATION, after.difficulty)
    }

    @Test
    fun `the age grows for every skill and resets for the practiced one`() {
        val withAges = profile().copy(
            practiceAge = mapOf(Skill.VOCABULARY.id to 2, Skill.GRAMMAR.id to 1)
        )
        val updated = engine.registerPractice(withAges, Skill.GRAMMAR)
        assertEquals(3, updated.practiceAge[Skill.VOCABULARY.id])
        assertEquals(0, updated.practiceAge[Skill.GRAMMAR.id])
        assertEquals(1, updated.practiceAge[Skill.READING.id])
        assertEquals(1, updated.practiceAge[Skill.LISTENING.id])
    }

    @Test
    fun `diagnosis is scheduled by the configured interval`() {
        assertFalse(engine.diagnosisDue(4))
        assertTrue(engine.diagnosisDue(5))
    }

    @Test
    fun `an invalid diagnosis result is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            engine.updateProfile(profile(), mapOf(Skill.LISTENING.id to 1.5))
        }
        assertFailsWith<IllegalArgumentException> { engine.updateProfile(profile(), emptyMap()) }
    }

    @Test
    fun `the profile survives a serialization round trip`() {
        val original = profile()
        val restored = JsonCodec.decodeProfile(JsonCodec.encodeProfile(original))
        assertEquals(original, restored)
    }

    @Test
    fun `the recommendation is serialized to JSON with the reason`() {
        val recommendation = engine.recommend(profile())
        val json = JsonCodec.encodeRecommendation(recommendation)
        assertTrue(json.contains("\"prioritySkill\": \"listening\""), json)
        assertTrue(json.contains("\"difficulty\": \"basic\""), json)
        assertTrue(json.contains("reason"), json)
    }
}
