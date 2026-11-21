# adaptive-japanese-core

Recommender core for an adaptive platform that teaches Japanese. Given a
fixed-structure learner profile, the core selects the next learning task and
explains the choice; the interface, the content and the storage stay outside.

- four skills — vocabulary, grammar, reading and listening — each with a
  mastery score in `[0; 1]`;
- selection rule: the skill with the lowest mastery wins; ties are broken by
  the longest rest, then by the curriculum order, so the result is deterministic;
- difficulty from two thresholds: below `0.50` — basic, `0.50`–`0.75` —
  consolidation, above `0.75` — development;
- `O(K)` per step over the number of skills, no interaction history, no server
  round trip;
- one external dependency: `kotlinx.serialization`.

## Requirements

JDK 21. The Gradle wrapper is included, so no local Gradle installation is needed.

## Build and test

```bash
./gradlew :core:test
```

The suite covers the selection rule (lowest mastery, tie-breaking, threshold
boundaries, nearest-level fallback, missing task), profile updates, the
immutability of untouched components, serialization round trips and input
validation — 24 tests in total.

## Benchmark

```bash
./gradlew :cli:run
```

The CLI prints the JSON contract samples, a profile update and timing:

- mean ≈ `1.2 µs`, p50 ≈ `1.0 µs`, p99 ≈ `3 µs`, ≈ 800k calls/s;
- measured on JDK 21, 16 cores, one thread, with a 300-task catalog and
  20 000 profiles after 50 000 warm-up calls; 200 000 calls measured;
- JSON serialization of a recommendation takes a few microseconds.

## Contract

`Recommendation` is the only value the caller has to handle:

```json
{
    "learnerId": "user_001",
    "prioritySkill": "listening",
    "prioritySkillTitle": "Listening",
    "mastery": 0.4,
    "difficulty": "basic",
    "taskId": "JP-LIST-BAS-001",
    "partial": false,
    "reason": "listening — 0.40, basic level",
    "configVersion": "1.0.0",
    "schemaVersion": 1
}
```

`partial` is `true` when the catalog has no task of the required level and the
nearest available one was assigned, or when there is no task at all (then
`taskId` is `null`). Thresholds, `alpha` and the diagnosis interval live in
`EngineConfig` and are versioned together with the profile schema, so changing
them does not require recomputing stored profiles.

## Layout

```
core/src/main/kotlin/io/mikomikocchi/adaptive/core/
    Skill.kt                 skills and their curriculum order
    Difficulty.kt            difficulty scale
    LearnerProfile.kt        profile record and invariants
    TaskCatalog.kt           task metadata and the skill × level index
    EngineConfig.kt          thresholds, alpha, diagnosis interval
    Recommendation.kt        response contract
    RecommendationEngine.kt  the selection rule
    JsonCodec.kt             serialization setup
cli/src/main/kotlin/io/mikomikocchi/adaptive/cli/
    Benchmark.kt             contract demo and timing
```

## License

MIT — see [LICENSE](LICENSE).
