# MCQ Scanner-Checker

An Android app that reads and grades multiple-choice (OMR/bubble) answer sheets straight from your phone's camera — no scanner needed.

**Developed by:** Sir JPagdi
**Inspired by:** [OMRChecker](https://github.com/Udayraj123/OMRChecker) by Udayraj123 (Python/OpenCV desktop tool), reimagined as a native, offline-first Android app for classroom use.

## Why this exists

Sir JPagdi (Julius C. Pagdilao, Teacher II, Tarlac National High School) builds this to make checking Grade 12 STEM multiple-choice tests — Advanced Math, Pre-Calc, and beyond — as fast as snapping a photo.

## How it works

1. **Set Up Template** – tell the app how many questions, how many choices (A–D, A–E, etc.), and how many question-columns are on your answer sheet.
2. **Set Answer Key** – type in the correct answers once per test.
3. **Scan Student Sheet** – take a photo of a filled-out sheet, drag the 4 corners onto the sheet's edges to correct for angle/perspective, and the app grades it instantly on-device using OpenCV (Otsu thresholding + per-bubble fill-ratio detection — the same core idea OMRChecker uses, adapted for a live camera + touch workflow).
4. **Results** – see the score, per-question breakdown, save it, and export to CSV.
5. **History** – view all checked results and export the whole class to one CSV.

Everything runs **fully offline** — no internet, no cloud upload of student data.

## Tech stack

- Kotlin, single-module Android app (`minSdk 24`, `targetSdk 34`)
- CameraX for camera capture
- OpenCV (Android AAR via Maven Central) for perspective warp + thresholding
- SharedPreferences + JSON for lightweight local storage (template, answer key, results history)
- No backend, no accounts, no ads

## Building it yourself (GitHub Codespaces — recommended for first-timers)

1. Push this repo to your own GitHub account (see "Getting this onto GitHub" below).
2. On the repo page, click **Code → Codespaces → Create codespace on main**.
3. Wait for the container to finish setting up (it auto-installs the Android SDK — first boot takes a few minutes).
4. In the Codespace terminal:
   ```bash
   ./gradlew assembleDebug
   ```
5. Your APK will be at `app/build/outputs/apk/debug/app-debug.apk`. Right-click it in the file explorer → **Download**, then transfer it to your Android phone and install it (you'll need to allow "install unknown apps" for your file manager/browser).

Alternatively — every push to `main` automatically builds the APK via GitHub Actions (see the **Actions** tab → latest run → **Artifacts**). No local setup needed at all.

## Building locally with Android Studio

1. Clone the repo.
2. Open it in Android Studio (Giraffe or newer).
3. Let it sync Gradle, then Run ▶ on a device/emulator, or **Build → Build Bundle(s)/APK(s) → Build APK(s)**.

## Project structure

```
app/src/main/java/com/sirjpagdi/mcqscanner/
├── MainActivity.kt          # home screen / navigation
├── TemplateActivity.kt      # define sheet layout (# questions, choices, columns)
├── AnswerKeyActivity.kt     # enter correct answers
├── ScanActivity.kt          # CameraX capture screen
├── CornerSelectActivity.kt  # drag corners to align sheet
├── CornerSelectView.kt      # custom draggable-corner overlay view
├── OMRProcessor.kt          # OpenCV grading core (perspective warp + bubble detection)
├── ResultsActivity.kt       # score + per-question breakdown + CSV export
├── HistoryActivity.kt       # saved results list + bulk CSV export
├── Prefs.kt                 # local JSON persistence
├── ImageUtils.kt            # ImageProxy -> Bitmap helpers
└── SheetImageHolder.kt      # in-memory bitmap handoff between screens
```

## Customizing further

- **Theme colors**: `app/src/main/res/values/colors.xml` (currently a chalkboard green/yellow/red palette to match your PTA presentation branding).
- **Grading sensitivity**: tune `FILL_THRESHOLD` and `AMBIGUITY_MARGIN` in `OMRProcessor.kt` if bubbles are being mis-detected (e.g. very light pencil marks).
- **App name / package**: change `applicationId` and `namespace` in `app/build.gradle.kts`, and `app_name` in `strings.xml`.

## Roadmap ideas

- Save/load multiple templates (one per subject/test)
- Batch-scan mode (auto-advance camera between sheets)
- Per-question point weighting
- Export directly to Google Sheets

## License

Based on ideas from OMRChecker (Apache-2.0). This Android reimplementation is original code — see `LICENSE`.
