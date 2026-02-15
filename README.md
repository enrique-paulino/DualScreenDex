# DualScreenDex

**DualScreenDex** is a companion Pokedex app designed specifically for dual-screen Android gaming handhelds (like the Ayn Thor). It uses live OCR screen scanning to automatically detect the Pokémon currently on your screen and display their stats, weaknesses, and resistances instantly.

---

## New in v2.0

* **Modular Architecture:** Complete internal refactor replacing the previous monolithic system with a cleaner, more scalable modular structure.
* **Custom ROM Profiles:**
  * Built-in profiles for:
    * Vanilla Pokémon (Gen 1)
    * Vanilla Pokémon (Gen 2–5)
    * Vanilla Pokémon (Gen 6+)
    * Luminescent Platinum
    * Radical Red
  * Create fully custom profiles by uploading your own `.csv` files:
    * Custom Pokédex data
    * Regional variants
    * Type matchup logic
    * Designed to support any ROM hack or custom game.
* **Advanced Screen Scanning:**
  * Choose whether to scan the **top or bottom screen**.
  * Select scanning orientation (**left side or right side**) depending on how your game displays enemy Pokémon.
  * Ideal for different emulator layouts and custom ROM UI designs.

---

## Features

* **Live Battle Scanner:** Uses Android's AccessibilityService and Google ML Kit to scan the selected screen region for Pokémon names in real-time.
* **Themes (introduced in v1.1):**
  * **OLED Mode:** True black background for OLED screens.
  * **Pokedex Red:** Classic green-tinted aesthetic with scanlines.
  * **Pastel Magic:** Pastel gradients with floating stars.
  * **Dynamic:** Adapts UI colours based on the detected Pokémon's type.
* **Smart Multitasking:**
    * **Battle Mode:** Automatically displays data for the Pokémon detected on screen (supports multi-Pokémon battles e.g. 2v2, 3v3).
    * **Pokedex Mode:** Browse the full database manually.
    * **"Battle Tab":** Minimises active battle data to a small tab at the bottom while you browse, allowing you to multitask without losing your place.
* **Generation Selector:** Dynamic type system allows you to switch between logic for Gen 1, Gen 2–5, and Gen 6+ (e.g. retrofitting Fairy types back to Normal for older games).
* **Battery Optimised:**
    * Scanner automatically sleeps (`onPause`) when the app is backgrounded.
    * Uses a dynamic polling rate (2000ms) to minimise CPU usage and heat.
    * Crops image processing to the selected game window region to save resources.
 
* **Database Features**
   * **Regional Variants:** Full support for **Alolan, Galarian and Hisuian** forms.
   * **Form Switching:** A toggle button appears automatically when a Pokémon has multiple forms.
   * **Offline Ready:** Includes a complete database of all 1,025 Pokémon.

---

## Roadmap

* [x] **Custom ROM Support:** Import `.csv` files to support ROM hacks.
* [x] **Custom Matchup Logic:** User-defined type effectiveness charts (e.g. changing Fire to be weak against Ice).
* [x] **v1.1:** Themes, regional variants, and improved scanning.
* [x] **v2.0:** Modular architecture, custom ROM profiles, advanced screen scanning controls.
* [ ] **Language Support:** Support for non-English languages that have different Pokémon names (e.g. Japanese).
* [ ] **TTS:** Have the app read out Pokémon entries in a Pokédex-robot voice.

---

## Demo Video

<a href="https://www.youtube.com/watch?v=JMTiW8wY358">
  <video src="https://github.com/user-attachments/assets/7e5c7c09-2865-4e57-9b8e-76198134f4fb" width="400" controls muted autoplay loop>
  </video>
</a>

---

## Screenshots

| Dynamic Theme | OLED Theme | Pokemon Red Theme | Pastel Magic Theme |
|:---:|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/098a89e0-bafb-48f0-b330-0c7fb881911e" width="200" /> | <img src="https://github.com/user-attachments/assets/7f922481-6fd7-4688-b9da-d0dc99cf9177" width="200" /> | <img src="https://github.com/user-attachments/assets/21c2bafa-c446-4ed8-93bb-4437cdc17adf" width="200" /> | <img src="https://github.com/user-attachments/assets/f5a0858e-13ab-48e0-88c7-0ef487a65850" width="200" /> |
| <img src="https://github.com/user-attachments/assets/35895897-4248-483e-89e6-0b6a5c1dbf07" width="200" /> | <img src="https://github.com/user-attachments/assets/e16ea13b-265e-4cec-9111-a7290afd43ac" width="200" /> | <img src="https://github.com/user-attachments/assets/bb7c675f-f7b5-4631-a6f2-9dfac91a92a3" width="200" /> | <img src="https://github.com/user-attachments/assets/2e147b3d-0f34-4632-97fb-20150ed458ab" width="200" /> |

---

## CSV Formats
- **Pokedex CSV:** `id,name,type1,type2` [e.g. vanilla pokedex](https://github.com/enrique-paulino/DualScreenDex/blob/master/app/src/main/assets/dex/vanilla_pokedex.csv)
- **Regional Forms CSV:** `id,region,type1,type2` [e.g. vanilla regional](https://github.com/enrique-paulino/DualScreenDex/blob/master/app/src/main/assets/dex/vanilla_regional.csv)
- **Matchup CSV:** [Standard matchup chart](https://github.com/enrique-paulino/DualScreenDex/blob/master/app/src/main/assets/dex/vanilla_matchup.csv) 

---

## Installation

1. Download the latest APK from the [Releases](../../releases) page.
2. Install the APK on your Android device.
3. You will be prompted to enable **DualScreenDex** in your Android **Accessibility Settings**.
    * *Note: This permission is used strictly to read the screen content for local text recognition. No images are saved or transmitted off-device.*

---

## Tech Stack

* **Language:** Kotlin
* **UI:** XML Layouts / Material Design
* **OCR:** Google ML Kit (On-Device Text Recognition)
* **Database:** SQLite (Pre-populated asset)
* **Architecture (v2.0):** Modular structure using `AccessibilityService` and Global `BroadcastReceiver`

---

## License

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.

---

*Disclaimer: DualScreenDex is an unofficial, free fan-made app and is NOT affiliated, endorsed, or supported by Nintendo, Game Freak, or The Pokémon Company in any way. Pokémon and Pokémon character names are trademarks of Nintendo.*
