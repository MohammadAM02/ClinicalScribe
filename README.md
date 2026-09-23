# Clinical Scribe

[![Android build](https://github.com/MohammadAM02/ClinicalScribe/actions/workflows/android.yml/badge.svg)](https://github.com/MohammadAM02/ClinicalScribe/actions/workflows/android.yml)

Clinical Scribe is an Android dictation keyboard for clinical notes. It provides a tap-to-dictate workflow in any Android text field, with on-device speech recognition, medical vocabulary boosting, and reusable clinical snippets.

> **Status:** early-stage personal project. It has not been clinically validated and must not be relied on as a substitute for professional review.

## Highlights

- Custom Android keyboard (IME) that types into the focused app.
- On-device Parakeet TDT speech recognition through sherpa-onnx.
- No transcript history or cloud transcription by default.
- Vocabulary and snippet management with JSON import.
- Companion Compose app for setup, model download, and configuration.
- arm64-v8a target for modern Android phones.

## Requirements

- Android Studio Koala (2024.1) or newer, or JDK 17 and the included Gradle wrapper.
- Android SDK platform 34 and build tools 34.0.0.
- Android device/API 26 or newer; a physical device is recommended for audio testing.
- Network access for the one-time native library and speech-model downloads.

## Build locally

The sherpa-onnx Android release is downloaded separately rather than committed to this repository. From the repository root, run the following in Git Bash:

```bash
VERSION=1.13.8
ARCHIVE="sherpa-onnx-v${VERSION}-android.tar.bz2"
curl -LO "https://github.com/k2-fsa/sherpa-onnx/releases/download/v${VERSION}/${ARCHIVE}"
tar xjf "$ARCHIVE"
mkdir -p app/src/main/jniLibs/arm64-v8a
cp jniLibs/arm64-v8a/*.so app/src/main/jniLibs/arm64-v8a/
rm -rf "$ARCHIVE" jniLibs "sherpa-onnx-v${VERSION}-android"
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. CI performs the native-library fetch automatically and publishes the debug APK as a workflow artifact.

## First run

1. Install and launch the APK.
2. Download the speech model from the home screen. The model is approximately 650 MB and runs on-device.
3. Enable **Clinical Scribe** under Android keyboard settings.
4. Switch to the keyboard in any text field.
5. Tap the microphone, dictate, and tap again to insert the transcript.
6. Import `vocabulary.json` or `snippets.json` from the companion app when needed.

## Project layout

```text
app/src/main/java/com/clinicalscribe/app/
├── asr/       On-device speech recognition and model files
├── data/      Local vocabulary, snippets, and preferences
├── download/  Foreground speech-model download service
├── ime/       Android input method service
└── ui/        Compose companion application screens
```

## Privacy and safety

The app is designed to run speech recognition locally and does not retain transcript history. Android permissions are required for microphone access, network model downloads, and notifications. Review generated text before saving or acting on clinical information. Do not place real patient information in GitHub issues, pull requests, logs, or bug reports.

See [SECURITY.md](SECURITY.md) and [CONTRIBUTING.md](CONTRIBUTING.md) for project guidance.

## Architecture and licensing notes

The app is a native Kotlin/Android application. The keyboard uses regular Android views because it runs inside `InputMethodService`; the companion screens use Jetpack Compose. Speech recognition uses the Apache-2.0-licensed sherpa-onnx project and its Android native release.

A project license for Clinical Scribe itself has not yet been selected. Until one is added, the source should not be redistributed or reused beyond the permissions granted by the repository owner.
