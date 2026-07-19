# JS Today's Quote

An Android app (Kotlin + Jetpack Compose) that lets a user type or speak
Telugu (or English) text and overlay it onto a photo, then save/share the
result.

## What's implemented

- **Text input** — a standard `OutlinedTextField` (Gboard handles Telugu
  input automatically, no custom keyboard needed).
- **Voice input** — `SpeechRecognizer` via `RecognizerIntent`, with a
  Telugu/English toggle chip (`SpeechInputHelper.kt`).
- **Photo source** — Android Photo Picker for gallery (`PickVisualMedia`,
  no permission needed on API 33+) and in-app camera capture
  (`TakePicture` + `FileProvider`).
- **Rendering** — `PhotoQuoteRenderer.kt` draws the caption onto the photo
  using `StaticLayout` on a `Canvas`, which correctly shapes Telugu
  conjunct consonants (this is why raw `Canvas.drawText` is avoided).
- **Save/Share** — saves to `MediaStore.Images` (scoped storage, API 29+
  aware) and offers a standard Android share sheet.

## ⚠️ Required setup before building: add font files

The app renders text using **Noto Sans Telugu**, which is not bundled in
this project (binary font files can't be generated programmatically). You
must add it yourself:

1. Download **Noto Sans Telugu** (Regular, 400 weight) from
   https://fonts.google.com/noto/specimen/Noto+Sans+Telugu
2. Rename the file to `noto_sans_telugu_regular.ttf`
3. Place it in `app/src/main/res/font/`

That's it — `res/font/telugu_font_family.xml` already references this
file, and Android's text-layout engine (Minikin/HarfBuzz) will
automatically fall back to a system Latin font for any English characters
in the same string, so mixed Telugu+English captions render cleanly out
of the box.

*(Optional enhancement noted in code comments: for pixel-matched Latin
glyphs instead of the system fallback, also add `noto_sans_regular.ttf`
and wire up `Typeface.CustomFallbackBuilder` on API 29+, per the comment
in `QuoteTypefaceFactory`.)*

## How to open and run

1. Open this folder in **Android Studio** (Koala/2024.1 or newer
   recommended).
2. Let Gradle sync — it will pull all dependencies from Google/Maven
   automatically (internet required on your dev machine).
3. Add the font file as described above.
4. Run on a device or emulator with **API 24+**. For voice input testing,
   use a real device or an emulator image that includes Google Play
   Services (voice typing needs it).

## Project structure

```
app/src/main/java/com/example/teluguphotoquote/
├── MainActivity.kt          # Compose UI: input, photo pick/camera, preview, save/share
├── PhotoQuoteRenderer.kt    # Core: draws text onto bitmap (StaticLayout + Canvas)
└── SpeechInputHelper.kt     # Speech-to-text intent builder (Telugu/English)

app/src/main/res/
├── font/telugu_font_family.xml   # Font family declaration (add .ttf here)
├── xml/file_paths.xml            # FileProvider paths for camera photos
└── values/                       # strings, theme
```

## Natural next steps

- Add more style controls (font size slider, text color picker, more
  font choices).
- Add undo/redo or multiple text layers.
- Add image filters/crop before captioning.
- If you want offline-first voice recognition reliability across all
  devices, consider Google Cloud Speech-to-Text (`te-IN`) as a fallback
  when the on-device recognizer isn't available.
