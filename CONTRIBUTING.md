# Contributing

Thanks for helping improve Clinical Scribe.

## Before opening a pull request

1. Explain the user-facing problem and the smallest proposed change.
2. Keep clinical text handling local and avoid adding transcript storage or telemetry.
3. Keep secrets, signing keys, generated APKs, model files, and native build outputs out of Git.
4. Run `./gradlew assembleDebug` after fetching the sherpa-onnx native libraries.
5. Describe testing performed, including Android API level and device/ emulator details.

## Pull requests

- Use a focused branch and a descriptive title.
- Include screenshots or a short recording for UI changes.
- Call out permission, keyboard/IME, audio, model-download, and privacy implications.
- Do not silently change recognition or clinical text semantics.

## Scope

This project is currently maintained as a personal/early-stage application. Please open an issue before substantial architectural changes.
