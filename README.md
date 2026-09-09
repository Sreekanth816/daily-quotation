# Telugu Photo Quote

An Android app (Kotlin + Jetpack Compose) that lets users create beautiful
image quotes. Type or speak Telugu (or English) text, overlay it onto one
of 100+ professional background photos, and share the result.

## Recent Updates

- **Onboarding & Profile**: New flow where users set a name and avatar.
- **Watermarking**: Profile info is rendered onto the final image.
- **Draggable Text**: Interactive preview with position presets.
- **Image Pool**: 124 bundled high-quality background images.
- **Voice Toggle**: Telugu/English language switch for speech-to-text.
- **Project Structure**: Added `ProfileFormScreen.kt` and `UserProfile.kt`.

## Key Features

- **User Onboarding** — A first-run experience to set up your profile name
  and avatar, which are used to watermark your creations.
- **Dynamic Text Input** — A standard `OutlinedTextField` for typing,
  leveraging system keyboards (like Gboard) for Telugu input.
- **Voice Input** — `SpeechRecognizer` via `RecognizerIntent` with a
  dedicated Telugu/English toggle for high-accuracy dictation.
- **Interactive Preview** — Drag the quote text directly on the photo to
  position it exactly where you want, or use quick-jump presets (Top,
  Center, Bottom).
- **Background Image Pool** — Includes 124 bundled high-quality background
  images specifically curated for quotes.
- **Watermarked Rendering** — `PhotoQuoteRenderer.kt` draws your caption
  onto the photo using `StaticLayout` for perfect Telugu conjunct
  consonant shaping, and adds a professional profile badge (avatar + name)
  in the corner.
- **Save/Share** — High-resolution export to `MediaStore.Images` and
  standard Android share sheet integration.

## ⚠️ Required setup before building: add font files

The app renders text using **Noto Sans Telugu**, which is not bundled in
this project due to licensing/size. You must add it yourself:

1. Download **Noto Sans Telugu** (Regular, 400 weight) from
   https://fonts.google.com/noto/specimen/Noto+Sans+Telugu
2. Rename the file to `noto_sans_telugu_regular.ttf`
3. Place it in `app/src/main/res/font/`

Android's text-layout engine will automatically handle mixed Telugu+English
captions. For the best visual consistency, the app also looks for
`noto_sans_regular.ttf` as a fallback.

## How to open and run

1. Open this folder in **Android Studio** (Koala/2024.1 or newer
   recommended).
2. Let Gradle sync — it will pull all dependencies (including `ExifInterface`
   for photo rotation handling) from Google/Maven automatically.
3. Add the font file as described above.
4. Run on a device or emulator with **API 24+**. Voice input requires
   Google Play Services.

## Project structure

```
app/src/main/java/com/example/teluguphotoquote/
├── MainActivity.kt          # App entry point, screen navigation, and main UI
├── PhotoQuoteRenderer.kt    # Core: renders text and profile badges onto bitmaps
├── SpeechInputHelper.kt     # Speech-to-text integration logic
├── ProfileFormScreen.kt     # UI for user onboarding and profile editing
└── UserProfile.kt           # Data models and persistence for user settings

app/src/main/res/
├── font/telugu_font_family.xml   # Font family declaration
├── xml/file_paths.xml            # FileProvider paths for camera/share photos
└── values/                       # strings, colors, themes
```

## Implementation Details

- **Telugu Rendering**: Uses `StaticLayout` instead of raw `Canvas.drawText`
  to ensure complex Telugu glyphs (ottulu) and conjuncts are shaped
  correctly.
- **Scaling**: The renderer uses a `scaleFactor` logic to ensure text and
  watermarks look identical regardless of the background photo's
  resolution (from 1080p to 4K).
- **Profile Persistence**: User avatars are copied to internal app storage
  to ensure they remain available even if the original source image is
  deleted from the gallery.
- **Exif Handling**: Uses `androidx.exifinterface` to ensure profile
  pictures and camera captures are correctly oriented.
