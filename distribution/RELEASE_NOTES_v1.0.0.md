# ExamSathi v1.0.0 — Production Release Notes

**ExamSathi** is an offline-first examination preparation and practice platform for Indian competitive examinations (SSC CGL, UPSC Civil Services, Banking / IBPS / SBI PO, and Railway RRB).

---

## 🚀 What's New in v1.0.0

- **Offline-First Exam Preparation**: Complete syllabus coverage for UPSC, SSC, Banking, and Railways with pre-seeded question banks that function with zero internet connection.
- **Full Examination Simulation**: Real-time countdown timer, comprehensive question status palette (Answered, Unanswered, Marked for Review), and instant answer state retention.
- **Instant Result Evaluation**: Comprehensive scorecards featuring negative marking (-0.50 marks per wrong answer), estimated percentile, subject performance breakdowns, and question-by-question solution explanations.
- **Adaptive Practice Engine**: Dual-mode engine providing targeted question generation based on subject difficulty, with seamless automatic fallback to local rule-based generation when offline or when Gemini AI is unreachable.
- **Institutional Branding & PDF Reports**: Full institution customizability (name, tagline, contact) and on-device scorecard PDF generation with watermarks and digital verification seals, shareable directly via Android system share.
- **Admin Portal & Question Bank Management**: PIN-protected security portal with salted SHA-256 hashing, subject filtering, inline question editing, and bulk JSON import/export.
- **Over-The-Air (OTA) Updates**: Built-in update checking and verified APK installation directly from GitHub Releases or custom HTTPS endpoints.

---

## 📦 Release Assets & Verification

| Asset | Description | Size |
|---|---|---|
| `ExamSathi-v1.0.0-release.apk` | Signed Production APK | 16,041,377 bytes (~15.3 MB) |
| `version.json` | OTA Update Metadata Specification | ~1 KB |

### Cryptographic Checksums (SHA-256)
```text
8b697abd3fe690db7ff3e48066662fc6d6926a1f3934fa4fc3555d5d5da24c71  ExamSathi-v1.0.0-release.apk
```

### Signing Details
- **Application ID**: `com.examsathi.app`
- **Signing Scheme**: APK Signature Scheme v2 (Full APK integrity verification)
- **Signer DN**: `CN=ExamSathi, OU=ExamPrep, O=ExamSathi, L=New Delhi, ST=Delhi, C=IN`
- **Minimum Android Version**: Android 7.0 (API 24)
- **Target Android Version**: Android 16 (API 36)

---

## 📲 Installation Instructions

1. Download `ExamSathi-v1.0.0-release.apk` from the GitHub release assets below.
2. On your Android device, open the downloaded APK file.
3. If prompted by Android, enable **"Allow installation from this source"** for your browser or file manager.
4. Tap **Install** to complete setup. All student progress and questions will be initialized automatically.
