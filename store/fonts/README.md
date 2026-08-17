# Build-time fonts

`Roboto-Regular.ttf` is used only by `../generate_feature_graphic.py` to render the store
feature graphic. **It is not bundled into the app** — `LapseSans = FontFamily.SansSerif` resolves
to the device's own Roboto at runtime, so nothing here is shipped in the APK or AAB.

It is vendored rather than fetched so the graphic regenerates identically on any machine. Pulled
from `/system/fonts/Roboto-Regular.ttf` on the API 36 emulator, which is the exact face the app
renders in, so the banner's tagline matches the app's body text rather than approximating it.

Licensed under the Apache License 2.0 — <https://github.com/googlefonts/roboto-3-classic>.

Instrument Serif is not duplicated here; the generator reads it straight from
`app/src/main/res/font/instrument_serif_regular.ttf`, whose OFL licence is in
`app/src/main/assets/licenses/`.
