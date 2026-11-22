#!/usr/bin/env node
/*
 * Renders Android launcher icons and Play Store graphics.
 *   node tools/render-icons.js
 * Output:
 *   android/app/src/main/res/mipmap-*            legacy + adaptive icon layers (PNG)
 *   android/store/icon-512.png                    Play Store app icon (full square)
 *   android/store/feature-graphic-1024x500.png    Play Store feature graphic
 *   android/app/src/main/res/drawable-nodpi/mascot.png   the mascot used inside the app
 * Artwork: tools/art/mascot.js (the Headzy buddy) and tools/art/feature.html.
 */
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const RES = path.join(ROOT, 'android/app/src/main/res');
const STORE = path.join(ROOT, 'android/store');

const { DEFS: MASCOT_DEFS, art } = require('./art/mascot.js');
const DEFS = `<defs>${MASCOT_DEFS}</defs>`;

// Full icon (legacy launchers, store icon). `round` clips to a circle, `rx` rounds corners.
const fullIcon = ({ rx = 0, round = false } = {}) => `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">${DEFS}
  ${round ? '<clipPath id="c"><circle cx="256" cy="256" r="256"/></clipPath><g clip-path="url(#c)">' : '<g>'}
  <rect width="512" height="512" rx="${rx}" fill="url(#hzBg)"/>
  <g transform="translate(256 262) scale(${round ? 0.86 : 0.94}) translate(-256 -262)">${art(false)}</g></g>
</svg>`;

// Adaptive icon layers are 108dp with a 66dp safe zone: keep the art inside the centre ~64%.
const layer = (inner) => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">${DEFS}
  <g transform="translate(256 258) scale(.74) translate(-256 -262)">${inner}</g></svg>`;
const adaptiveBg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">${DEFS}<rect width="512" height="512" fill="url(#hzBg)"/></svg>`;
// Mascot without background, for the app's home screen and splash.
const mascot = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">${DEFS}${art(false)}</svg>`;

const DENSITIES = { mdpi: 1, hdpi: 1.5, xhdpi: 2, xxhdpi: 3, xxxhdpi: 4 };

const FEATURE = fs.readFileSync(path.join(__dirname, 'art/feature.html'), 'utf8').replace('<!--MASCOT-->', mascot);

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
  await shot(mascot, 480, path.join(RES, 'drawable-nodpi', 'mascot.png'));
  await page.setViewportSize({ width: 1024, height: 500 });
  fs.writeFileSync(path.join(__dirname, 'art', '_feature.html'), FEATURE);
  await page.goto('file://' + path.join(__dirname, 'art', '_feature.html'));
  await page.waitForTimeout(500);
  await page.screenshot({ path: path.join(STORE, 'feature-graphic-1024x500.png') });
  fs.unlinkSync(path.join(__dirname, 'art', '_feature.html'));
  await browser.close();
  console.log('icons + store graphics rendered');
}
main().catch((e) => { console.error(e); process.exit(1); });
