# Bujho Toh! 🇮🇳 — the desi forehead guessing game

**बूझो तो!** is a free, Indian-context take on the "phone on your forehead" party game.
Put the phone on your forehead, your friends give clues, **nod down** if you guessed it,
**tilt back** to pass.

No app store, no paywall, no locked decks — it's a web page that works on any phone,
can be installed to the home screen, and keeps working offline.

## What's inside

**16 decks · ~1,190 cards**

| Deck | | How to clue |
|---|---|---|
| 🌶️ Mix Masala | everything shuffled | mixed |
| 🎬 Bollywood Blockbusters | Sholay → Stree 2 | talk |
| 🌟 Filmy Sitaare | actors, Bollywood + South | talk / mimic |
| 🎭 Dumb Charades | desi situations | **act, no talking** |
| 🪔 Tyohaar Time | festivals across India | talk |
| 💍 Shaadi Season | the big fat Indian wedding | talk |
| 🍛 Khaana Peena | street food & mithai | talk |
| 🛍️ Brand Bazaar | Amul to Zepto | talk / sing the jingle |
| 🫖 Ghar ki Cheezein | pressure cooker, steel tiffin… | talk |
| 🏏 Cricket Fever | players, shots, gully rules | talk |
| 🕌 Bharat Darshan | monuments, cities, temples | talk |
| 🔱 Pauranik Kathayein | mythology & folk tales | talk |
| 📺 TV, OTT & Comics | Doordarshan to Panchayat | talk |
| 🗯️ Filmy Dialogues | iconic lines | **act** |
| 🎤 Gaana Bajao | hit songs | **hum, no lyrics** |
| 🛺 Desi Life | Sharma ji ka beta, jugaad… | talk |
| 🐘 Bachcha Party | easy words for kids & grandparents | talk |

Plus **Apna Deck**: make your own deck (family inside jokes, office names, a trip…),
saved on your phone.

## Features

- **Tilt controls**: uses the phone's motion sensor. Screen to the floor = ✅ correct,
  screen to the ceiling = ⏭ pass. Auto-starts when the phone is on your forehead.
- Works on **iPhone and Android**, either landscape direction. If an iPhone has rotation lock
  on, the game rotates the card itself.
- **Laptop mode**: the guesser turns their back to the screen; friends press Space (correct) or P (pass). **Tap mode** for phones without a sensor.
- **Three music styles** (Settings → Music style): ☕ Chill Chai (default, soft piano + flute + light tabla), 🪕 Classic (harmonium + bansuri + dholak), 🥁 Party (bhangra). All synthesized live in the browser
  (harmonium, bansuri, dhol bhangra beat, tumbi, claps, temple-bell "correct",
  shankh "time's up", shehnai fanfare). Nothing copyrighted, nothing to download.
- Animations: marigold-petal confetti, floating diyas, spinning rangoli, card pops.
- Round timer of 30 / 60 / 90 / 120 s, best score per deck, no repeated cards until a deck runs out.
- Vibration feedback, screen kept awake, fullscreen + landscape lock where supported.
- Installable PWA, works offline after the first load.

## Run it locally

Any static file server works:

```bash
python3 -m http.server 8000
```

Then open http://localhost:8000. To use the **motion sensor on a phone** the page must be served over
**HTTPS** (browsers block sensors on plain http, except localhost).

## Live

Play it at **https://oshal7.github.io/headsup/**. It's hosted by GitHub Pages (Settings → Pages → Deploy from a branch → `main` / root);
every push to `main` goes live in about a minute.

## Android app

A native Android version (Kotlin + Jetpack Compose) lives in [`android/`](android/).
It is built entirely by GitHub Actions; no Android Studio needed:

- **Actions → Android app → latest run → Artifacts → `BujhoToh-debug-apk`** to install on your phone.
- Signed `.aab` for Google Play once the signing secrets are added.

See **[PLAY_STORE.md](PLAY_STORE.md)** for the full publishing checklist and store listing text.

## Adding cards

Edit `js/data.js` (the website and the Android app share it; run `node tools/build-decks.js` to update the app's copy). Each deck is a plain object:

```js
{ id: 'my-deck', name: 'My Deck', hindi: 'मेरा डेक', emoji: '🎉',
  colors: ['#ff3d6e', '#ff9a3d'], desc: '…', mode: 'say' /* or 'act' | 'hum' */,
  words: ['…', '…'] }
```

When you change files, bump `CACHE` in `sw.js` (e.g. `bujho-v2`) so installed copies update.

## Code map

```
index.html          screens (home, deck sheet, ready, play, results, settings, custom deck)
css/style.css       theme + all animations
js/data.js          the decks
js/tilt.js          forehead tilt detection (DeviceOrientation → screen-facing vector)
js/audio.js         Web Audio music & SFX engine
js/fx.js            confetti + background diyas
js/app.js           game flow & state
sw.js               offline cache
```
