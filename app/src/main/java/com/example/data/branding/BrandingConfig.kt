package com.example.data.branding

/**
 * Centralized Branding Configuration - Single source of truth for app branding,
 * student display, admin controls, and generated PDF scorecards.
 */
data class BrandingConfig(
    val brandName: String = DEFAULT_BRAND_NAME,
    val tagline: String = DEFAULT_TAGLINE,
    val subTagline: String = DEFAULT_SUB_TAGLINE,
    val organizationName: String = DEFAULT_ORG_NAME,
    val hasCustomLogo: Boolean = false,
    val customLogoPath: String? = null,
    val presetLogo: String = PRESET_EMBLEM,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_BRAND_NAME = "ExamSathi"
        const val DEFAULT_TAGLINE = "आपकी परीक्षा, आपका साथी"
        const val DEFAULT_SUB_TAGLINE = "Smart Offline Exam Preparation"
        const val DEFAULT_ORG_NAME = "ExamSathi Education"

        const val PRESET_EMBLEM = "emblem"
        const val PRESET_TROPHY = "trophy"
        const val PRESET_SHIELD = "shield"
        const val PRESET_ACADEMY = "academy"
    }
}
