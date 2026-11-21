package io.mikomikocchi.adaptive.core

import kotlinx.serialization.Serializable
import kotlin.math.abs

/** Learning task with metadata: skill, difficulty, topic and type. */
@Serializable
data class TaskItem(
    val id: String,
    val skill: String,
    val difficulty: Difficulty,
    val topic: String? = null,
    val type: String? = null
)

/**
 * Task catalog. Task order is preserved: when conditions are equal the engine
 * returns the first task of the matching skill and level, so the recommendation
 * is deterministic and suitable for regression testing.
 */
class TaskCatalog(tasks: List<TaskItem>) {

    private val index: Map<Pair<Skill, Difficulty>, List<TaskItem>>

    val size: Int = tasks.size

    init {
        require(tasks.isNotEmpty()) { "Task catalog is empty" }
        val ids = HashSet<String>()
        tasks.forEach { task ->
            require(task.id.isNotBlank()) { "Task id must not be blank" }
            require(ids.add(task.id)) { "Duplicate task id: ${task.id}" }
            Skill.of(task.skill)
        }
        index = tasks.groupBy { Skill.of(it.skill) to it.difficulty }
    }

    fun tasksFor(skill: Skill, difficulty: Difficulty): List<TaskItem> =
        index[skill to difficulty].orEmpty()

    /**
     * First task of the nearest available difficulty level.
     * On a tie the easier level is preferred.
     */
    fun nearestAvailable(skill: Skill, difficulty: Difficulty): TaskItem? =
        Difficulty.entries
            .sortedWith(compareBy<Difficulty>({ abs(it.ordinal - difficulty.ordinal) }, { it.ordinal }))
            .firstNotNullOfOrNull { tasksFor(skill, it).firstOrNull() }

    companion object {
        fun of(vararg tasks: TaskItem): TaskCatalog = TaskCatalog(tasks.toList())
    }
}
