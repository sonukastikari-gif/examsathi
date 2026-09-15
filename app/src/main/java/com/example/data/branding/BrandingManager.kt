package com.example.data.branding

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

/**
 * Centralized Branding Manager for ExamSathi.
 * Acts as the single source of truth for:
 * - App Logo
 * - PDF Logo
 * - Brand Name & Tagline
 *
 * Provides Admin-only controls to replace or remove custom logo with confirmation,
 * while Student Views consume the configured state in read-only mode.
 */
class BrandingManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _brandingConfig = MutableStateFlow(loadConfig())
    val brandingConfig: StateFlow<BrandingConfig> = _brandingConfig.asStateFlow()

    private fun loadConfig(): BrandingConfig {
        val brandName = prefs.getString(KEY_BRAND_NAME, BrandingConfig.DEFAULT_BRAND_NAME)
            ?: BrandingConfig.DEFAULT_BRAND_NAME
        val tagline = prefs.getString(KEY_TAGLINE, BrandingConfig.DEFAULT_TAGLINE)
            ?: BrandingConfig.DEFAULT_TAGLINE
        val subTagline = prefs.getString(KEY_SUB_TAGLINE, BrandingConfig.DEFAULT_SUB_TAGLINE)
            ?: BrandingConfig.DEFAULT_SUB_TAGLINE
        val orgName = prefs.getString(KEY_ORG_NAME, BrandingConfig.DEFAULT_ORG_NAME)
            ?: BrandingConfig.DEFAULT_ORG_NAME
        val hasCustom = prefs.getBoolean(KEY_HAS_CUSTOM_LOGO, false)
        val logoPath = prefs.getString(KEY_CUSTOM_LOGO_PATH, null)
        val preset = prefs.getString(KEY_PRESET_LOGO, BrandingConfig.PRESET_EMBLEM)
            ?: BrandingConfig.PRESET_EMBLEM
        val updatedAt = prefs.getLong(KEY_UPDATED_AT, System.currentTimeMillis())

        // Verify that custom logo file still exists on disk if flagged
        val verifiedHasCustom = hasCustom && logoPath != null && File(logoPath).exists()

        return BrandingConfig(
            brandName = brandName,
            tagline = tagline,
            subTagline = subTagline,
            organizationName = orgName,
            hasCustomLogo = verifiedHasCustom,
            customLogoPath = if (verifiedHasCustom) logoPath else null,
            presetLogo = preset,
            updatedAt = updatedAt
        )
    }

    /**
     * Update Brand Name, Taglines, and Organization (Admin only)
     */
    fun updateBrandDetails(
        brandName: String,
        tagline: String,
        subTagline: String,
        organizationName: String
    ) {
        val cleanName = brandName.trim().ifBlank { BrandingConfig.DEFAULT_BRAND_NAME }
        val cleanTagline = tagline.trim().ifBlank { BrandingConfig.DEFAULT_TAGLINE }
        val cleanSubTagline = subTagline.trim().ifBlank { BrandingConfig.DEFAULT_SUB_TAGLINE }
        val cleanOrg = organizationName.trim().ifBlank { BrandingConfig.DEFAULT_ORG_NAME }

        prefs.edit()
            .putString(KEY_BRAND_NAME, cleanName)
            .putString(KEY_TAGLINE, cleanTagline)
            .putString(KEY_SUB_TAGLINE, cleanSubTagline)
            .putString(KEY_ORG_NAME, cleanOrg)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()

        _brandingConfig.value = _brandingConfig.value.copy(
            brandName = cleanName,
            tagline = cleanTagline,
            subTagline = cleanSubTagline,
            organizationName = cleanOrg,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Select a built-in preset logo badge (Admin only)
     */
    fun setPresetLogo(preset: String) {
        prefs.edit()
            .putString(KEY_PRESET_LOGO, preset)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()

        _brandingConfig.value = _brandingConfig.value.copy(
            presetLogo = preset,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Replace logo with a custom Bitmap image (Admin only)
     */
    fun saveCustomLogo(bitmap: Bitmap): Boolean {
        return try {
            val brandingDir = File(context.filesDir, "branding").apply { mkdirs() }
            val logoFile = File(brandingDir, "custom_logo.png")

            // Scale to max 512x512 to conserve memory while retaining high PDF print quality
            val scaledBitmap = scaleBitmapToMax(bitmap, 512)
            FileOutputStream(logoFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            prefs.edit()
                .putBoolean(KEY_HAS_CUSTOM_LOGO, true)
                .putString(KEY_CUSTOM_LOGO_PATH, logoFile.absolutePath)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply()

            _brandingConfig.value = _brandingConfig.value.copy(
                hasCustomLogo = true,
                customLogoPath = logoFile.absolutePath,
                updatedAt = System.currentTimeMillis()
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Replace logo from an image URI (Admin only)
     */
    fun saveCustomLogoFromUri(uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return false
            val decoded = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (decoded != null) {
                saveCustomLogo(decoded)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Remove custom logo and revert to the default ExamSathi emblem (Admin only)
     */
    fun removeCustomLogo(): Boolean {
        return try {
            val logoPath = _brandingConfig.value.customLogoPath
            if (logoPath != null) {
                val file = File(logoPath)
                if (file.exists()) {
                    file.delete()
                }
            }

            prefs.edit()
                .putBoolean(KEY_HAS_CUSTOM_LOGO, false)
                .remove(KEY_CUSTOM_LOGO_PATH)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply()

            _brandingConfig.value = _brandingConfig.value.copy(
                hasCustomLogo = false,
                customLogoPath = null,
                updatedAt = System.currentTimeMillis()
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reset branding configuration back to factory default
     */
    fun resetToDefaults() {
        prefs.edit().clear().commit()
        val brandingDir = File(context.filesDir, "branding")
        if (brandingDir.exists()) {
            brandingDir.deleteRecursively()
        }
        _brandingConfig.value = loadConfig()
        cachedLogoBitmap = null
    }

    @Volatile
    private var cachedLogoBitmap: Bitmap? = null
    private var cachedTargetSize: Int = 0
    private var cachedConfigUpdatedAt: Long = -1L

    /**
     * Get the authoritative Bitmap for the logo (used in UI and PDF documents).
     * If a custom logo exists and is valid, returns it;
     * otherwise draws a crisp default ExamSathi emblem.
     */
    fun getLogoBitmap(targetSize: Int = 160): Bitmap {
        val config = _brandingConfig.value
        val cached = cachedLogoBitmap
        if (cached != null && !cached.isRecycled && cachedTargetSize == targetSize && cachedConfigUpdatedAt == config.updatedAt) {
            return cached
        }

        val resultBitmap = if (config.hasCustomLogo && config.customLogoPath != null) {
            try {
                val customFile = File(config.customLogoPath)
                if (customFile.exists()) {
                    val customBmp = BitmapFactory.decodeFile(customFile.absolutePath)
                    if (customBmp != null) {
                        scaleBitmapToMax(customBmp, targetSize)
                    } else {
                        renderDefaultLogoEmblem(targetSize, config.presetLogo)
                    }
                } else {
                    renderDefaultLogoEmblem(targetSize, config.presetLogo)
                }
            } catch (_: Exception) {
                renderDefaultLogoEmblem(targetSize, config.presetLogo)
            }
        } else {
            renderDefaultLogoEmblem(targetSize, config.presetLogo)
        }

        cachedLogoBitmap = resultBitmap
        cachedTargetSize = targetSize
        cachedConfigUpdatedAt = config.updatedAt
        return resultBitmap
    }

    /**
     * Programmatically renders a high-definition, crisp vector badge
     * representing the ExamSathi brand for PDF documents and screen display.
     */
    private fun renderDefaultLogoEmblem(size: Int, preset: String): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = size / 160f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        // Circular background
        paint.color = Color.parseColor("#1E1B4B") // Deep Indigo
        canvas.drawCircle(size / 2f, size / 2f, 70f * scale, paint)

        // Inner ring
        paint.color = Color.parseColor("#4F46E5") // Indigo accent
        canvas.drawCircle(size / 2f, size / 2f, 65f * scale, paint)

        paint.color = Color.parseColor("#0F172A") // Slate Dark
        canvas.drawCircle(size / 2f, size / 2f, 58f * scale, paint)

        // Draw Graduation Cap / Scholar Book Emblem
        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#FBBF24") // Amber Gold
        }

        // Open Book Base
        val bookPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#E0E7FF") // Soft Lilac
        }
        val bookPath = Path().apply {
            moveTo(44f * scale, 102f * scale)
            quadTo(60f * scale, 96f * scale, 80f * scale, 100f * scale)
            quadTo(100f * scale, 96f * scale, 116f * scale, 102f * scale)
            lineTo(116f * scale, 110f * scale)
            quadTo(100f * scale, 104f * scale, 80f * scale, 108f * scale)
            quadTo(60f * scale, 104f * scale, 44f * scale, 110f * scale)
            close()
        }
        canvas.drawPath(bookPath, bookPaint)

        // Graduation Cap Top (Rhombus)
        val capPath = Path().apply {
            moveTo(80f * scale, 52f * scale)
            lineTo(116f * scale, 68f * scale)
            lineTo(80f * scale, 84f * scale)
            lineTo(44f * scale, 68f * scale)
            close()
        }
        canvas.drawPath(capPath, capPaint)

        // Cap Base
        val capBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#D97706") // Dark Amber
        }
        val capBasePath = Path().apply {
            moveTo(58f * scale, 74f * scale)
            lineTo(58f * scale, 84f * scale)
            quadTo(80f * scale, 92f * scale, 102f * scale, 84f * scale)
            lineTo(102f * scale, 74f * scale)
            lineTo(80f * scale, 84f * scale)
            close()
        }
        canvas.drawPath(capBasePath, capBasePaint)

        // Tassel
        val tasselPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        canvas.drawRect(
            RectF(78f * scale, 68f * scale, 82f * scale, 86f * scale),
            tasselPaint
        )

        // Gold Star / Sparkle
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#FEF08A")
        }
        canvas.drawCircle(112f * scale, 56f * scale, 4.5f * scale, starPaint)

        return bitmap
    }

    private fun scaleBitmapToMax(source: Bitmap, maxDim: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDim && height <= maxDim) return source

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDim
            newHeight = (maxDim / ratio).toInt()
        } else {
            newHeight = maxDim
            newWidth = (maxDim * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(source, newWidth.coerceAtLeast(1), newHeight.coerceAtLeast(1), true)
    }

    companion object {
        private const val PREFS_NAME = "examsathi_branding_prefs"
        private const val KEY_BRAND_NAME = "key_brand_name"
        private const val KEY_TAGLINE = "key_tagline"
        private const val KEY_SUB_TAGLINE = "key_sub_tagline"
        private const val KEY_ORG_NAME = "key_org_name"
        private const val KEY_HAS_CUSTOM_LOGO = "key_has_custom_logo"
        private const val KEY_CUSTOM_LOGO_PATH = "key_custom_logo_path"
        private const val KEY_PRESET_LOGO = "key_preset_logo"
        private const val KEY_UPDATED_AT = "key_updated_at"

        @Volatile
        private var instance: BrandingManager? = null

        fun getInstance(context: Context): BrandingManager {
            return instance ?: synchronized(this) {
                instance ?: BrandingManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
