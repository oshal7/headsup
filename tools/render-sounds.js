#!/usr/bin/env node
/*
 * Renders the game's sounds and music (from js/audio.js, the same engine the web
 * game uses) into OGG files for the Android app.
 *
 *   node tools/render-sounds.js
 *
 * Needs: Playwright (Chromium) and ffmpeg with libvorbis.
 * Output: android/app/src/main/res/raw/*.ogg
 *
 * Music loops are rendered twice back-to-back and only the second pass is kept,
 * so reverb tails and held notes wrap around and the loop is seamless.
 */
const { chromium } = require('playwright');
const { execFileSync } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const OUT = path.join(ROOT, 'android/app/src/main/res/raw');
const SR = 44100;

// name -> { kind: 'sfx', call: JS expression run at t=0, seconds }
const SFX = {
  sfx_tap: { call: 'Audio2.sfx.tap()', seconds: 0.4 },
  sfx_correct: { call: 'Audio2.sfx.correct()', seconds: 1.4 },
  sfx_pass: { call: 'Audio2.sfx.pass()', seconds: 0.8 },
  sfx_tick: { call: 'Audio2.sfx.tick(false)', seconds: 0.35 },
  sfx_tick_urgent: { call: 'Audio2.sfx.tick(true)', seconds: 0.35 },
  sfx_count: { call: 'Audio2.sfx.count(3)', seconds: 0.8 },
  sfx_go: { call: 'Audio2.sfx.count(0)', seconds: 1.4 },
  sfx_timeup: { call: 'Audio2.sfx.shankh()', seconds: 2.6 },
  sfx_win: { call: 'Audio2.sfx.fanfare(true)', seconds: 2.8 },
  sfx_lose: { call: 'Audio2.sfx.fanfare(false)', seconds: 2.4 },
};

// name -> { style, role, steps } ; one musical loop = `steps` sequencer steps (12 per bar)
const MUSIC = {
  music_chill: { style: 'chill', role: 'menu', steps: 192 },
  music_chill_play: { style: 'chill', role: 'play', steps: 192 },
  music_classic: { style: 'classic', role: 'menu', steps: 192 },
  music_party: { style: 'party', role: 'play', steps: 96 },
};

async function main() {
  fs.mkdirSync(OUT, { recursive: true });
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'bujho-snd-'));
  const browser = await chromium.launch();
  const page = await browser.newPage();
  await page.setContent('<!doctype html><title>render</title>');
  // Offline rendering: replace AudioContext with an OfflineAudioContext whose clock we drive.
  await page.evaluate((SR) => {
    window.__t = 0;
    window.AudioContext = function () {
      const c = new OfflineAudioContext(2, SR * 120, SR);
      Object.defineProperty(c, 'currentTime', { get: () => window.__t });
      c.resume = () => Promise.resolve();
      window.__ctx = c;
      return c;
    };
  }, SR);
  await page.addScriptTag({ path: path.join(ROOT, 'js/audio.js') });

  const render = (job) => page.evaluate(async ({ job, SR }) => {
    const A = window.Audio2;
    A.ctx = null; A._song = null; window.__t = 0;
    A.init();
    let seconds, start = 0;
    if (job.call) {
      eval(job.call);
      seconds = job.seconds;
    } else {
      A.style = job.style; A.playSong(job.role);
      const stepDur = 60 / A._song.bpm / 3;
      const loopLen = job.steps * stepDur;
      // advance the clock so the scheduler queues two full loops
      for (let t = 0; t < loopLen * 2 + 0.3; t += 0.05) { window.__t = t; await new Promise(r => setTimeout(r, 0)); }
      A.stopSong();
      start = 0.08 + loopLen; seconds = loopLen;
    }
    const buf = await window.__ctx.startRendering();
    const from = Math.round(start * SR), n = Math.round(seconds * SR);
    const ch = [buf.getChannelData(0).slice(from, from + n), buf.getChannelData(1).slice(from, from + n)];
    // 16-bit stereo WAV
    const dv = new DataView(new ArrayBuffer(44 + n * 4));
    const w = (o, s) => [...s].forEach((c, i) => dv.setUint8(o + i, c.charCodeAt(0)));
    w(0, 'RIFF'); dv.setUint32(4, 36 + n * 4, true); w(8, 'WAVEfmt '); dv.setUint32(16, 16, true);
    dv.setUint16(20, 1, true); dv.setUint16(22, 2, true); dv.setUint32(24, SR, true); dv.setUint32(28, SR * 4, true);
    dv.setUint16(32, 4, true); dv.setUint16(34, 16, true); w(36, 'data'); dv.setUint32(40, n * 4, true);
    let peak = 0;
    for (let i = 0; i < n; i++) for (let c = 0; c < 2; c++) {
      const v = Math.max(-1, Math.min(1, ch[c][i] || 0)); peak = Math.max(peak, Math.abs(v));
      dv.setInt16(44 + (i * 2 + c) * 2, v * 32767, true);
    }
    const bytes = new Uint8Array(dv.buffer);
    let bin = '';
    for (let i = 0; i < bytes.length; i += 0x8000) bin += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
    return { peak, b64: btoa(bin) };
  }, { job, SR });

  for (const [name, job] of [...Object.entries(SFX), ...Object.entries(MUSIC)]) {
    const r = await render(job);
    const wav = path.join(tmp, name + '.wav');
    fs.writeFileSync(wav, Buffer.from(r.b64, 'base64'));
    const out = path.join(OUT, name + '.ogg');
    // Normalise every file to a 0.9 peak; the app sets each sound's relative loudness.
    const gain = r.peak > 0 ? (0.9 / r.peak).toFixed(3) : '1';
    execFileSync('ffmpeg', ['-y', '-loglevel', 'error', '-i', wav, '-af', `volume=${gain}`, '-c:a', 'libvorbis', '-q:a', job.call ? '5' : '4', out]);
    console.log(`${name.padEnd(18)} peak ${r.peak.toFixed(2)}  ${(fs.statSync(out).size / 1024).toFixed(0)} KB`);
  }
  await browser.close();
  fs.rmSync(tmp, { recursive: true, force: true });
}

main().catch((e) => { console.error(e); process.exit(1); });
