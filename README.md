# DualScreenDex

**DualScreenDex** is a companion Pokedex app designed specifically for dual-screen Android gaming handhelds (like the Ayn Thor). It uses live OCR screen scanning to automatically detect the Pokémon currently on your screen and display their stats, weaknesses, and resistances instantly.

## Features

* **Live Battle Scanner:** Uses Android's MediaProjection and Google ML Kit to scan the top screen for Pokémon names in real-time.
* **Themes (new in v1.1):**
  * **OLED Mode:** True black background for OLED screens
  * **Pokedex Red:** Classic green-tinted aesthetic with scanlines.
  * **Pastel Magic:** Pastel gradients with floating stars.
  * **Dynamic:** Adapts UI colors based on the detected Pokemon's type.
* **Smart Multitasking:**
    * **Battle Mode:** Automatically displays data for the Pokémon detected on screen (supports multi-pokemon battles e.g. 2v2, 3v3).
    * **Pokedex Mode:** Browse the full database manually.
    * **"Battle Tab":** Minimizes active battle data to a small tab at the bottom while you browse, allowing you to multitask without losing your place.
* **Generation Selector:** Dynamic type system allows you to switch between logic for Gen 1, Gen 2-5, and Gen 6+ (e.g., retrofitting Fairy types back to Normal for older games).
* **Battery Optimized:**
    * Scanner automatically sleeps (`onPause`) when the app is backgrounded.
    * Uses a dynamic polling rate (2000ms) to minimize CPU usage and heat.
    * Crops image processing to the game window to save resources.
 
* **Database Features**
   * **Regional Variants:** Full support for **Alolan, Galarian and Hisuian** forms.
   * **Form Switching:** A toggle button appears automatically when a Pokemon has multiple forms.
   * **Offline Ready:** Includes a complete database of all 1,025 Pokémon.

## Roadmap
* [x] **v1.1 (current):** Themes, regional variants, and improved scanning.
* [ ] **Custom ROM Support:** Allow users to import their own `.csv` files to support ROM hacks.
* [ ] **Custom Matchup Logic:** Support for user-defined type effectiveness charts (e.g., changing Fire to be weak against Ice).
* [ ] **Language Support:** Support for non-english languages that have different pokemon names (e.g. Japanese)
* [ ] **TTS:** Have the app read out Pokemon entries in a Pokedex-robot voice.

## Demo Video

<a href="https://www.youtube.com/watch?v=JMTiW8wY358">
  <video src="https://github.com/user-attachments/assets/7e5c7c09-2865-4e57-9b8e-76198134f4fb" width="400" controls muted autoplay loop>
  </video>
</a>

## Screenshots

| Dynamic Theme | OLED Theme | Retro Theme | Magical Theme |
|:---:|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/098a89e0-bafb-48f0-b330-0c7fb881911e" width="200" /> | <img src="https://github.com/user-attachments/assets/7f922481-6fd7-4688-b9da-d0dc99cf9177" width="200" /> | <img src="https://github.com/user-attachments/assets/21c2bafa-c446-4ed8-93bb-4437cdc17adf" width="200" /> | <img src="https://github.com/user-attachments/assets/f5a0858e-13ab-48e0-88c7-0ef487a65850" width="200" /> |
| <img src="https://github.com/user-attachments/assets/35895897-4248-483e-89e6-0b6a5c1dbf07" width="200" /> | <img src="https://github.com/user-attachments/assets/e16ea13b-265e-4cec-9111-a7290afd43ac" width="200" /> | <img src="https://github.com/user-attachments/assets/bb7c675f-f7b5-4631-a6f2-9dfac91a92a3" width="200" /> | <img src="https://github.com/user-attachments/assets/2e147b3d-0f34-4632-97fb-20150ed458ab" width="200" /> |

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
