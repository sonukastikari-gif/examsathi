# ExamSathi — Production Release & Distribution Specification

## 1. Application Identity
- **App Name**: ExamSathi
- **Application ID (Package Name)**: `com.examsathi.app`
- **Internal Namespace**: `com.example` (preserves R class and internal directory structure)
- **Version Name**: `1.0.0`
- **Version Code**: `1`
- **Minimum SDK**: Android 7.0 / 8.0+ (API 24/26)
- **Target SDK**: Android 16 (API 36)
- **Primary Keystore**: `examsathi-release-v2.jks`
  - **Key Algorithm**: RSA 2048-bit
  - **Signer**: `CN=ExamSathi, OU=ExamPrep, O=ExamSathi, L=New Delhi, ST=Delhi, C=IN`
  - **Signature Scheme**: APK Signature Scheme v2 (Full APK integrity verification)

---

## 2. Free Public Distribution Structure (GitHub Releases)

ExamSathi's Over-The-Air (OTA) update system is designed to consume release metadata and APK binaries directly from free GitHub Releases and GitHub raw repository content over secure HTTPS.

### GitHub Repository & Release Asset Structure:
```text
GitHub Repository: https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPOSITORY_NAME

1. Raw metadata endpoint (polled by ExamSathi OTA client):
   https://raw.githubusercontent.com/YOUR_GITHUB_USERNAME/YOUR_REPOSITORY_NAME/main/distribution/version.json

2. Release assets hosted on GitHub Releases:
   https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPOSITORY_NAME/releases/download/v1.0.0/ExamSathi-v1.0.0-release.apk
   https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPOSITORY_NAME/releases/download/v1.0.0/version.json
```

### Current v1.0.0 Update Metadata Format (`version.json`):
```json
{
  "versionCode": 1,
  "versionName": "1.0.0",
  "minSupportedVersionCode": 1,
  "title": "ExamSathi v1.0.0 Official Production Release",
  "releaseDate": "2026-09-15",
  "changelog": [
    "Initial Official Production Release of ExamSathi",
    "Offline-first Indian competitive examination preparation system (SSC, UPSC, Banking, Railway)",
    "Full-featured Mock Test engine with real-time countdown timer and question navigation grid",
    "Instant score evaluation with negative marking (-0.50/wrong) and detailed subject breakdowns",
    "AI-powered Adaptive Practice mode with fallback rule-based generation",
    "Institutional Branding and PDF scorecard generation with watermarks and digital verification badges",
    "Admin Question Bank management portal with PIN protection, bulk JSON import/export, and subject filters",
    "Built-in secure Over-The-Air (OTA) update checker and installer"
  ],
  "apkUrl": "https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPOSITORY_NAME/releases/download/v1.0.0/ExamSathi-v1.0.0-release.apk",
  "apkSizeBytes": 16041377,
  "sha256": "8b697abd3fe690db7ff3e48066662fc6d6926a1f3934fa4fc3555d5d5da24c71"
}
```

---

## 3. Data Preservation Across Future APK Updates

When Android updates an existing application via package replacement (`Intent.ACTION_VIEW` via `FileProvider`):
1. **SQLite / Room Database**: The app's database (`examsathi.db`) resides in `/data/data/com.examsathi.app/databases/`. Android OS preserves this file across updates because both APKs share the identical `applicationId` (`com.examsathi.app`) and signing certificate (`examsathi-release-v2.jks`).
2. **Student Test Results & Analytics**: Preserved automatically in the Room database tables (`test_results`, `student_responses`).
3. **Question Bank**: Admin-created and system-seeded questions stored in `questions` table remain intact.
4. **Institutional Branding**: Stored in SharedPreferences (`examsathi_branding_prefs`), preserved across updates.
5. **Admin Security Credentials**: Stored in SharedPreferences (`examsathi_admin_security_prefs`), including salted PIN hashes and lockout state.
6. **Update Preferences**: Stored in `examsathi_update_prefs`.

---

## 4. Rollback & Recovery Safety

1. **Atomic APK Download**: Download writes to a temporary staging file (`.tmp`), validating byte length before committing the final APK file.
2. **Signature Verification by Android Package Manager**:
   - The Android OS installer enforces certificate continuity. Any unsigned APK, tampered file, or APK signed with a different key is automatically rejected before touching user data.
3. **Non-Destructive Update**: The running APK is never removed prior to successful OS package verification.
4. **Cache Clean-Up**: Downloaded update APKs can be purged on demand via Admin Settings without affecting user data or results.
