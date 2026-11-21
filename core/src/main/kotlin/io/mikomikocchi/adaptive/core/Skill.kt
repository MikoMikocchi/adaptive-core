package io.mikomikocchi.adaptive.core

/**
 * Skill assessed separately in the learner profile.
 *
 * Declaration order defines the priority when both the mastery score and the
 * practice age are equal: it follows the order of skills in the curriculum.
 * Adding a new skill does not change the algorithm - the skill receives its own
 * score and takes part in selecting the minimum profile component.
 */
enum class Skill(val id: String, val title: String) {
    VOCABULARY("vocabulary", "Vocabulary"),
    GRAMMAR("grammar", "Grammar"),
    READING("reading", "Reading"),
    LISTENING("listening", "Listening");

    companion object {
        private val byId: Map<String, Skill> = entries.associateBy { it.id }

        fun of(raw: String): Skill = byId[raw.trim().lowercase()]
            ?: throw IllegalArgumentException("Unknown skill: $raw")
    }
}
