/* Bujho Toh! — procedural Indian music & SFX with the Web Audio API.
 * Nothing is downloaded: sitar plucks use Karplus-Strong synthesis, tabla is
 * pitched sines + noise, the tanpura drone is filtered saws, the temple bell is
 * inharmonic partials and the shankh (conch) is a breathy filtered horn.
 */
(function () {
  const A = {
    ctx: null, master: null, musicBus: null, sfxBus: null, reverb: null,
    musicOn: true, sfxOn: true,
    _timer: null, _nextTime: 0, _step: 0, _song: null, _droneNodes: [], _pluckCache: new Map(),
  };

  // Sa = C#4. Bilawal (major) thaat in semitones: S R G M P D N S'
  const SA = 277.18;
  const SCALE = [0, 2, 4, 5, 7, 9, 11, 12, 14, 16];
  const hz = (deg, oct = 0) => {
    const i = ((deg % 7) + 7) % 7;
    const extra = Math.floor(deg / 7);
    return SA * Math.pow(2, (SCALE[i] + 12 * (extra + oct)) / 12);
  };

  A.init = function () {
    if (A.ctx) { if (A.ctx.state === 'suspended') A.ctx.resume(); return; }
    const AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return;
    const ctx = A.ctx = new AC();
    A.master = ctx.createGain(); A.master.gain.value = 0.9;
    const comp = ctx.createDynamicsCompressor();
    comp.threshold.value = -14; comp.ratio.value = 4;
    A.master.connect(comp).connect(ctx.destination);
    A.musicBus = ctx.createGain(); A.musicBus.gain.value = A.musicOn ? 0.55 : 0;
    A.sfxBus = ctx.createGain(); A.sfxBus.gain.value = A.sfxOn ? 1 : 0;
    A.musicBus.connect(A.master); A.sfxBus.connect(A.master);
    // Simple convolution reverb from decaying noise — gives a "temple hall" feel
    A.reverb = ctx.createConvolver();
    const len = ctx.sampleRate * 2.2, ir = ctx.createBuffer(2, len, ctx.sampleRate);
    for (let c = 0; c < 2; c++) {
      const d = ir.getChannelData(c);
      for (let i = 0; i < len; i++) d[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / len, 3);
    }
    A.reverb.buffer = ir;
    const wet = ctx.createGain(); wet.gain.value = 0.22;
    A.reverb.connect(wet).connect(A.master);
  };

  A.setMusic = function (on) {
    A.musicOn = on;
    if (A.musicBus) A.musicBus.gain.setTargetAtTime(on ? 0.55 : 0, A.ctx.currentTime, 0.1);
  };
  A.setSfx = function (on) {
    A.sfxOn = on;
    if (A.sfxBus) A.sfxBus.gain.setTargetAtTime(on ? 1 : 0, A.ctx.currentTime, 0.05);
  };

  function out(node, bus, send = 0.3) {
    node.connect(bus);
    if (send > 0) { const g = A.ctx.createGain(); g.gain.value = send; node.connect(g).connect(A.reverb); }
  }

  let noiseBuf = null;
  function noise() {
    if (!noiseBuf) {
      noiseBuf = A.ctx.createBuffer(1, A.ctx.sampleRate, A.ctx.sampleRate);
      const d = noiseBuf.getChannelData(0);
      for (let i = 0; i < d.length; i++) d[i] = Math.random() * 2 - 1;
    }
    const s = A.ctx.createBufferSource(); s.buffer = noiseBuf; return s;
  }

  /* ---------- Instruments ---------- */

  // Karplus-Strong plucked string with a touch of sitar "jawari" buzz.
  function pluckBuffer(freq, dur) {
    const key = Math.round(freq) + ':' + dur;
    if (A._pluckCache.has(key)) return A._pluckCache.get(key);
    const sr = A.ctx.sampleRate, n = Math.floor(sr * dur);
    const buf = A.ctx.createBuffer(1, n, sr), d = buf.getChannelData(0);
    const period = Math.max(2, Math.round(sr / freq));
    const ring = new Float32Array(period);
    for (let i = 0; i < period; i++) ring[i] = Math.random() * 2 - 1;
    let idx = 0;
    for (let i = 0; i < n; i++) {
      const next = (idx + 1) % period;
      const v = 0.4985 * (ring[idx] + ring[next]);
      ring[idx] = v;
      // jawari: soft asymmetric clip adds the characteristic buzz
      d[i] = v > 0 ? Math.tanh(v * 2.2) * 0.8 : v;
      idx = next;
    }
    A._pluckCache.set(key, buf);
    return buf;
  }

  function sitar(freq, t, vol = 0.35, dur = 1.6, bus = A.musicBus) {
    const src = A.ctx.createBufferSource();
    src.buffer = pluckBuffer(freq, dur);
    // meend (slide) on some notes
    if (Math.random() < 0.15) {
      src.playbackRate.setValueAtTime(0.94, t);
      src.playbackRate.linearRampToValueAtTime(1, t + 0.12);
    }
    const g = A.ctx.createGain(); g.gain.value = vol;
    const hp = A.ctx.createBiquadFilter(); hp.type = 'highpass'; hp.frequency.value = 160;
    src.connect(hp).connect(g); out(g, bus, 0.35);
    src.start(t);
  }

  function tablaDayan(t, pitch = 1, vol = 0.5, bus = A.musicBus) { // "na"/"tin" — right drum
    const o = A.ctx.createOscillator(), g = A.ctx.createGain();
    o.type = 'sine'; o.frequency.setValueAtTime(SA * 2 * pitch, t);
    g.gain.setValueAtTime(0, t); g.gain.linearRampToValueAtTime(vol, t + 0.003);
    g.gain.exponentialRampToValueAtTime(0.001, t + 0.28);
    o.connect(g); out(g, bus, 0.15); o.start(t); o.stop(t + 0.3);
    const o2 = A.ctx.createOscillator(), g2 = A.ctx.createGain();
    o2.type = 'sine'; o2.frequency.value = SA * 2 * pitch * 2.7;
    g2.gain.setValueAtTime(vol * 0.3, t); g2.gain.exponentialRampToValueAtTime(0.001, t + 0.08);
    o2.connect(g2); out(g2, bus, 0); o2.start(t); o2.stop(t + 0.1);
    const n = noise(), bp = A.ctx.createBiquadFilter(), gn = A.ctx.createGain();
    bp.type = 'bandpass'; bp.frequency.value = 3500; bp.Q.value = 1.2;
    gn.gain.setValueAtTime(vol * 0.35, t); gn.gain.exponentialRampToValueAtTime(0.001, t + 0.03);
    n.connect(bp).connect(gn); out(gn, bus, 0); n.start(t); n.stop(t + 0.04);
  }

  function tablaBayan(t, vol = 0.7, bus = A.musicBus) { // "ge" — bass drum with the famous pitch bend
    const o = A.ctx.createOscillator(), g = A.ctx.createGain();
    o.type = 'sine';
    o.frequency.setValueAtTime(70, t);
    o.frequency.linearRampToValueAtTime(115, t + 0.06);
    o.frequency.exponentialRampToValueAtTime(85, t + 0.45);
    g.gain.setValueAtTime(0, t); g.gain.linearRampToValueAtTime(vol, t + 0.005);
    g.gain.exponentialRampToValueAtTime(0.001, t + 0.55);
    o.connect(g); out(g, bus, 0.1); o.start(t); o.stop(t + 0.6);
  }

  function dhol(t, vol = 0.8, bus = A.sfxBus) {
    tablaBayan(t, vol, bus);
    const n = noise(), lp = A.ctx.createBiquadFilter(), g = A.ctx.createGain();
    lp.type = 'lowpass'; lp.frequency.value = 900;
    g.gain.setValueAtTime(vol * 0.6, t); g.gain.exponentialRampToValueAtTime(0.001, t + 0.15);
    n.connect(lp).connect(g); out(g, bus, 0.2); n.start(t); n.stop(t + 0.2);
  }

  function drone(on) {
    A._droneNodes.forEach(n => { try { n.stop(); } catch (e) {} });
    A._droneNodes = [];
    if (!on) return;
    const t = A.ctx.currentTime;
    const freqs = [SA / 2, hz(4, -1), SA / 2 * 1.002, SA];
    const lp = A.ctx.createBiquadFilter(); lp.type = 'lowpass'; lp.frequency.value = 900;
    const g = A.ctx.createGain(); g.gain.setValueAtTime(0, t); g.gain.linearRampToValueAtTime(0.06, t + 2);
    lp.connect(g); out(g, A.musicBus, 0.4);
    freqs.forEach(f => {
      const o = A.ctx.createOscillator(); o.type = 'sawtooth'; o.frequency.value = f;
      const lfo = A.ctx.createOscillator(), lg = A.ctx.createGain();
      lfo.frequency.value = 0.15 + Math.random() * 0.2; lg.gain.value = 1.5;
      lfo.connect(lg).connect(o.detune);
      o.connect(lp); o.start(t); lfo.start(t);
      A._droneNodes.push(o, lfo);
    });
  }

  /* ---------- Songs (step sequencer) ---------- */
  // Keherwa taal: Dha Ge Na Ti | Na Ka Dhi Na  (8 beats, played as 16 half-steps)
  const KEHERWA = ['DHA', '', 'ge', 'NA', 'ti', '', 'NA', 'ka', 'na', '', 'ka', 'DHI', 'na', 'ge', 'NA', ''];
  // Dadra (6) for the menu — lilting & festive
  const DADRA = ['DHA', '', 'DHI', '', 'NA', '', 'DHA', '', 'TU', '', 'NA', ''];

  // Melody phrases (scale degrees; null = rest). Inspired by folk/raag Bilawal shapes.
  const MENU_TUNE = [
    0, null, 2, 4, null, 4, 5, 4, 2, null, 0, null,
    4, null, 5, 7, null, 7, 8, 7, 5, 4, null, null,
    7, null, 5, 4, null, 2, 4, 2, 0, null, -1, null,
    0, 2, 4, 2, 0, null, -3, null, 0, null, null, null,
  ];
  const PLAY_TUNE = [
    0, 2, 4, 2, 4, 5, 4, null, 7, 5, 4, 2, 4, null, 2, null,
    0, 2, 4, 5, 7, null, 8, 7, 5, null, 4, 5, 4, 2, 0, null,
  ];

  const SONGS = {
    menu: { bpm: 100, sub: 2, taal: DADRA, tune: MENU_TUNE, drone: true },
    play: { bpm: 112, sub: 2, taal: KEHERWA, tune: PLAY_TUNE, drone: false },
  };

  function playTaalStroke(s, t, vol) {
    switch (s) {
      case 'DHA': case 'DHI': tablaBayan(t, 0.6 * vol); tablaDayan(t, 1, 0.45 * vol); break;
      case 'ge': tablaBayan(t, 0.45 * vol); break;
      case 'NA': tablaDayan(t, 1, 0.45 * vol); break;
      case 'TU': tablaDayan(t, 0.75, 0.4 * vol); break;
      case 'na': tablaDayan(t, 1, 0.25 * vol); break;
      case 'ti': case 'ka': tablaDayan(t, 1.5, 0.18 * vol); break;
    }
  }

  function scheduler() {
    const song = A._song; if (!song) return;
    const stepDur = 60 / song.bpm / song.sub;
    while (A._nextTime < A.ctx.currentTime + 0.15) {
      const t = A._nextTime, i = A._step;
      const tempoBoost = song.intense ? 1 : 0;
      playTaalStroke(song.taal[i % song.taal.length], t, 1 + tempoBoost * 0.2);
      const note = song.tune[i % song.tune.length];
      if (note !== null && note !== undefined) {
        // light variation each cycle so it doesn't get monotonous
        const cycle = Math.floor(i / song.tune.length);
        const v = (cycle % 2 === 1 && Math.random() < 0.25) ? note + (Math.random() < 0.5 ? 1 : -1) : note;
        sitar(hz(v), t, 0.3);
      }
      A._nextTime += stepDur / (song.intense ? 1.18 : 1);
      A._step++;
    }
  }

  A.playSong = function (name) {
    if (!A.ctx) return;
    if (A._song && A._song.name === name) return;
    A.stopSong();
    const s = SONGS[name]; if (!s) return;
    A._song = Object.assign({ name, intense: false }, s);
    A._step = 0; A._nextTime = A.ctx.currentTime + 0.08;
    drone(!!s.drone);
    A._timer = setInterval(scheduler, 25);
  };
  A.setIntense = function (on) { if (A._song) A._song.intense = on; };
  A.stopSong = function () {
    clearInterval(A._timer); A._timer = null; A._song = null;
    if (A.ctx) drone(false);
  };

  /* ---------- SFX ---------- */
  A.sfx = {
    tap() {
      if (!A.ctx) return; const t = A.ctx.currentTime; tablaDayan(t, 1.3, 0.25, A.sfxBus);
    },
    // Temple bell (ghanti) + rising sitar arpeggio
    correct() {
      if (!A.ctx) return; const t = A.ctx.currentTime;
      [1, 2.76, 5.4, 8.93].forEach((r, i) => {
        const o = A.ctx.createOscillator(), g = A.ctx.createGain();
        o.type = 'sine'; o.frequency.value = 880 * r;
        g.gain.setValueAtTime(0.22 / (i + 1), t); g.gain.exponentialRampToValueAtTime(0.0005, t + 1.2 / (i * 0.5 + 1));
        o.connect(g); out(g, A.sfxBus, 0.5); o.start(t); o.stop(t + 1.3);
      });
      [0, 2, 4, 7].forEach((d, i) => sitar(hz(d, 1), t + i * 0.06, 0.32, 1.2, A.sfxBus));
    },
    // Whoosh down + low tabla
    pass() {
      if (!A.ctx) return; const t = A.ctx.currentTime;
      const n = noise(), bp = A.ctx.createBiquadFilter(), g = A.ctx.createGain();
      bp.type = 'bandpass'; bp.Q.value = 2;
      bp.frequency.setValueAtTime(2500, t); bp.frequency.exponentialRampToValueAtTime(250, t + 0.35);
      g.gain.setValueAtTime(0, t); g.gain.linearRampToValueAtTime(0.5, t + 0.05); g.gain.exponentialRampToValueAtTime(0.001, t + 0.4);
      n.connect(bp).connect(g); out(g, A.sfxBus, 0.2); n.start(t); n.stop(t + 0.45);
      tablaBayan(t + 0.02, 0.6, A.sfxBus);
    },
    tick(urgent) {
      if (!A.ctx) return; const t = A.ctx.currentTime;
      tablaDayan(t, urgent ? 1.6 : 1.25, urgent ? 0.45 : 0.25, A.sfxBus);
    },
    count(n) {
      if (!A.ctx) return; const t = A.ctx.currentTime;
      if (n > 0) { dhol(t, 0.7); tablaDayan(t, 1, 0.4, A.sfxBus); }
      else { dhol(t, 1); dhol(t + 0.12, 0.8); [0, 4, 7, 9].forEach((d, i) => sitar(hz(d, 1), t + i * 0.05, 0.3, 1.2, A.sfxBus)); }
    },
    // Shankh (conch) — time's up
    shankh() {
      if (!A.ctx) return; const t = A.ctx.currentTime;
      const g = A.ctx.createGain(), lp = A.ctx.createBiquadFilter();
      lp.type = 'lowpass'; lp.frequency.setValueAtTime(600, t); lp.frequency.linearRampToValueAtTime(2200, t + 0.6);
      lp.Q.value = 3;
      g.gain.setValueAtTime(0, t); g.gain.linearRampToValueAtTime(0.35, t + 0.35);
      g.gain.setValueAtTime(0.35, t + 1.3); g.gain.exponentialRampToValueAtTime(0.001, t + 2.2);
      [1, 2, 3].forEach((h) => {
        const o = A.ctx.createOscillator(); o.type = 'sawtooth';
        o.frequency.setValueAtTime(SA * 0.5 * h * 0.97, t);
        o.frequency.linearRampToValueAtTime(SA * 0.5 * h, t + 0.4);
        const vib = A.ctx.createOscillator(), vg = A.ctx.createGain();
        vib.frequency.value = 5.5; vg.gain.value = 6; vib.connect(vg).connect(o.detune);
        o.connect(lp); o.start(t); o.stop(t + 2.3); vib.start(t); vib.stop(t + 2.3);
      });
      const n = noise(), bp = A.ctx.createBiquadFilter(), ng = A.ctx.createGain();
      bp.type = 'bandpass'; bp.frequency.value = 1200; ng.gain.value = 0.05;
      n.connect(bp).connect(ng).connect(g); n.start(t); n.stop(t + 2);
      lp.connect(g); out(g, A.sfxBus, 0.6);
    },
    // Shehnai-ish celebratory phrase for results
    fanfare(good) {
      if (!A.ctx) return; const t0 = A.ctx.currentTime + 0.1;
      const phrase = good ? [0, 2, 4, 5, 7, 7, 9, 7, 8] : [4, 2, 0, -1, 0];
      const durs = good ? [0.14, 0.14, 0.14, 0.14, 0.3, 0.14, 0.14, 0.14, 0.7] : [0.25, 0.25, 0.25, 0.25, 0.8];
      let t = t0;
      const g = A.ctx.createGain(); g.gain.value = 0.16;
      const lp = A.ctx.createBiquadFilter(); lp.type = 'bandpass'; lp.frequency.value = 1400; lp.Q.value = 0.8;
      const o = A.ctx.createOscillator(); o.type = 'square';
      const vib = A.ctx.createOscillator(), vg = A.ctx.createGain(); vib.frequency.value = 6; vg.gain.value = 18;
      vib.connect(vg).connect(o.detune);
      const env = A.ctx.createGain(); env.gain.setValueAtTime(0, t);
      phrase.forEach((d, i) => {
        const f = hz(d, 1);
        o.frequency.setTargetAtTime(f, t, 0.02);
        env.gain.setTargetAtTime(1, t, 0.02);
        env.gain.setTargetAtTime(0.6, t + durs[i] * 0.7, 0.03);
        t += durs[i];
      });
      env.gain.setTargetAtTime(0, t, 0.08);
      o.connect(lp).connect(env).connect(g); out(g, A.sfxBus, 0.5);
      o.start(t0); vib.start(t0); o.stop(t + 0.6); vib.stop(t + 0.6);
      if (good) { dhol(t0, 0.8); dhol(t0 + 0.28, 0.6); dhol(t0 + 0.56, 0.9); }
    },
  };

  window.Audio2 = A;
})();
