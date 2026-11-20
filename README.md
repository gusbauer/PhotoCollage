# 📸 PhotoCollage

**Native Android photo collage editor built with Kotlin.**

[🇬🇧 English](README.md) · [🇪🇸 Español](README_ES.md)

PhotoCollage lets users select photos, arrange them into customizable grids, edit individual images, and save or share the final composition as a JPEG. Built using the Android SDK, Kotlin, and XML layouts—without relying on external photo-editing applications.

![Android](https://img.shields.io/badge/Android-Native-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-7F52FF?logo=kotlin&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-8.13-02303A?logo=gradle&logoColor=white)
![Min SDK](https://img.shields.io/badge/Min%20SDK-29-blue)

## 📱 App screenshots

The screenshots below show the Android app running on an emulator.

![PhotoCollage — app screenshot 1](screenshots/Screenshot%202026-11-18%20135909.png)

![PhotoCollage — app screenshot 2](screenshots/Screenshot%202026-11-18%20140203.png)

## ✨ Features

### Photo selection and collage management

- Select multiple images with the **Android Photo Picker**, up to **30 photos** per collage.
- Detect duplicate image content using **SHA-256**.
- Display photos in a **RecyclerView** grid with **1–4 configurable columns**.
- Highlight the selected photo and edit it independently.
- Remove one photo, remove several selected for deletion, or clear the collage with confirmation.
- Shuffle photo order and open an enlarged image preview.

### Image editing

- Apply **grayscale** and **sepia** filters.
- Adjust **brightness** and **saturation** using sliders.
- Rotate a photo clockwise in **90° increments**.
- Add custom text, including size and position controls.
- **Restore the original appearance** without modifying the source image in the device gallery.

### Export and persistence

- Render the full collage as a **JPEG** using Android's bitmap and canvas APIs.
- **Save** the result to the device gallery using **MediaStore**.
- **Share** the exported file using an Android **Intent** and **FileProvider**.
- Preserve imported photos, their edits, selection, and layout between app launches.

## 🧠 Technical highlights

| Technology / API | Implementation |
|---|---|
| **Kotlin + Android SDK** | Native Android app logic and lifecycle |
| **XML layouts** | Responsive controls and scrollable main screen |
| **RecyclerView + GridLayoutManager** | Efficient photo grid and configurable columns |
| **Android Photo Picker** | Photo selection without broad gallery permissions |
| **SHA-256** | Content-based duplicate detection |
| **ColorMatrix** | Grayscale, sepia, brightness, and saturation adjustments |
| **Canvas + Bitmap** | Rendering the edited collage for JPEG export |
| **MediaStore + FileProvider** | Saving to the gallery and secure sharing |
| **SharedPreferences** | Persisting the photo collection and editing state |

Imported images are stored in the app's private storage, allowing non-destructive editing and restoration of the original appearance.

## 🚀 Getting started

**Requirements:** Android Studio, JDK 21, Android SDK 36, and an Android emulator or device running **API 29+**.

1. Clone this repository:

   ```bash
   git clone https://github.com/gusbauer/PhotoCollage.git
   ```

2. Open the `PhotoCollage` folder in **Android Studio**.
3. Use the project's **Gradle Wrapper** and configure **JDK 21** for Gradle.
4. Let Gradle sync, then select an emulator or connected Android device.
5. Run the **app** configuration.

To build a debug APK from Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
```

The generated APK can be found at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 🧭 How to use

1. Tap **Add photos** (*Añadir fotos* in the app) and choose multiple pictures.
2. Tap a photo in the grid to select it.
3. Apply grayscale or sepia, adjust brightness/saturation, rotate, or add text.
4. Use **Restore** (*Restaurar*) to reset edits for that photo.
5. Change the number of columns or shuffle the images.
6. Tap **Save JPG** (*Guardar JPG*) or **Share collage** (*Compartir collage*).

> The app interface is currently in Spanish; this README is available in English and Spanish.

## 📂 Project structure

```text
PhotoCollage/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/gusbauer/photocollage/
│       │   ├── MainActivity.kt     # UI events, state, and export
│       │   ├── Foto.kt             # Photo data model
│       │   ├── FotoAdapter.kt      # RecyclerView photo adapter
│       │   └── Imagenes.kt         # Import, image filters, and rendering
│       └── res/                   # Layouts, drawables, and provider paths
├── gradle/                        # Gradle wrapper configuration
├── screenshots/                   # Real app screenshots
├── build.gradle.kts
├── settings.gradle.kts
├── README.md                      # English
└── README_ES.md                   # Spanish
```

---

**About this project:** Developed during my Higher Vocational Training in Multiplatform Application Development (DAM) in Spain, as a practical exercise in native Android development and image processing.
