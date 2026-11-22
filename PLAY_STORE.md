# Publishing Headzy on Google Play

Everything is built by **GitHub Actions** (`.github/workflows/android.yml`). You never need
Android Studio or the Android SDK.

---

## 1. Try the app on your phone (no account needed)

**Direct link (always the latest build):**
https://github.com/oshal7/headsup/releases/download/latest-build/BujhoToh.apk

Open it on your Android phone → tap to install → allow installing from your browser when asked.
New builds install over the old one.

Or from GitHub Actions:

1. Open the repo on GitHub → **Actions** → **Android app** → the latest green run.
2. Under **Artifacts**, download **BujhoToh-debug-apk** (a zip) and unzip it on your phone.
3. Tap the `.apk`. Android will ask you to allow installing from this source → allow → **Install**.

The debug build is called "Headzy" too, but it installs alongside the Play version (its ID ends in `.debug`).

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
- App name (store title, max 30 characters): **Headzy: Charades Party Game**
  (the brand is "Headzy"; "Charades Party Game" puts the words people actually search for into the title.
  The name on the phone's home screen is just "Headzy".)
- Default language: English (United States) – en-US (add en-IN / en-GB listings later if you like)
- App or game: **Game** · Free
- Package name (set by the build, permanent): **`com.bujhotoh.game`**
  *(It still carries the old name. It shows in the Play Store URL only. If nothing has been uploaded to
  Play Console yet, change `applicationId` in `android/app/build.gradle.kts` to e.g. `com.headzy.game`
  before the first upload; it can never change afterwards.)*

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
- **App icon:** `android/store/icon-512.png` (the Headzy buddy with a phone on its head and tilt arrows)
- **Feature graphic:** `android/store/feature-graphic-1024x500.png`
- **Phone screenshots:** `android/store/screenshots/` (1080×1920, ready to upload in order 1 → 6)
- **Contact email:** required (shown publicly), so use one you're happy to share.

**App name / title** (max 30): `Headzy: Charades Party Game`

**Short description** (max 80 characters):

> Phone on your forehead, friends give clues, tilt to guess! The party word game.

**Full description** (paste as is; edit freely):

> Headzy is the phone-on-your-forehead guessing game for parties, family nights and road trips! 📱🤯
>
> Hold the phone to your forehead, your friends shout clues, act it out or make the sound, and you guess the word before time runs out.
>
> ✅ Tilt down when you guess it
> ⏭ Tilt up to pass
> ⏰ Most correct answers wins!
>
> 30+ DECKS, 3,000+ CARDS
> 🎉 Party Classics: Animals, Everyday Things, Food & Drinks, Famous People, Jobs, Sports, Famous Places, Countries, Brands, Things That Go, Hobbies
> 🎭 Act It Out: Charades, Emotions, Make the Sound, Instruments, Dance Moves
> 🎬 Movies, Music & TV: Movies, Celebrities, Songs (hum them!), TV Shows, Famous Movie Lines. Pick India, USA, UK or China.
> 🧸 Family Fun: Kids (Easy), Cartoons, Heroes & Villains, Fairy Tales, Games & Toys, School Days, Nature & Space, Myths & Legends
> 🪔 Festivals & Traditions: Festivals & Holidays, Weddings, Daily Life
>
> MAKE YOUR OWN DECK
> Family inside jokes, office names, your last holiday: create unlimited custom decks.
>
> MADE FOR GAME NIGHTS
> • Smooth tilt controls using your phone's gyroscope
> • Pauses by itself if you put the phone down, and carries on when it's back on a forehead
> • Fun haptics, sounds and confetti; three music styles
> • 30 s, 60 s, 90 s or 2 min rounds and a best score for every deck
> • Cards don't repeat until you've seen the whole deck
>
> 100% free. No ads. No sign-up. No internet needed. Nothing is collected.

**Search keywords to keep in the description** (Play has no keyword field; it reads the title and description):
charades, forehead game, guess the word, party game, heads up style game, family game, word game, act it out, who am I.
(Writing "heads up" in lowercase as a description of the gameplay is common; never use "Heads Up!" as a name or in the title.)

### Content cautions (avoid rejections)
- Don't call it "Heads Up!" (Warner Bros. trademark) in the title, icon or graphics. "Headzy" is a distinct name.
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
- Cards come from `content/` (universal decks + country packs) and `js/data.js` (India, shared with the website) via `node tools/build-content.js`.
- Sounds are rendered from the web game's synth engine via `node tools/render-sounds.js`.
