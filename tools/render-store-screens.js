#!/usr/bin/env node
/*
 * Play Store phone screenshots (1080x1920) with captions, built from real app captures.
 *   node tools/render-store-screens.js
 * Input:  android/store/raw/*.png  (captures of the app; portrait 9:16 and landscape 16:9)
 * Output: android/store/screenshots/01-…png … 06-…png
 */
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const ROOT = path.resolve(__dirname, '..');
const RAW = path.join(ROOT, 'android/store/raw');
const OUT = path.join(ROOT, 'android/store/screenshots');
const { DEFS, art } = require('./art/mascot.js');
const mascot = `<svg viewBox="0 0 512 512"><defs>${DEFS}</defs>${art(false)}</svg>`;
const font = (f) => 'file://' + path.join(ROOT, 'android/app/src/main/res/font', f);

const SHOTS = [
  { file: '01-home', raw: 'home', land: false, title: '30+ decks,<br>3,000+ cards', sub: 'Party classics, charades, movies, kids & more', bg: ['#8a3cff', '#3a0d78'] },
  { file: '02-forehead', raw: 'play', land: true, title: 'Phone on your<br>forehead!', sub: 'Your friends shout clues. You guess the word.', bg: ['#ff4f8b', '#7a1035'], mascot: true },
  { file: '03-correct', raw: 'correct', land: true, title: 'Got it?<br>Tilt down', sub: 'Nod the phone down for a point', bg: ['#14c46a', '#0a5a33'] },
  { file: '04-pass', raw: 'pass', land: true, title: 'Stuck?<br>Tilt up to pass', sub: 'Tip the phone back to skip', bg: ['#ff8a3d', '#8a2f00'] },
  { file: '05-countries', raw: 'movies-sheet', land: false, title: 'Movies, songs & TV<br>from your country', sub: 'India, USA, UK, China and the whole world', bg: ['#3f5efb', '#1a0638'] },
  { file: '06-act', raw: 'sounds-sheet', land: false, title: 'Act it, hum it,<br>make the sound!', sub: 'Charades, emotions, dance moves and noises', bg: ['#00b09b', '#0b3d3a'] },
];

const page = (s, img) => `<!doctype html><html><head><meta charset="utf-8"><style>
 @font-face{font-family:Baloo;src:url(${font('baloo2_extrabold.ttf')});font-weight:800}
 @font-face{font-family:Baloo;src:url(${font('baloo2_bold.ttf')});font-weight:700}
 body{margin:0;width:1080px;height:1920px;overflow:hidden;font-family:Baloo,sans-serif;color:#fff;position:relative;
   background:radial-gradient(120% 80% at 50% 0%,${s.bg[0]} 0%,${s.bg[1]} 70%,#12001f 100%)}
 .rays{position:absolute;left:-460px;top:-1000px;width:2000px;height:2000px;opacity:.10;border-radius:50%;
   background:repeating-conic-gradient(#fff 0 5deg,transparent 5deg 15deg)}
 h1{position:absolute;top:110px;left:60px;right:60px;margin:0;text-align:center;font-size:104px;line-height:.98;font-weight:800;
   text-shadow:0 8px 0 rgba(0,0,0,.25)}
 p{position:absolute;top:${s.land ? 360 : 345}px;left:80px;right:80px;margin:0;text-align:center;font-size:44px;font-weight:700;opacity:.92}
 .phone{position:absolute;background:#0d0220;border-radius:70px;padding:22px;box-shadow:0 40px 80px rgba(0,0,0,.5),inset 0 0 0 4px rgba(255,255,255,.15)}
 .phone img{display:block;border-radius:50px;width:100%;height:100%}
 .port{left:170px;top:470px;width:696px;height:1237px}
 .land{left:40px;top:${s.mascot ? 760 : 620}px;width:956px;height:538px;transform:rotate(${s.mascot ? -4 : 0}deg)}
 .m{position:absolute;width:420px;height:420px;left:330px;top:1310px;filter:drop-shadow(0 20px 30px rgba(0,0,0,.4))}
 .m svg{width:100%;height:100%}
 .big{position:absolute;left:50%;transform:translateX(-50%);top:1290px;font-size:230px;font-weight:800;line-height:1}
</style></head><body><div class="rays"></div>
 <h1>${s.title}</h1><p>${s.sub}</p>
 <div class="phone ${s.land ? 'land' : 'port'}"><img src="${img}"></div>
 ${s.land ? `<div class="m">${mascot}</div>` : ''}
</body></html>`;

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const browser = await chromium.launch();
  const p = await browser.newPage({ viewport: { width: 1080, height: 1920 } });
  for (const s of SHOTS) {
    const img = path.join(RAW, `${s.raw}.png`);
    if (!fs.existsSync(img)) { console.warn('missing', img); continue; }
    const html = path.join(RAW, '_page.html');
    fs.writeFileSync(html, page(s, 'file://' + img));
    await p.goto('file://' + html);
    await p.waitForTimeout(200);
    await p.screenshot({ path: path.join(OUT, `${s.file}.png`) });
    fs.unlinkSync(html);
  }
  await browser.close();
  console.log('store screenshots ->', path.relative(ROOT, OUT));
})().catch((e) => { console.error(e); process.exit(1); });
