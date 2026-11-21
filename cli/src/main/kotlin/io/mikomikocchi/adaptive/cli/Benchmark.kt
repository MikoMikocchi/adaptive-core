package io.mikomikocchi.adaptive.cli

import io.mikomikocchi.adaptive.core.Difficulty
import io.mikomikocchi.adaptive.core.EngineConfig
import io.mikomikocchi.adaptive.core.JsonCodec
import io.mikomikocchi.adaptive.core.LearnerProfile
import io.mikomikocchi.adaptive.core.RecommendationEngine
import io.mikomikocchi.adaptive.core.Skill
import io.mikomikocchi.adaptive.core.TaskCatalog
import io.mikomikocchi.adaptive.core.TaskItem
import java.util.Locale
import kotlin.random.Random

private const val WARMUP_CALLS = 50_000
private const val MEASURED_CALLS = 200_000
private const val PROFILES = 20_000
private const val JSON_CALLS = 20_000
private const val TASKS_PER_LEVEL = 25

fun main() {
    val catalog = buildCatalog()
    val engine = RecommendationEngine(catalog, EngineConfig())

    println("recommender-core 1.0.0")
    println(
        "jvm=${System.getProperty("java.version")} " +
            "cores=${Runtime.getRuntime().availableProcessors()} " +
            "os=${System.getProperty("os.name")}"
    )
    println("catalog=${catalog.size} tasks, skills=${Skill.entries.size}, levels=${Difficulty.entries.size}")

    demoContract(engine)

    val profiles = buildProfiles(PROFILES)
    benchmarkRecommend(engine, profiles)
    benchmarkJson(engine, profiles)
}

private fun demoContract(engine: RecommendationEngine) {
    val profile = LearnerProfile(
        learnerId = "user_001",
        mastery = mapOf(
            Skill.VOCABULARY.id to 0.90,
            Skill.GRAMMAR.id to 0.85,
            Skill.READING.id to 0.80,
            Skill.LISTENING.id to 0.40
        )
    )

    println()
    println("=== Contract: profile ===")
    print(JsonCodec.encodeProfile(profile))
    println()

    println("=== Contract: recommendation ===")
    print(JsonCodec.encodeRecommendation(engine.recommend(profile)))
    println()

    val updated = engine.updateProfile(profile, mapOf(Skill.LISTENING.id to 0.70))
    println("=== Profile update ===")
    println(
        "listening: 0.60 * 0.70 + 0.40 * 0.40 = " +
            String.format(Locale.ROOT, "%.4f", updated.masteryOf(Skill.LISTENING))
    )
    println()
}

private fun benchmarkRecommend(engine: RecommendationEngine, profiles: List<LearnerProfile>) {
    var sink = 0
    for (i in 0 until WARMUP_CALLS) {
        sink = sink xor engine.recommend(profiles[i % profiles.size]).prioritySkill.hashCode()
    }

    val times = LongArray(MEASURED_CALLS)
    val started = System.nanoTime()
    for (i in 0 until MEASURED_CALLS) {
        val begin = System.nanoTime()
        val result = engine.recommend(profiles[i % profiles.size])
        times[i] = System.nanoTime() - begin
        sink = sink xor (result.taskId?.hashCode() ?: 0)
    }
    val elapsed = System.nanoTime() - started
    times.sort()

    println("=== recommend() ===")
    println("warmup=$WARMUP_CALLS calls, measured=$MEASURED_CALLS calls, profiles=${profiles.size}")
    println(
        "mean=" + us(times.average()) +
            " p50=" + us(percentile(times, 0.50)) +
            " p95=" + us(percentile(times, 0.95)) +
            " p99=" + us(percentile(times, 0.99))
    )
    println(
        "throughput=" + String.format(Locale.ROOT, "%.0f", MEASURED_CALLS * 1e9 / elapsed) +
            " calls/s, checksum=$sink"
    )
    println()
}

private fun benchmarkJson(engine: RecommendationEngine, profiles: List<LearnerProfile>) {
    var sink = 0
    val times = LongArray(JSON_CALLS)
    for (i in 0 until JSON_CALLS) {
        val recommendation = engine.recommend(profiles[i % profiles.size])
        val begin = System.nanoTime()
        val text = JsonCodec.encodeRecommendation(recommendation)
        times[i] = System.nanoTime() - begin
        sink += text.length
    }
    times.sort()

    println("=== JSON serialization of the recommendation ===")
    println(
        "mean=" + us(times.average()) +
            " p50=" + us(percentile(times, 0.50)) +
            " p99=" + us(percentile(times, 0.99)) +
            ", checksum=$sink"
    )
}

private fun us(nanos: Double): String =
    String.format(Locale.ROOT, "%.3f us", nanos / 1000.0)

private fun percentile(sorted: LongArray, q: Double): Double {
    val index = ((sorted.size - 1) * q).toInt()
    return sorted[index].toDouble()
}

private fun buildCatalog(): TaskCatalog {
    val tasks = mutableListOf<TaskItem>()
    for (skill in Skill.entries) {
        for (difficulty in Difficulty.entries) {
            for (n in 1..TASKS_PER_LEVEL) {
                tasks += TaskItem(
                    id = "JP-${skill.id.uppercase().take(4)}-${difficulty.id.take(3).uppercase()}-" +
                        n.toString().padStart(3, '0'),
                    skill = skill.id,
                    difficulty = difficulty,
                    topic = "topic-$n",
                    type = "closed"
                )
            }
        }
    }
    return TaskCatalog(tasks)
}

private fun buildProfiles(count: Int): List<LearnerProfile> {
    val random = Random(20261121)
    return List(count) { index ->
        LearnerProfile(
            learnerId = "user_${index.toString().padStart(5, '0')}",
            mastery = mapOf(
                Skill.VOCABULARY.id to round2(random.nextDouble(0.20, 0.90)),
                Skill.GRAMMAR.id to round2(random.nextDouble(0.20, 0.90)),
                Skill.READING.id to round2(random.nextDouble(0.20, 0.90)),
                Skill.LISTENING.id to round2(random.nextDouble(0.20, 0.90))
            ),
            practiceAge = Skill.entries.associate { it.id to random.nextInt(0, 10) }
        )
    }
}

private fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0
