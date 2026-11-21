package io.mikomikocchi.adaptive.core

import kotlinx.serialization.Serializable

/**
 * Core parameters. They live in the configuration and are versioned together
 * with the profile schema: changing the thresholds does not require recomputing
 * the stored records.
 */
@Serializable
data class EngineConfig(
    val configVersion: String = "1.0.0",
    val alpha: Double = 0.60,
    val basicThreshold: Double = 0.50,
    val developmentThreshold: Double = 0.75,
    val diagnosisIntervalSessions: Int = 5
) {

    fun validate() {
        require(configVersion.isNotBlank()) { "Config version must not be blank" }
        require(alpha in 0.0..1.0) { "Alpha must be in range [0; 1]: $alpha" }
        require(basicThreshold > 0.0 && basicThreshold < developmentThreshold) {
            "Thresholds must satisfy 0 < basic < development: " +
                "$basicThreshold and $developmentThreshold"
        }
        require(developmentThreshold <= 1.0) {
            "Upper threshold cannot exceed 1: $developmentThreshold"
        }
        require(diagnosisIntervalSessions > 0) {
            "Diagnosis interval must be positive: $diagnosisIntervalSessions"
        }
    }
}
