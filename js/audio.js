/* Bujho Toh! — procedural Indian music & SFX with the Web Audio API.
 * Nothing is downloaded. Music is a Bollywood/Punjabi-style groove: harmonium
 * chords, bansuri or reed lead (with meend glides & kan grace notes), dhol,
 * dholak, tumbi and claps in the Khamaj scale. SFX: tabla taps, temple bell,
 * shankh (conch) and a shehnai fanfare.
 */
(function () {
  const A = {
    ctx: null, master: null, musicBus: null, sfxBus: null, reverb: null,
    musicOn: true, sfxOn: true,
    _timer: null, _nextTime: 0, _step: 0, _song: null, 
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
    A.musicBus = ctx.createGain(); A.musicBus.gain.value = A.musicOn ? 0.45 : 0;
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
    if (A.musicBus) A.musicBus.gain.setTargetAtTime(on ? 0.45 : 0, A.ctx.currentTime, 0.1);
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

  /* ---------- Bollywood / Punjabi style instruments ---------- */
  // Mixolydian = Bilawal with komal Ni (Khamaj flavour), the sound of Punjabi folk & Bollywood dance songs.
  // Degrees: 0 Sa, 1 Re, 2 Ga, 3 Ma, 4 Pa, 5 Dha, 6 komal Ni, 7 Sa'
  const KHAMAJ = [0, 2, 4, 5, 7, 9, 10];
  const kz = (deg, oct = 0) => {
    const i = ((deg % 7) + 7) % 7, extra = Math.floor(deg / 7);
    return SA * Math.pow(2, (KHAMAJ[i] + 12 * (extra + oct)) / 12);
  };

  // Harmonium: detuned reeds, a bit nasal, with bellows tremolo.
  function harmonium(freqs, t, dur, vol = 0.07, bus = A.musicBus) {
    const g = A.ctx.createGain(), lp = A.ctx.createBiquadFilter();
    lp.type = 'lowpass'; lp.frequency.value = 2200; lp.Q.value = 0.7;
    g.gain.setValueAtTime(0, t);
    g.gain.linearRampToValueAtTime(vol, t + 0.04);
    g.gain.setValueAtTime(vol, t + dur - 0.06);
    g.gain.linearRampToValueAtTime(0, t + dur);
    const trem = A.ctx.createOscillator(), tg = A.ctx.createGain();
    trem.frequency.value = 4.5; tg.gain.value = vol * 0.15;
    trem.connect(tg).connect(g.gain);
    lp.connect(g); out(g, bus, 0.15);
    freqs.forEach(f => {
      [-6, 6].forEach(det => {
        const o = A.ctx.createOscillator(); o.type = 'sawtooth'; o.frequency.value = f; o.detune.value = det;
        o.connect(lp); o.start(t); o.stop(t + dur + 0.02);
      });
      const sq = A.ctx.createOscillator(); sq.type = 'square'; sq.frequency.value = f;
      const sg = A.ctx.createGain(); sg.gain.value = 0.4; sq.connect(sg).connect(lp); sq.start(t); sq.stop(t + dur + 0.02);
    });
    trem.start(t); trem.stop(t + dur + 0.02);
  }

  // Monophonic lead voice with meend (glides) and kan (grace notes).
  // kind: 'bansuri' (breathy flute) or 'reed' (harmonium/shehnai-ish lead)
  function makeLead(kind) {
    const ctx = A.ctx;
    const o = ctx.createOscillator(); o.type = kind === 'bansuri' ? 'triangle' : 'sawtooth';
    const o2 = ctx.createOscillator(); o2.type = kind === 'bansuri' ? 'sine' : 'square'; o2.detune.value = kind === 'bansuri' ? 1200 : 5;
    const g2 = ctx.createGain(); g2.gain.value = kind === 'bansuri' ? 0.12 : 0.35;
    const f = ctx.createBiquadFilter(); f.type = 'lowpass'; f.frequency.value = kind === 'bansuri' ? 2600 : 1900; f.Q.value = 1;
    const env = ctx.createGain(); env.gain.value = 0;
    const vib = ctx.createOscillator(), vg = ctx.createGain(); vib.frequency.value = 5.2; vg.gain.value = 0;
    vib.connect(vg); vg.connect(o.detune); vg.connect(o2.detune);
    o.connect(f); o2.connect(g2).connect(f); f.connect(env);
    // breath noise for bansuri
    let n = null;
    if (kind === 'bansuri') {
      n = noise(); n.loop = true;
      const bp = ctx.createBiquadFilter(); bp.type = 'bandpass'; bp.frequency.value = 1800; bp.Q.value = 0.8;
      const ng = ctx.createGain(); ng.gain.value = 0.05; n.connect(bp).connect(ng).connect(env); n.start();
    }
    const master = ctx.createGain(); master.gain.value = kind === 'bansuri' ? 0.3 : 0.1;
    env.connect(master); out(master, A.musicBus, kind === 'bansuri' ? 0.45 : 0.25);
    o.start(); o2.start(); vib.start();
    return {
      note(freq, t, dur, opts = {}) {
        const fr = o.frequency, fr2 = o2.frequency;
        if (opts.kan) { // quick grace note from above
          fr.setValueAtTime(opts.kan, t); fr2.setValueAtTime(opts.kan, t);
          fr.setTargetAtTime(freq, t + 0.05, 0.015); fr2.setTargetAtTime(freq, t + 0.05, 0.015);
        } else if (opts.glide) { // meend: slide into the note
          fr.setTargetAtTime(freq, t, 0.04); fr2.setTargetAtTime(freq, t, 0.04);
        } else { fr.setValueAtTime(freq, t); fr2.setValueAtTime(freq, t); }
        env.gain.cancelScheduledValues(t);
        env.gain.setTargetAtTime(1, t, 0.015);
        env.gain.setTargetAtTime(0.75, t + 0.08, 0.1);
        env.gain.setTargetAtTime(0, t + dur * 0.92, 0.03);
        // vibrato blooms on long notes, like a real singer/flautist
        vg.gain.cancelScheduledValues(t); vg.gain.setValueAtTime(0, t);
        if (dur > 0.35) vg.gain.linearRampToValueAtTime(kind === 'bansuri' ? 18 : 12, t + Math.min(dur, 0.8));
      },
      stop() { try { o.stop(); o2.stop(); vib.stop(); n && n.stop(); } catch (e) {} env.disconnect(); },
    };
  }

  // Tumbi: tiny bright pluck, very short (the Punjabi one-string "tung tung").
  function tumbi(freq, t, vol = 0.16) {
    const o = A.ctx.createOscillator(), g = A.ctx.createGain(), f = A.ctx.createBiquadFilter();
    o.type = 'square'; o.frequency.setValueAtTime(freq * 1.02, t); o.frequency.exponentialRampToValueAtTime(freq, t + 0.03);
    f.type = 'bandpass'; f.frequency.value = freq * 3; f.Q.value = 3;
    g.gain.setValueAtTime(vol, t); g.gain.exponentialRampToValueAtTime(0.001, t + 0.18);
    o.connect(f).connect(g); out(g, A.musicBus, 0.1); o.start(t); o.stop(t + 0.2);
  }

  // Dhol: heavy "dagga" bass side and sharp "tilli" stick side.
  function dholBass(t, vol = 0.9) {
    const o = A.ctx.createOscillator(), g = A.ctx.createGain();
    o.type = 'sine'; o.frequency.setValueAtTime(110, t); o.frequency.exponentialRampToValueAtTime(52, t + 0.18);
    g.gain.setValueAtTime(vol, t); g.gain.exponentialRampToValueAtTime(0.001, t + 0.4);
    o.connect(g); out(g, A.musicBus, 0.05); o.start(t); o.stop(t + 0.42);
    const n = noise(), lp = A.ctx.createBiquadFilter(), ng = A.ctx.createGain();
    lp.type = 'lowpass'; lp.frequency.value = 700;
    ng.gain.setValueAtTime(vol * 0.5, t); ng.gain.exponentialRampToValueAtTime(0.001, t + 0.07);
    n.connect(lp).connect(ng); out(ng, A.musicBus, 0); n.start(t); n.stop(t + 0.08);
  }
  function dholTilli(t, vol = 0.35) {
    const n = noise(), hp = A.ctx.createBiquadFilter(), g = A.ctx.createGain();
    hp.type = 'bandpass'; hp.frequency.value = 2600; hp.Q.value = 1.4;
    g.gain.setValueAtTime(vol, t); g.gain.exponentialRampToValueAtTime(0.001, t + 0.06);
    n.connect(hp).connect(g); out(g, A.musicBus, 0.1); n.start(t); n.stop(t + 0.07);
    const o = A.ctx.createOscillator(), og = A.ctx.createGain();
    o.type = 'triangle'; o.frequency.value = 420;
    og.gain.setValueAtTime(vol * 0.6, t); og.gain.exponentialRampToValueAtTime(0.001, t + 0.08);
    o.connect(og); out(og, A.musicBus, 0); o.start(t); o.stop(t + 0.09);
  }
  function clap(t, vol = 0.25) {
    [0, 0.012, 0.024].forEach(d => {
      const n = noise(), bp = A.ctx.createBiquadFilter(), g = A.ctx.createGain();
      bp.type = 'bandpass'; bp.frequency.value = 1400; bp.Q.value = 0.9;
      g.gain.setValueAtTime(vol, t + d); g.gain.exponentialRampToValueAtTime(0.001, t + d + 0.09);
      n.connect(bp).connect(g); out(g, A.musicBus, 0.2); n.start(t + d); n.stop(t + d + 0.1);
    });
  }
  function bass(freq, t, dur, vol = 0.28) {
    const o = A.ctx.createOscillator(), g = A.ctx.createGain(), lp = A.ctx.createBiquadFilter();
    o.type = 'triangle'; o.frequency.value = freq; lp.type = 'lowpass'; lp.frequency.value = 400;
    g.gain.setValueAtTime(vol, t); g.gain.exponentialRampToValueAtTime(0.01, t + dur);
    o.connect(lp).connect(g); out(g, A.musicBus, 0); o.start(t); o.stop(t + dur + 0.02);
  }

  /* ---------- Songs ---------- */
  // Bars are 12 steps (4 beats × triplets) for the bouncy bhangra/dholak swing.
  // Chord loop: Sa – komal Ni – Ma – Sa  (I – bVII – IV – I), the classic Punjabi/Bollywood party loop.
  const CHORDS = [[0, 2, 4], [-1, 1, 3], [3, 5, 7], [0, 2, 4]]; // degree triads
  const ROOTS = [0, -1, 3, 0];

  // Melodies: [step in 4-bar loop (0..47), degree, length in steps]
  const PLAY_MELODY = [
    [0, 7, 2], [3, 6, 1], [4, 5, 2], [6, 4, 3], [9, 2, 3],
    [12, 4, 2], [15, 5, 1], [16, 6, 2], [18, 7, 6],
    [24, 8, 2], [27, 7, 1], [28, 6, 2], [30, 5, 2], [33, 4, 3],
    [36, 2, 2], [39, 3, 1], [40, 2, 2], [42, 1, 3], [45, 0, 3],
  ];
  const PLAY_MELODY_B = [
    [0, 4, 3], [3, 4, 1], [4, 5, 2], [6, 7, 6],
    [12, 6, 3], [15, 5, 1], [16, 6, 2], [18, 4, 6],
    [24, 3, 3], [27, 4, 1], [28, 5, 2], [30, 4, 2], [33, 2, 3],
    [36, 1, 2], [39, 2, 1], [40, 1, 2], [42, -1, 3], [45, 0, 3],
  ];
  const MENU_MELODY = [
    [0, 4, 6], [6, 5, 3], [9, 4, 3],
    [12, 2, 6], [18, 1, 3], [21, 2, 3],
    [24, 3, 3], [27, 4, 3], [30, 5, 3], [33, 6, 3],
    [36, 7, 9], [45, 6, 3],
  ];
  const MENU_MELODY_B = [
    [0, 7, 3], [3, 6, 3], [6, 5, 3], [9, 4, 3],
    [12, 6, 6], [18, 5, 3], [21, 4, 3],
    [24, 3, 6], [30, 2, 3], [33, 1, 3],
    [36, 0, 12],
  ];

  /* ---------- Chill Chai: soft & simple ---------- */
  // Soft electric-piano (Rhodes-like) note.
  function epiano(freq, t, dur, vol = 0.09) {
    const g = A.ctx.createGain(), lp = A.ctx.createBiquadFilter();
    lp.type = 'lowpass'; lp.frequency.value = 2600;
    g.gain.setValueAtTime(0, t);
    g.gain.linearRampToValueAtTime(vol, t + 0.006);
    g.gain.exponentialRampToValueAtTime(vol * 0.35, t + 0.4);
    g.gain.exponentialRampToValueAtTime(0.0008, t + Math.max(0.6, dur));
    lp.connect(g); out(g, A.musicBus, 0.4);
    [[1, 1], [2, 0.22], [3.01, 0.06]].forEach(([m, a]) => {
      const o = A.ctx.createOscillator(), og = A.ctx.createGain();
      o.type = 'sine'; o.frequency.value = freq * m; og.gain.value = a;
      o.connect(og).connect(lp); o.start(t); o.stop(t + Math.max(0.6, dur) + 0.05);
    });
  }
  function shaker(t, vol = 0.03) {
    const n = noise(), hp = A.ctx.createBiquadFilter(), g = A.ctx.createGain();
    hp.type = 'highpass'; hp.frequency.value = 6000;
    g.gain.setValueAtTime(0, t); g.gain.linearRampToValueAtTime(vol, t + 0.015); g.gain.exponentialRampToValueAtTime(0.001, t + 0.07);
    n.connect(hp).connect(g); out(g, A.musicBus, 0); n.start(t); n.stop(t + 0.08);
  }

  // Warm pop progression I – V – vi – IV (major scale degrees)
  const CHILL_CHORDS = [[0, 2, 4], [4, 6, 8], [5, 7, 9], [3, 5, 7]];
  const CHILL_MEL_A = [[0, 4, 6], [6, 5, 3], [9, 4, 3], [12, 4, 9], [21, 2, 3], [24, 5, 6], [30, 4, 3], [33, 2, 3], [36, 3, 6], [42, 2, 6]];
  const CHILL_MEL_B = [[0, 7, 6], [6, 6, 3], [9, 5, 3], [12, 4, 12], [24, 5, 3], [27, 4, 3], [30, 2, 6], [36, 1, 6], [42, 0, 6]];

  function chillStep(song, i, t, stepDur) {
    const s = i % 12, bar = Math.floor(i / 12) % 4, loop = Math.floor(i / 48), pos = i % 48;
    const ch = CHILL_CHORDS[bar], V = 2.2; // overall loudness of this style
    // gentle broken chords
    if (s === 0) { ch.forEach(d => epiano(hz(d, -1), t, stepDur * 12, 0.05 * V)); epiano(hz(ch[0], -2), t, stepDur * 12, 0.08 * V); }
    if (s === 3) epiano(hz(ch[1]), t, stepDur * 4, 0.04 * V);
    if (s === 6) epiano(hz(ch[2]), t, stepDur * 4, 0.04 * V);
    if (s === 9) epiano(hz(ch[1]), t, stepDur * 4, 0.035 * V);
    // light tabla & shaker
    if (s === 0) tablaBayan(t, 0.22 * V);
    if (s === 6 && song.busy) tablaBayan(t, 0.14 * V);
    if (s === 3 || s === 9) tablaDayan(t, 1, 0.1 * V);
    if (song.busy ? s % 3 === 0 : s === 6) shaker(t, (song.busy ? 0.025 : 0.02) * V);
    const mel = song.melodies[loop % song.melodies.length];
    if (mel) for (const [st, deg, len] of mel) {
      if (st === pos) song.leadVoice.note(hz(deg, 1), t, stepDur * len, Math.random() < 0.3 ? { glide: true } : {});
    }
  }

  const SONGS = {
    chill: { bpm: 80, lead: 'bansuri', swing: false, step: chillStep, melodies: [null, CHILL_MEL_A, null, CHILL_MEL_B] },
    chillPlay: { bpm: 92, lead: 'bansuri', swing: false, step: chillStep, busy: true, melodies: [CHILL_MEL_A, null, CHILL_MEL_B, null] },
    // Calm, warm groove: dholak + harmonium + bansuri
    classic: {
      bpm: 92, lead: 'bansuri', swing: true, melodies: [MENU_MELODY, MENU_MELODY_B, null, MENU_MELODY],
      drums(s, t, intense) {
        if (s === 0 || s === 6) dholBass(t, s === 0 ? 0.55 : 0.4);
        if (s === 3 || s === 9) dholTilli(t, 0.22);
        if (s === 5 || s === 11) dholTilli(t, 0.12);
      },
      harm: true, tumbi: false, clap: false,
    },
    // Upbeat bhangra: dhol chaal + claps + tumbi + harmonium lead (speeds up near the end)
    party: {
      bpm: 118, lead: 'reed', swing: true, canIntense: true, melodies: [PLAY_MELODY, PLAY_MELODY_B],
      drums(s, t, intense) {
        if (s === 0 || s === 5 || s === 6 || (intense && s === 11)) dholBass(t, s === 0 ? 0.9 : 0.7);
        if (s === 2 || s === 3 || s === 8 || s === 9) dholTilli(t, s % 3 === 0 ? 0.35 : 0.2);
        if (intense && (s === 1 || s === 7 || s === 10)) dholTilli(t, 0.15);
      },
      harm: true, tumbi: true, clap: true,
    },
  };

  // Music styles the player can choose in Settings: which song plays on menus vs. during a round.
  const STYLES = {
    chill: { menu: 'chill', play: 'chillPlay' },
    classic: { menu: 'classic', play: 'classic' },
    party: { menu: 'classic', play: 'party' },
  };
  A.style = 'chill';
  A._role = null;

  function scheduleStep(song, i, t, stepDur) {
    if (song.step) return song.step(song, i, t, stepDur);
    const s = i % 12, bar = Math.floor(i / 12) % 4, loop = Math.floor(i / 48), pos = i % 48;
    song.drums(s, t, song.intense);
    if (song.clap && (s === 3 || s === 9)) clap(t, 0.22);
    if (s === 0) {
      const chord = CHORDS[bar].map(d => kz(d, -1));
      if (song.harm) harmonium(chord, t, stepDur * 12 * 0.98, song.lead === 'reed' ? 0.045 : 0.06);
      bass(kz(ROOTS[bar], -2), t, stepDur * 5);
    }
    if (s === 6) bass(kz(ROOTS[bar] + 4, -2), t, stepDur * 5, 0.2);
    if (song.tumbi && [0, 2, 3, 5, 6, 8, 9, 11].includes(s)) {
      tumbi(kz(s % 6 === 0 ? 7 : (s % 3 === 0 ? 4 : 7), 1), t, s % 6 === 0 ? 0.13 : 0.08);
    }
    const mel = song.melodies[loop % song.melodies.length];
    if (mel) {
      for (const [st, deg, len] of mel) {
        if (st === pos) {
          const f = kz(deg, song.lead === 'bansuri' ? 1 : 0);
          const opts = {};
          if (len >= 3 && Math.random() < 0.5) opts.kan = kz(deg + 1, song.lead === 'bansuri' ? 1 : 0);
          else if (Math.random() < 0.35) opts.glide = true;
          song.leadVoice.note(f, t, stepDur * len, opts);
        }
      }
    }
  }

  function scheduler() {
    const song = A._song; if (!song) return;
    const stepDur = 60 / (song.bpm * (song.intense ? 1.12 : 1)) / 3;
    while (A._nextTime < A.ctx.currentTime + 0.15) {
      const s = A._step % 3;
      const swing = !song.swing ? 1 : s === 0 ? 1.06 : s === 1 ? 0.94 : 1;
      scheduleStep(song, A._step, A._nextTime, stepDur);
      A._nextTime += stepDur * swing;
      A._step++;
    }
  }

  // role: 'menu' or 'play'. The chosen style decides the actual song.
  A.playSong = function (role) {
    if (!A.ctx) return;
    const name = (STYLES[A.style] || STYLES.chill)[role] || role;
    if (A._song && A._song.name === name) { A._role = role; return; }
    A.stopSong();
    A._role = role;
    const s = SONGS[name]; if (!s) return;
    A._song = Object.assign({ name, intense: false }, s);
    A._song.leadVoice = makeLead(s.lead);
    A._step = 0; A._nextTime = A.ctx.currentTime + 0.08;
    A._timer = setInterval(scheduler, 25);
  };
  A.setStyle = function (style) {
    if (!STYLES[style]) return;
    A.style = style;
    if (A._song && A._role) { const r = A._role; A.stopSong(); A.playSong(r); }
  };
  A.setIntense = function (on) { if (A._song && A._song.canIntense) A._song.intense = on; };
  A.stopSong = function () {
    clearInterval(A._timer); A._timer = null;
    if (A._song && A._song.leadVoice) A._song.leadVoice.stop();
    A._song = null; A._role = null;
  };
  A._harmonium = (degs, t, dur, vol) => harmonium(degs.map(d => kz(d)), t, dur, vol, A.sfxBus);

  /* ---------- SFX ---------- */
  A.sfx = {
    tap() {
      if (!A.ctx) return; const t = A.ctx.currentTime; tablaDayan(t, 1.3, 0.25, A.sfxBus);
    },
    // Temple bell (ghanti) + harmonium "ta-daa"
    correct() {
      if (!A.ctx) return; const t = A.ctx.currentTime;
      [1, 2.76, 5.4, 8.93].forEach((r, i) => {
        const o = A.ctx.createOscillator(), g = A.ctx.createGain();
        o.type = 'sine'; o.frequency.value = 880 * r;
        g.gain.setValueAtTime(0.22 / (i + 1), t); g.gain.exponentialRampToValueAtTime(0.0005, t + 1.2 / (i * 0.5 + 1));
        o.connect(g); out(g, A.sfxBus, 0.5); o.start(t); o.stop(t + 1.3);
      });
      A._harmonium([7, 9, 11], t + 0.02, 0.22, 0.07); A._harmonium([9, 11, 14], t + 0.2, 0.4, 0.07);
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
      else { dhol(t, 1); dhol(t + 0.12, 0.8); A._harmonium([0, 2, 4, 7], t, 0.6, 0.06); }
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
