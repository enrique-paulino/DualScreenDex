# DualScreenDex

**DualScreenDex** is a companion Pokedex app designed specifically for dual-screen Android gaming handhelds (like the Ayn Thor). It uses live OCR screen scanning to automatically detect the Pokémon currently on your screen and display their stats, weaknesses, and resistances instantly.

## Features

* **Live Battle Scanner:** Uses Android's MediaProjection and Google ML Kit to scan the top screen for Pokémon names in real-time.
* **Smart Multitasking:**
    * **Battle Mode:** Automatically displays data for the Pokémon detected on screen.
    * **Pokedex Mode:** Browse the full database manually.
    * **"Battle Tab":** Minimizes active battle data to a small tab at the bottom while you browse, allowing you to multitask without losing your place.
* **Generation Selector:** Dynamic type system allows you to switch between logic for Gen 1, Gen 2-5, and Gen 6+ (e.g., retrofitting Fairy types back to Normal for older games).
* **Battery Optimized:**
    * Scanner automatically sleeps (`onPause`) when the app is backgrounded.
    * Uses a dynamic polling rate (2000ms) to minimize CPU usage and heat.
    * Crops image processing to the game window to save resources.
* **Offline Ready:** Includes a complete database of all 1,025 Pokémon.

## Demo Video

<a href="https://www.youtube.com/watch?v=JMTiW8wY358">
  <video src="https://github.com/user-attachments/assets/7e5c7c09-2865-4e57-9b8e-76198134f4fb" width="400" controls muted autoplay loop>
  </video>
</a>

## Installation

1.  Download the latest APK from the [Releases](../../releases) page.
2.  Install the APK on your Android device.
3.  Launch the app and grant the required **Screen Recording** permission.
    * *Note: This permission is used strictly for local text recognition. No images are saved or transmitted off-device.*

## Tech Stack

* **Language:** Kotlin
* **UI:** XML Layouts / Material Design
* **OCR:** Google ML Kit (On-Device Text Recognition)
* **Database:** SQLite (Pre-populated asset)
* **Architecture:** Foreground Service with `LocalBroadcastManager`


## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*Disclaimer: DualScreenDex is an unofficial, free fan-made app and is NOT affiliated, endorsed, or supported by Nintendo, Game Freak, or The Pokémon Company in any way. Pokémon and Pokémon character names are trademarks of Nintendo.*
