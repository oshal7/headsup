# Publishing Bujho Toh! on Google Play

Everything is built by **GitHub Actions** (`.github/workflows/android.yml`). You never need
Android Studio or the Android SDK.

---

## 1. Try the app on your phone (no account needed)

1. Open the repo on GitHub → **Actions** → **Android app** → the latest green run.
2. Under **Artifacts**, download **BujhoToh-debug-apk** (a zip) and unzip it on your phone.
3. Tap the `.apk`. Android will ask you to allow installing from this source → allow → **Install**.

The debug build is called "Bujho Toh!" too, but it installs alongside the Play version (its ID ends in `.debug`).

To rebuild at any time: **Actions → Android app → Run workflow**.

---

## 2. Create your upload key (one time, ~5 minutes)

Google Play only accepts apps signed with *your* key. You create it once and keep it forever.

**Easiest without installing anything: GitHub Codespaces** (free tier is plenty)

1. On the repo page: **Code → Codespaces → Create codespace on main**.
2. In the terminal at the bottom, run (replace the passwords with your own strong one; use the same for both):

   ```bash
   keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000 -storepass 'YOUR_PASSWORD' -keypass 'YOUR_PASSWORD' -dname "CN=Bujho Toh"
   ```

3. Print it as text for GitHub:

   ```bash
   base64 -w0 upload.jks > upload.jks.b64
   ```

4. In the file explorer on the left, right-click `upload.jks` → **Download** and keep it somewhere safe
   (Google Drive / password manager) **together with the password**. Do the same for `upload.jks.b64`.
5. Delete both files from the codespace (`rm upload.jks upload.jks.b64`) and delete the codespace.
   **Never commit them.** (`.gitignore` already blocks `*.jks`.)

> If you ever lose this key, Play App Signing lets you request a new upload key from the Play Console, so it's recoverable, but avoid it.

## 3. Add the key to GitHub (one time)

Repo → **Settings → Secrets and variables → Actions → New repository secret**. Add four:

| Name | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | the whole contents of `upload.jks.b64` |
| `ANDROID_KEYSTORE_PASSWORD` | your password |
| `ANDROID_KEY_ALIAS` | `upload` |
| `ANDROID_KEY_PASSWORD` | your password |

From the next run on, Actions also produces **BujhoToh-release-…** containing the signed
**`app-release.aab`**, which is the file you upload to Google Play.

Every run gets a higher version code automatically (Play requires that).
For a named version, push a tag such as `v1.0.0` and a GitHub Release is created with the files attached.

---

## 4. Google Play Console checklist

### Account
- [ ] Google Play Console developer account (one-time US$25 fee, ID verification).
- [ ] **New personal accounts must run a closed test with at least 12 testers for 14 days in a row**
      before they can publish to everyone. Start this as early as possible: family and friends with Android phones.

### Create the app
- App name: **Bujho Toh!**
- Default language: English (India) – en-IN
- App or game: **Game** · Free
- Package name (set by the build, permanent): **`com.bujhotoh.game`**
  *(Want a different one? Tell me before the first upload; it can never change afterwards.)*

### App content (Policy section)
| Item | Answer |
|---|---|
| Privacy policy URL | `https://oshal7.github.io/headsup/privacy.html` |
| Ads | **No, the app does not contain ads** |
| App access | All functionality available without special access |
| Data safety | **No data collected, no data shared.** (No internet permission; everything stays on the device.) Encryption in transit: not applicable. Users can delete data by uninstalling. |
| Content rating | Fill in the IARC questionnaire honestly: no violence, no gambling, no user-to-user chat, no location. Expect **Everyone / 3+**. |
| Target audience | Suggested: **13–15, 16–17, 18+**. If you also tick under-13 ages, the app enters Google's Families program, which this app already meets (no ads, no data), but review is stricter. |
| News app | No |
| Government app | No |
| Financial features | None |
| Health | None |

### Store listing
- **Category:** Games → Word (or Casual)
- **Tags:** Party, Word, Trivia, Family
- **App icon:** `android/store/icon-512.png`
- **Feature graphic:** `android/store/feature-graphic-1024x500.png`
- **Phone screenshots (2–8):** take them on your phone with the installed app (power + volume-down):
  the home screen, a deck, the "phone on forehead" screen, a card mid-round, the green "Sahi!" flash and the results.
  Normal phone screenshots meet Play's size rules.
- **Contact email:** required (shown publicly), so use one you're happy to share.

**Short description** (max 80 characters):

> Desi party guessing game! Phone on forehead, friends give clues, nod to answer.

**Full description** (paste as is; edit freely):

> Maathe pe phone, dimaag mein dhamaal! 📱🤯
>
> Bujho Toh! is the party guessing game made for India. Put the phone on your forehead, let your friends and family give you clues, and guess the word before time runs out.
>
> ✅ Nod down when you guess it
> ⏭ Tilt your head back to pass
> ⏰ Most correct answers wins!
>
> 16 DESI DECKS, 1,190+ CARDS
> 🎬 Bollywood movies · 🌟 Filmy stars · 🗯️ Iconic dialogues · 🎤 Hit songs (hum them!)
> 🎭 Dumb charades · 🪔 Festivals · 💍 Shaadi season · 🍛 Khaana peena
> 🛍️ Brands we grew up with · 🫖 Ghar ki cheezein · 🏏 Cricket · 🕌 Bharat darshan
> 🔱 Mythology & legends · 📺 TV, OTT & comics · 🛺 Desi life · 🐘 Bachcha party
> 🌶️ Mix Masala — everything shuffled together
>
> MAKE YOUR OWN DECK
> Family inside jokes, office names, your last Goa trip: create unlimited custom decks.
>
> MADE FOR GAME NIGHTS
> • Smooth tilt controls that work on any Android phone
> • Tap & swipe mode when a friend holds the phone
> • Original Indian music: Chill Chai, Classic harmonium & bansuri, or Party bhangra
> • Temple-bell for correct, shankh when time's up, marigold confetti for winners
> • 30 s, 60 s, 90 s or 2 min rounds and best score for every deck
> • Cards don't repeat until you've seen the deck
>
> 100% free. No ads. No sign-up. No internet needed. Nothing is collected.
>
> Perfect for Diwali parties, family get-togethers, road trips, sangeet nights and lazy Sundays.
> Bujho toh jaanein! 🎉

### Content cautions (avoid rejections)
- Don't call it "Heads Up" anywhere (another company's trademark). The app and this text don't.
- Don't use movie posters, actor photos or brand logos in screenshots or graphics. Card text is fine.

### Release flow
1. Play Console → **Testing → Closed testing** → create track → upload `app-release.aab` → add testers' emails → roll out.
2. Keep 12+ testers opted in for 14 days, then apply for **Production**.
3. For updates: push to `main` (or tag `v1.0.1`), download the new `.aab` from Actions, upload it as a new release.

---

## What's in the app (for reviewers & you)

- Kotlin + Jetpack Compose, targets **Android 16 (API 36)**, runs on Android 8.0+ (API 26).
- Only permission: **vibration**. No internet, no ads, no analytics.
- Tilt detection uses the gravity sensor (`TiltLogic.kt`, unit-tested).
- Release builds are minified with R8; native libraries are 16 KB page-size aligned.
- Card data comes from `js/data.js` (shared with the website) via `node tools/build-decks.js`.
- Sounds are rendered from the web game's synth engine via `node tools/render-sounds.js`.
