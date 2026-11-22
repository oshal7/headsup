#!/usr/bin/env node
/*
 * Renders Android launcher icons and Play Store graphics.
 *   node tools/render-icons.js
 * Output:
 *   android/app/src/main/res/mipmap-*            legacy + adaptive icon layers (PNG)
 *   android/store/icon-512.png                    Play Store app icon (full square)
 *   android/store/feature-graphic-1024x500.png    Play Store feature graphic
 */
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const RES = path.join(ROOT, 'android/app/src/main/res');
const STORE = path.join(ROOT, 'android/store');

const DEFS = `
<defs>
  <radialGradient id="bg" cx="50%" cy="30%" r="80%"><stop offset="0" stop-color="#7b2ff7"/><stop offset=".6" stop-color="#2a0845"/><stop offset="1" stop-color="#12001f"/></radialGradient>
  <linearGradient id="ph" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#ff3d6e"/><stop offset="1" stop-color="#ff9a3d"/></linearGradient>
  <linearGradient id="fl" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fff6c2"/><stop offset=".5" stop-color="#ffd200"/><stop offset="1" stop-color="#ff6a00"/></linearGradient>
</defs>`;

// The artwork (phone with "?!" + diya), drawn in a 512 box, centred.
const art = (mono) => `
  <rect x="96" y="118" width="320" height="176" rx="30" fill="${mono ? '#fff' : 'url(#ph)'}" stroke="${mono ? 'none' : '#1b0030'}" stroke-width="12"/>
  <text x="256" y="238" font-family="Arial Black, Arial, sans-serif" font-size="120" font-weight="900" text-anchor="middle" fill="${mono ? '#000' : '#fff'}">?!</text>
  <path d="M256 330 C 236 360 240 380 256 392 C 272 380 276 360 256 330Z" fill="${mono ? '#fff' : 'url(#fl)'}"/>
  <path d="M150 400 Q256 470 362 400 Q340 440 256 446 Q172 440 150 400Z" fill="${mono ? '#fff' : '#ff9a3d'}" stroke="${mono ? 'none' : '#b3002d'}" stroke-width="6"/>`;

const ring = `<circle cx="256" cy="256" r="210" fill="none" stroke="#ffd200" stroke-opacity=".22" stroke-width="6" stroke-dasharray="6 16"/>`;

// Full icon (legacy launchers, store icon). `round` clips to a circle, `rx` rounds corners.
const fullIcon = ({ rx = 0, round = false } = {}) => `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">${DEFS}
  ${round ? '<clipPath id="c"><circle cx="256" cy="256" r="256"/></clipPath><g clip-path="url(#c)">' : '<g>'}
  <rect width="512" height="512" rx="${rx}" fill="url(#bg)"/>${ring}
  <g transform="translate(256 270) scale(.92) translate(-256 -270)">${art(false)}</g></g>
</svg>`;

// Adaptive icon layers are 108dp with a 66dp safe zone: scale art into the centre 61%.
const layer = (inner) => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">${DEFS}
  <g transform="translate(256 262) scale(.6) translate(-256 -278)">${inner}</g></svg>`;
const adaptiveBg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">${DEFS}<rect width="512" height="512" fill="url(#bg)"/>${ring}</svg>`;

const DENSITIES = { mdpi: 1, hdpi: 1.5, xhdpi: 2, xxhdpi: 3, xxxhdpi: 4 };

const FEATURE = `<!doctype html><html><head>
<link href="https://fonts.googleapis.com/css2?family=Baloo+2:wght@800&family=Tiro+Devanagari+Hindi&display=block" rel="stylesheet">
<style>
 body{margin:0;width:1024px;height:500px;overflow:hidden;font-family:'Baloo 2',sans-serif;
   background:radial-gradient(120% 90% at 30% 0%,#7b2ff7 0%,#2a0845 55%,#12001f 100%);color:#fff;position:relative}
 .r{position:absolute;opacity:.18}
 .t{position:absolute;left:70px;top:80px}
 .hi{font-family:'Tiro Devanagari Hindi',serif;color:#ffd200;font-size:40px}
 h1{margin:0;font-size:112px;line-height:.9;background:linear-gradient(180deg,#fff6c2,#ffd200 45%,#ff9a3d);-webkit-background-clip:text;color:transparent;
   filter:drop-shadow(0 5px 0 #b3002d)}
 h1 em{font-style:normal;background:linear-gradient(180deg,#ffd1e0,#ff3d6e);-webkit-background-clip:text}
 p{font-size:30px;margin:16px 0 0;color:#ffe3ef}
 .phone{position:absolute;right:56px;top:110px;width:280px;height:165px;border-radius:30px;border:10px solid #1b0030;
   background:linear-gradient(135deg,#ff3d6e,#ff9a3d);display:grid;place-items:center;font-size:58px;font-weight:800;transform:rotate(-8deg);
   box-shadow:0 20px 50px rgba(0,0,0,.45);text-align:center;line-height:1}
 .tag{position:absolute;left:70px;top:395px;display:flex;gap:12px;font-size:24px}
 .tag span{background:rgba(255,255,255,.14);padding:4px 16px;border-radius:99px}
</style></head><body>
 <svg class="r" style="width:600px;height:600px;right:-200px;top:-230px" viewBox="0 0 200 200"><g fill="#ffd200">${
   Array.from({ length: 8 }, (_, i) => `<path transform="rotate(${i * 45} 100 100)" d="M100 100 C 85 70, 92 30, 100 12 C 108 30, 115 70, 100 100Z"/>`).join('')}</g></svg>
 <div class="t"><div class="hi">बूझो तो!</div><h1>Bujho <em>Toh!</em></h1><p>The desi forehead guessing game</p></div>
 <div class="phone">Sholay</div>
 <div class="tag"><span>🎬 Bollywood</span><span>🪔 Festivals</span><span>🍛 Food</span></div>
</body></html>`;

async function main() {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  const shot = async (svg, size, file, transparent = true) => {
    await page.setViewportSize({ width: size, height: size });
    await page.setContent(`<style>html,body{margin:0;background:transparent}</style><img src="data:image/svg+xml;base64,${Buffer.from(svg).toString('base64')}" width="${size}" height="${size}">`);
    await page.waitForTimeout(50);
    fs.mkdirSync(path.dirname(file), { recursive: true });
    await page.screenshot({ path: file, omitBackground: transparent });
  };

  for (const [d, m] of Object.entries(DENSITIES)) {
    const dir = path.join(RES, `mipmap-${d}`);
    await shot(fullIcon({ rx: 112 }), Math.round(48 * m), path.join(dir, 'ic_launcher.png'));
    await shot(fullIcon({ round: true }), Math.round(48 * m), path.join(dir, 'ic_launcher_round.png'));
    await shot(layer(art(false)), Math.round(108 * m), path.join(dir, 'ic_launcher_foreground.png'));
    await shot(adaptiveBg, Math.round(108 * m), path.join(dir, 'ic_launcher_background.png'), false);
    await shot(layer(art(true)), Math.round(108 * m), path.join(dir, 'ic_launcher_monochrome.png'));
  }
  const adaptive = `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
    <monochrome android:drawable="@mipmap/ic_launcher_monochrome" />
</adaptive-icon>
`;
  const any = path.join(RES, 'mipmap-anydpi-v26');
  fs.mkdirSync(any, { recursive: true });
  fs.writeFileSync(path.join(any, 'ic_launcher.xml'), adaptive);
  fs.writeFileSync(path.join(any, 'ic_launcher_round.xml'), adaptive);

  await shot(fullIcon(), 512, path.join(STORE, 'icon-512.png'), false);
  await page.setViewportSize({ width: 1024, height: 500 });
  await page.setContent(FEATURE, { waitUntil: 'networkidle' });
  await page.waitForTimeout(500);
  await page.screenshot({ path: path.join(STORE, 'feature-graphic-1024x500.png') });
  await browser.close();
  console.log('icons + store graphics rendered');
}
main().catch((e) => { console.error(e); process.exit(1); });
