# LiquidLoop

**LiquidLoop** is a specialized native Android audio player designed for musicians, poets, songwriters, and writers. It allows users to visually select a specific segment of a track (an A-B loop) and seamlessly play it in the background while they work on their lyrics or notes in other applications.

## 🎵 Core Concept
The app focuses on providing a gapless, precise looping experience. Whether you are writing lyrics to a beat, practicing a guitar solo, or drafting poetry over an ambient track, LiquidLoop ensures the chosen segment loops perfectly without interruption.

## ✨ Features
* **Interactive Waveform:** Visually select loop boundaries (A and B markers) directly on the track's waveform.
* **Micro-Tuning:** Precisely adjust the start and end of the loop by ±50ms for absolute gapless playback.
* **Background Playback & PiP:** Keep the music looping in the background or use the floating Picture-in-Picture (PiP) widget to control playback while using text editors or messengers.
* **Liquid UI:** A sleek, dark Material 3 design crafted for focus and immersion.
* **High-Precision Audio Engine:** Built on AndroidX Media3 (ExoPlayer) with a custom `LoopWatcher` to ensure micro-fade in/out and eliminate clicking sounds at loop boundaries.

## 🛠️ Technology Stack
* **Language:** Kotlin
* **UI:** Jetpack Compose, Material 3
* **Audio:** AndroidX Media3 (ExoPlayer, MediaSessionService)
* **Architecture:** MVVM, Clean Architecture
* **Target:** Android (Min SDK 26, Target SDK 37)

## 🚀 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/liqtranq/liquid-loop.git
   ```
2. Open the project in **Android Studio**.
3. Sync Gradle and run the app on an emulator or a physical device running Android 8.0+.

## 🤝 Contribution
Feel free to open issues or submit pull requests if you want to improve the looping accuracy, add new audio processing features, or tweak the UI!

## 📄 License
This project is licensed under the MIT License.
