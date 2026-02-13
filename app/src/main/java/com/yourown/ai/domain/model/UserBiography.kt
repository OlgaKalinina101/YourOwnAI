package com.yourown.ai.domain.model

/**
 * User biography generated from memory clusters
 */
data class UserBiography(
    val values: String = "",            // Что важно (ценности, убеждения, взгляды)
    val profile: String = "",           // Основная информация о пользователе
    val painPoints: String = "",        // Что болит
    val joys: String = "",              // Что радует
    val fears: String = "",             // Что пугает
    val loves: String = "",             // Что любит
    val currentSituation: String = "",  // Что происходит сейчас
    val lastUpdated: Long = System.currentTimeMillis(),
    val processedClusters: Int = 0      // Количество обработанных кластеров
) {
    /**
     * Check if biography is empty
     */
    fun isEmpty(): Boolean {
        return values.isBlank() &&
               profile.isBlank() &&
               painPoints.isBlank() &&
               joys.isBlank() &&
               fears.isBlank() &&
               loves.isBlank() &&
               currentSituation.isBlank()
    }
    
    /**
     * Format biography as text
     */
    fun toFormattedText(): String {
        val sections = mutableListOf<String>()
        
        if (values.isNotBlank()) {
            sections.add("## Что важно\n$values")
        }
        
        if (profile.isNotBlank()) {
            sections.add("## Профиль\n$profile")
        }
        
        if (painPoints.isNotBlank()) {
            sections.add("## Что болит\n$painPoints")
        }
        
        if (joys.isNotBlank()) {
            sections.add("## Что радует\n$joys")
        }
        
        if (fears.isNotBlank()) {
            sections.add("## Что пугает\n$fears")
        }
        
        if (loves.isNotBlank()) {
            sections.add("## Что любит\n$loves")
        }
        
        if (currentSituation.isNotBlank()) {
            sections.add("## Что происходит сейчас\n$currentSituation")
        }
        
        return sections.joinToString("\n\n")
    }
}

/**
 * Status of biography generation
 */
sealed class BiographyGenerationStatus {
    object Idle : BiographyGenerationStatus()
    data class Processing(
        val currentCluster: Int,
        val totalClusters: Int,
        val step: String
    ) : BiographyGenerationStatus()
    data class Completed(val biography: UserBiography) : BiographyGenerationStatus()
    data class Failed(val error: String) : BiographyGenerationStatus()
}
