/* Bujho Toh! — main game controller. */
(function () {
  'use strict';
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
  const A = window.Audio2, T = window.Tilt, FX = window.FX;

  /* ---------- Persistence (wrapped: storage may be unavailable) ---------- */
  const store = {
    get(k, d) { try { const v = localStorage.getItem('bujho:' + k); return v === null ? d : JSON.parse(v); } catch (e) { return d; } },
    set(k, v) { try { localStorage.setItem('bujho:' + k, JSON.stringify(v)); } catch (e) {} },
  };
  const settings = Object.assign(
    { music: true, sfx: true, vibe: true, controls: 'auto', sens: 'medium', time: 60 },
    store.get('settings', {})
  );
  const saveSettings = () => store.set('settings', settings);
  let best = store.get('best', {});
  let seen = store.get('seen', {});
  let customDecks = store.get('custom', []);

  A.musicOn = settings.music; A.sfxOn = settings.sfx;
  T.setSensitivity(settings.sens);

  /* ---------- Decks ---------- */
  const HINTS = { say: '', act: '🎭 Act it out — no talking!', hum: '🎤 Hum it — no lyrics!' };
  const builtIn = window.DECKS.map(d => Object.assign({}, d, { words: Array.from(new Set(d.words)) }));
  const MIX = {
    id: 'mix', name: 'Mix Masala', hindi: 'मिक्स मसाला', emoji: '🌶️', colors: ['#ff0844', '#ffb199'],
    desc: 'A spicy mix from every deck. Anything can come up!', mode: 'say', mix: true,
    get words() { return builtIn.flatMap(d => d.words); },
  };
  const allDecks = () => [MIX, ...builtIn, ...customDecks];
  const findDeck = id => allDecks().find(d => d.id === id);

  function cardsFor(deck) {
    if (deck.mix) {
      return builtIn.flatMap(d => d.words.map(w => ({ t: w, hint: `${d.emoji} ${d.name}${d.mode !== 'say' ? ' · ' + HINTS[d.mode].split(' — ')[0].slice(3) : ''}` })));
    }
    return deck.words.map(w => ({ t: w, hint: HINTS[deck.mode || 'say'] }));
  }

  function shuffle(a) {
    for (let i = a.length - 1; i > 0; i--) { const j = (Math.random() * (i + 1)) | 0; [a[i], a[j]] = [a[j], a[i]]; }
    return a;
  }

  /* ---------- Screens & sheets ---------- */
  function show(id) {
    $$('.screen').forEach(s => s.classList.toggle('active', s.id === id));
    document.body.dataset.screen = id;
    window.scrollTo(0, 0);
    updateRotation();
  }
  function openSheet(id) { $('#' + id).classList.add('open'); }
  function closeSheets() { $$('.sheet').forEach(s => s.classList.remove('open')); }
  $$('[data-close]').forEach(el => el.addEventListener('click', () => { A.sfx.tap(); closeSheets(); }));
  $$('[data-home]').forEach(el => el.addEventListener('click', goHome));

  let toastTimer;
  function toast(msg) {
    const t = $('#toast'); t.textContent = msg; t.classList.add('show');
    clearTimeout(toastTimer); toastTimer = setTimeout(() => t.classList.remove('show'), 2600);
  }

  // First interaction anywhere unlocks audio (browser autoplay rules).
  function unlockAudio() {
    A.init();
    if (document.body.dataset.screen === 'home' || !document.body.dataset.screen) A.playSong('menu');
  }
  addEventListener('pointerdown', unlockAudio, { once: true });
  addEventListener('keydown', unlockAudio, { once: true });

  /* ---------- Home grid ---------- */
  function renderGrid() {
    const g = $('#deckGrid'); g.innerHTML = '';
    allDecks().forEach((d, i) => {
      const b = document.createElement('button');
      b.className = 'deck' + (d.mix ? ' wide' : '');
      b.style.setProperty('--c1', d.colors[0]); b.style.setProperty('--c2', d.colors[1]);
      b.style.setProperty('--d', (i * 0.4) + 's');
      b.style.animationDelay = (i * 0.04) + 's';
      const bst = best[d.id];
      b.innerHTML = `
        ${d.custom ? '<span class="badge">APNA</span>' : bst ? `<span class="badge">🏆 ${bst}</span>` : ''}
        <span class="emo">${d.emoji}</span>
        <span class="nm"></span><span class="hi"></span>
        <span class="ct">${d.mix ? builtIn.reduce((n, x) => n + x.words.length, 0) : d.words.length} cards${d.mode === 'act' ? ' · 🎭' : d.mode === 'hum' ? ' · 🎤' : ''}</span>`;
      b.querySelector('.nm').textContent = d.name;
      b.querySelector('.hi').textContent = d.hindi || '';
      b.addEventListener('click', () => { A.init(); A.sfx.tap(); openDeck(d); });
      g.appendChild(b);
    });
    const add = document.createElement('button');
    add.className = 'deck add';
    add.innerHTML = '<span class="nm">➕ Apna Deck banao</span><span class="ct">Make your own cards</span>';
    add.addEventListener('click', () => { A.init(); A.sfx.tap(); openCustom(); });
    g.appendChild(add);
  }

  /* ---------- Deck sheet ---------- */
  let currentDeck = null;
  function openDeck(d) {
    currentDeck = d;
    $('#sheetEmoji').textContent = d.emoji;
    $('#sheetTitle').textContent = d.name;
    $('#sheetHindi').textContent = d.hindi || '';
    $('#sheetDesc').textContent = d.desc || '';
    $('#sheetCount').textContent = `🃏 ${cardsFor(d).length} cards`;
    $('#sheetBest').textContent = best[d.id] ? `🏆 Best: ${best[d.id]}` : '';
    $('#customActions').hidden = !d.custom;
    setChips('#timeChips', 't', settings.time);
    openSheet('deckSheet');
  }
  function setChips(sel, attr, val) {
    $$(sel + ' button').forEach(b => b.classList.toggle('on', String(b.dataset[attr]) === String(val)));
  }
  $$('#timeChips button').forEach(b => b.addEventListener('click', () => {
    settings.time = +b.dataset.t; saveSettings(); setChips('#timeChips', 't', settings.time); A.sfx.tap();
  }));

  /* ---------- Fullscreen / orientation / wake lock ---------- */
  let wakeLock = null;
  async function enterGameMode() {
    try {
      const el = document.documentElement;
      if (!document.fullscreenElement && el.requestFullscreen && matchMedia('(pointer: coarse)').matches) {
        await el.requestFullscreen({ navigationUI: 'hide' });
      }
    } catch (e) {}
    try { if (screen.orientation && screen.orientation.lock) await screen.orientation.lock('landscape'); } catch (e) {}
    try { if ('wakeLock' in navigator) wakeLock = await navigator.wakeLock.request('screen'); } catch (e) {}
  }
  function exitGameMode() {
    try { if (screen.orientation && screen.orientation.unlock) screen.orientation.unlock(); } catch (e) {}
    try { if (document.fullscreenElement) document.exitFullscreen(); } catch (e) {}
    try { wakeLock && wakeLock.release(); } catch (e) {} wakeLock = null;
  }
  document.addEventListener('visibilitychange', async () => {
    if (document.visibilityState === 'visible' && round && round.running && 'wakeLock' in navigator) {
      try { wakeLock = await navigator.wakeLock.request('screen'); } catch (e) {}
    }
  });

  // If the page can't rotate (iPhone rotation lock) but the phone is held sideways, rotate the stage ourselves.
  function setViewportVars() {
    document.documentElement.style.setProperty('--vw', innerWidth + 'px');
    document.documentElement.style.setProperty('--vh', innerHeight + 'px');
  }
  let rot = '';
  function updateRotation() {
    setViewportVars();
    const inGame = ['play'].includes(document.body.dataset.screen);
    const portrait = innerHeight > innerWidth;
    let want = '';
    if (inGame && portrait && T.supported) {
      if (T.sideX > 0.6) want = 'rot-cw';
      else if (T.sideX < -0.6) want = 'rot-ccw';
      else want = rot; // hysteresis: keep previous
    }
    if (want !== rot) {
      document.body.classList.remove('rot-cw', 'rot-ccw');
      if (want) document.body.classList.add(want);
      rot = want;
      if (round && round.current) requestAnimationFrame(fitWord);
    }
    const ready = document.body.dataset.screen === 'ready';
    $('#rotateHint').style.display = (ready && portrait && T.supported && Math.abs(T.sideX) < 0.5 && useTilt) ? 'block' : 'none';
  }
  addEventListener('resize', () => { updateRotation(); if (round && round.current) fitWord(); });

  /* ---------- Starting a round ---------- */
  let useTilt = false;
  let readyWatch = null;

  $('#btnPlay').addEventListener('click', async () => {
    A.init(); A.sfx.tap();
    closeSheets();
    // Permission must be requested inside this tap (iOS).
    let perm = 'unsupported';
    if (settings.controls !== 'tap') perm = await T.requestPermission();
    enterGameMode();
    show('ready');
    A.stopSong();
    $('#readyHint').textContent = '';
    useTilt = false;
    if (settings.controls !== 'tap') {
      // Probe even if the permission call said no: some browsers still deliver events.
      $('#readyTitle').textContent = 'Checking motion sensor…';
      useTilt = await T.probe(perm === 'granted' ? 1200 : 600);
    }
    if (settings.controls === 'tilt' && !useTilt) {
      $('#readyHint').textContent = perm === 'denied'
        ? 'Motion access was denied. Using tap buttons instead (you can allow it in browser settings).'
        : 'No motion sensor found — using tap buttons instead.';
    }
    setupReady();
  });

  function setupReady() {
    clearInterval(readyWatch);
    if (useTilt) {
      $('#readyTitle').textContent = 'Phone ko maathe pe rakho!';
      $('#readyText').textContent = 'Hold the phone sideways on your forehead, screen facing your friends. The game starts by itself.';
      $('.tilt-legend').style.display = '';
      $('#btnStartNow').textContent = 'Start now';
      let uprightSince = 0;
      readyWatch = setInterval(() => {
        updateRotation();
        if (document.body.dataset.screen !== 'ready') return clearInterval(readyWatch);
        const ok = T.isUpright() && Math.abs(T.sideX) > 0.7; // vertical AND sideways = on the forehead
        if (ok) {
          if (!uprightSince) uprightSince = performance.now();
          if (performance.now() - uprightSince > 900) { clearInterval(readyWatch); beginCountdown(); }
        } else uprightSince = 0;
      }, 100);
    } else {
      $('#readyTitle').textContent = 'Tap mode';
      $('#readyText').textContent = 'A friend holds the phone (or play on a laptop) and taps Sahi / Pass. Keyboard: ↓ or → = correct, ↑ or ← = pass.';
      $('.tilt-legend').style.display = 'none';
      $('#btnStartNow').textContent = 'Start! ▶';
    }
  }
  $('#btnStartNow').addEventListener('click', () => { A.init(); clearInterval(readyWatch); beginCountdown(); });

  /* ---------- Round state ---------- */
  let round = null;

  function buildQueue(deck) {
    const cards = cardsFor(deck);
    let s = new Set(seen[deck.id] || []);
    let fresh = cards.filter(c => !s.has(c.t));
    if (fresh.length < 15) { s = new Set(); seen[deck.id] = []; fresh = cards.slice(); }
    return shuffle(fresh);
  }

  function beginCountdown() {
    const deck = currentDeck;
    round = {
      deck, queue: buildQueue(deck), results: [], current: null, score: 0,
      duration: settings.time, running: false, locked: true, endAt: 0, lastSec: -1,
    };
    const stage = $('#stage');
    stage.style.setProperty('--c1', deck.colors[0]); stage.style.setProperty('--c2', deck.colors[1]);
    stage.classList.toggle('tap-mode', !useTilt);
    $('#playCat').textContent = `${deck.emoji} ${deck.name}`;
    $('#playScore').textContent = '0';
    $('#playTimer').textContent = round.duration;
    $('#playTimer').classList.remove('urgent');
    $('#timerBar').style.transform = 'scaleX(1)';
    $('#word').textContent = ''; $('#wordHint').textContent = '';
    show('play');

    const cd = $('#countdown'), num = $('#countNum');
    cd.classList.add('show');
    let n = 3;
    const step = () => {
      if (!round) return;
      num.className = ''; void num.offsetWidth;
      if (n > 0) { num.textContent = n; num.className = 'tick'; A.sfx.count(n); n--; setTimeout(step, 900); }
      else {
        num.textContent = 'Shuru!'; num.className = 'tick go'; A.sfx.count(0);
        setTimeout(() => { cd.classList.remove('show'); startRound(); }, 650);
      }
    };
    step();
  }

  function startRound() {
    if (!round) return;
    round.running = true; round.locked = false;
    round.endAt = performance.now() + round.duration * 1000;
    if (useTilt) { T.onCorrect = () => answer(true); T.onPass = () => answer(false); T.enable(); }
    A.playSong('play'); A.setIntense(false);
    nextWord();
    requestAnimationFrame(loop);
  }

  function nextWord() {
    if (!round.queue.length) round.queue = buildQueue(round.deck);
    round.current = round.queue.shift();
    const w = $('#word');
    w.textContent = round.current.t;
    $('#wordHint').textContent = round.current.hint || '';
    w.classList.remove('in'); void w.offsetWidth; w.classList.add('in');
    fitWord();
  }

  // Grow the word as large as it can be while still fitting the stage.
  function fitWord() {
    const w = $('#word'), wrap = w.parentElement;
    const maxW = wrap.clientWidth * 0.96, maxH = wrap.clientHeight * 0.82 - ($('#wordHint').offsetHeight || 0);
    let lo = 18, hi = Math.min(260, Math.max(40, maxH));
    while (hi - lo > 1) {
      const mid = (lo + hi) / 2;
      w.style.fontSize = mid + 'px';
      if (w.scrollWidth <= maxW && w.offsetHeight <= maxH && !hasBrokenWord(w, maxW)) lo = mid; else hi = mid;
    }
    w.style.fontSize = lo + 'px';
  }
  // Prevent a single long word being split mid-word when a smaller font would keep it whole.
  function hasBrokenWord(el, maxW) {
    const longest = el.textContent.split(/\s+/).reduce((a, b) => (b.length > a.length ? b : a), '');
    const probe = fitWord.probe || (fitWord.probe = Object.assign(document.createElement('span'), { style: 'position:absolute;visibility:hidden;white-space:nowrap;font-weight:800' }));
    if (!probe.isConnected) document.body.appendChild(probe);
    probe.style.fontSize = el.style.fontSize; probe.style.fontFamily = getComputedStyle(el).fontFamily;
    probe.textContent = longest;
    return probe.offsetWidth > maxW;
  }

  function loop() {
    if (!round || !round.running) return;
    const left = Math.max(0, round.endAt - performance.now());
    const sec = Math.ceil(left / 1000);
    $('#timerBar').style.transform = `scaleX(${left / (round.duration * 1000)})`;
    if (sec !== round.lastSec) {
      round.lastSec = sec;
      $('#playTimer').textContent = sec;
      if (sec <= 10 && sec > 0) {
        $('#playTimer').classList.add('urgent');
        A.sfx.tick(sec <= 5);
        if (sec === 10) A.setIntense(true);
      }
    }
    updateRotation();
    if (left <= 0) return endRound();
    requestAnimationFrame(loop);
  }

  function vibrate(p) { if (settings.vibe && navigator.vibrate) try { navigator.vibrate(p); } catch (e) {} }

  function answer(ok) {
    if (!round || !round.running || round.locked) return;
    round.locked = true;
    round.results.push({ t: round.current.t, ok });
    (seen[round.deck.id] = seen[round.deck.id] || []).push(round.current.t);
    const flash = $('#flash');
    flash.className = 'flash show ' + (ok ? 'correct' : 'pass');
    $('#flashText').textContent = ok ? pick(['Sahi! ✓', 'Waah! ✓', 'Correct! ✓', 'Jhakaas! ✓', 'Ekdum! ✓']) : 'Pass ⏭';
    if (ok) {
      round.score++;
      const sc = $('#playScore'); sc.textContent = round.score;
      sc.classList.remove('bump'); void sc.offsetWidth; sc.classList.add('bump');
      A.sfx.correct(); vibrate(60); FX.burst({ count: 60 });
    } else { A.sfx.pass(); vibrate([30, 40, 30]); }
    setTimeout(() => {
      flash.className = 'flash';
      if (!round || !round.running) return;
      nextWord();
      round.locked = false;
    }, 700);
  }
  const pick = a => a[(Math.random() * a.length) | 0];

  $('#tapCorrect').addEventListener('click', () => answer(true));
  $('#tapPass').addEventListener('click', () => answer(false));
  addEventListener('keydown', e => {
    if (document.body.dataset.screen !== 'play') return;
    if (['ArrowDown', 'ArrowRight', 'Enter'].includes(e.key)) { e.preventDefault(); answer(true); }
    else if (['ArrowUp', 'ArrowLeft'].includes(e.key)) { e.preventDefault(); answer(false); }
    else if (e.key === 'Escape') quitRound();
  });
  $('#btnQuit').addEventListener('click', quitRound);

  function quitRound() {
    if (!round) return;
    if (round.running && !confirm('End this round?')) return;
    round.running = false; T.disable(); round = null;
    $('#countdown').classList.remove('show');
    goHome();
  }

  function endRound() {
    round.running = false; round.locked = true;
    T.disable();
    // The word on screen when time ran out counts as a miss (unless it was just answered).
    const last = round.results[round.results.length - 1];
    if (round.current && !(last && last.t === round.current.t)) {
      round.results.push({ t: round.current.t, ok: false, timeout: true });
    }
    A.stopSong(); A.sfx.shankh(); vibrate([100, 60, 200]);
    $('#flash').className = 'flash show pass';
    $('#flashText').textContent = 'Time up! ⏰';
    setTimeout(() => { $('#flash').className = 'flash'; showResults(); }, 1600);
  }

  /* ---------- Results ---------- */
  function showResults() {
    const r = round; if (!r) return;
    exitGameMode();
    document.body.classList.remove('rot-cw', 'rot-ccw'); rot = '';
    store.set('seen', seen);
    const prev = best[r.deck.id] || 0;
    const isBest = r.score > prev && r.score > 0;
    if (isBest) { best[r.deck.id] = r.score; store.set('best', best); }

    const titles = r.score >= 12 ? ['Ekdum Jhakaas! 🔥', 'Kya baat hai! 🔥', 'Superstar! 🌟']
      : r.score >= 8 ? ['Shabaash! 🎉', 'Waah! 🎉', 'Zabardast! 🎉']
      : r.score >= 4 ? ['Not bad! 👍', 'Accha hai! 👍', 'Chalega! 👍']
      : ['Arre yaar! 😅', 'Koi baat nahi 😅', 'Agli baar pakka! 😅'];
    $('#resTitle').textContent = pick(titles);
    $('#resSub').textContent = isBest ? `🏆 New best for ${r.deck.name}!` : prev ? `Best: ${prev}` : '';
    const list = $('#resList'); list.innerHTML = '';
    r.results.forEach((x, i) => {
      const li = document.createElement('li');
      li.className = x.ok ? 'ok' : 'no';
      li.style.animationDelay = (0.25 + i * 0.05) + 's';
      li.innerHTML = '<span></span><b></b>';
      li.firstChild.textContent = x.t;
      li.lastChild.textContent = x.ok ? '✓' : x.timeout ? '⏰' : '⏭';
      list.appendChild(li);
    });
    show('results');
    // count-up animation
    const el = $('#resScore'); let n = 0; el.textContent = '0';
    const iv = setInterval(() => { if (n >= r.score) return clearInterval(iv); n++; el.textContent = n; A.sfx.tick(false); }, Math.max(40, 600 / Math.max(1, r.score)));
    A.sfx.fanfare(r.score >= 4);
    if (r.score >= 4) { FX.rain(r.score >= 8 ? 3.5 : 2); FX.burst({ count: 120, y: innerHeight * 0.3 }); }
    setTimeout(() => A.playSong('menu'), 2500);
  }

  $('#btnAgain').addEventListener('click', async () => {
    A.init(); A.sfx.tap();
    enterGameMode(); show('ready'); A.stopSong(); setupReady();
  });
  $('#btnShare').addEventListener('click', async () => {
    const r = round; if (!r) return;
    const text = `I got ${r.score} right in "${r.deck.name}" on Bujho Toh! 🇮🇳🎉 Can you beat me?`;
    const url = location.href.split('#')[0];
    try {
      if (navigator.share) await navigator.share({ title: 'Bujho Toh!', text, url });
      else { await navigator.clipboard.writeText(text + ' ' + url); toast('Copied! Paste it in WhatsApp 📋'); }
    } catch (e) {}
  });

  function goHome() {
    clearInterval(readyWatch);
    if (round && round.running) { round.running = false; T.disable(); }
    exitGameMode();
    document.body.classList.remove('rot-cw', 'rot-ccw'); rot = '';
    renderGrid(); show('home');
    if (A.ctx) A.playSong('menu');
  }

  /* ---------- Settings ---------- */
  function renderSettings() {
    $('#setMusic').checked = settings.music;
    $('#setSfx').checked = settings.sfx;
    $('#setVibe').checked = settings.vibe;
    setChips('#setControls', 'v', settings.controls);
    setChips('#setSens', 'v', settings.sens);
  }
  $('#btnSettings').addEventListener('click', () => { A.init(); A.sfx.tap(); renderSettings(); openSheet('settingsSheet'); });
  $('#btnHowTo').addEventListener('click', () => { A.init(); A.sfx.tap(); openSheet('howSheet'); });
  $('#setMusic').addEventListener('change', e => { settings.music = e.target.checked; saveSettings(); A.setMusic(settings.music); if (settings.music) A.playSong('menu'); });
  $('#setSfx').addEventListener('change', e => { settings.sfx = e.target.checked; saveSettings(); A.setSfx(settings.sfx); A.sfx.tap(); });
  $('#setVibe').addEventListener('change', e => { settings.vibe = e.target.checked; saveSettings(); vibrate(40); });
  $$('#setControls button').forEach(b => b.addEventListener('click', () => { settings.controls = b.dataset.v; saveSettings(); setChips('#setControls', 'v', settings.controls); A.sfx.tap(); }));
  $$('#setSens button').forEach(b => b.addEventListener('click', () => { settings.sens = b.dataset.v; saveSettings(); T.setSensitivity(settings.sens); setChips('#setSens', 'v', settings.sens); A.sfx.tap(); }));
  $('#btnResetSeen').addEventListener('click', () => { seen = {}; store.set('seen', seen); toast('Done — all cards are fresh again ✨'); });
  $('#btnTestTilt').addEventListener('click', async () => {
    const perm = await T.requestPermission();
    $('#tiltTest').hidden = false;
    if (perm !== 'granted') { $('#tiltTestText').textContent = 'Motion access not allowed on this device/browser.'; return; }
    const ok = await T.probe(1500);
    if (!ok) { $('#tiltTestText').textContent = 'No motion sensor detected. Tap buttons will be used.'; return; }
    T.onReading = (up) => {
      if (!$('#settingsSheet').classList.contains('open')) { T.onReading = null; return; }
      $('#tiltNeedle').style.left = `calc(${(1 - (up + 1) / 2) * 100}% - 3px)`;
      const d = up;
      $('#tiltTestText').textContent = d < -T.threshold ? '✅ CORRECT zone (screen to floor)' : d > T.threshold ? '⏭ PASS zone (screen to ceiling)' : '🙂 Neutral — phone upright';
    };
  });

  /* ---------- Custom decks ---------- */
  const EMOJIS = ['🎉', '👨‍👩‍👧', '🏢', '🎓', '🏖️', '🐶', '🍕', '💃', '🤪', '❤️', '🎮', '🏠'];
  let editing = null, custEmoji = EMOJIS[0], custMode = 'say';
  function openCustom(deck) {
    editing = deck || null;
    $('#custTitle').textContent = deck ? 'Edit deck' : 'Apna Deck banao';
    $('#custName').value = deck ? deck.name : '';
    $('#custWords').value = deck ? deck.words.join('\n') : '';
    custEmoji = deck ? deck.emoji : EMOJIS[0];
    custMode = deck ? deck.mode : 'say';
    $('#custErr').textContent = '';
    const ep = $('#custEmoji'); ep.innerHTML = '';
    EMOJIS.forEach(e => {
      const b = document.createElement('button'); b.textContent = e; b.type = 'button';
      b.classList.toggle('on', e === custEmoji);
      b.addEventListener('click', () => { custEmoji = e; $$('#custEmoji button').forEach(x => x.classList.toggle('on', x === b)); });
      ep.appendChild(b);
    });
    setChips('#custMode', 'v', custMode);
    closeSheets(); openSheet('customSheet');
  }
  $$('#custMode button').forEach(b => b.addEventListener('click', () => { custMode = b.dataset.v; setChips('#custMode', 'v', custMode); }));
  $('#btnSaveDeck').addEventListener('click', () => {
    const name = $('#custName').value.trim();
    const words = Array.from(new Set($('#custWords').value.split('\n').map(s => s.trim()).filter(Boolean)));
    if (!name) return ($('#custErr').textContent = 'Give your deck a name 🙂');
    if (words.length < 5) return ($('#custErr').textContent = `Add at least 5 cards (you have ${words.length}).`);
    const palettes = [['#ee0979', '#ff6a00'], ['#4776e6', '#8e54e9'], ['#00c6ff', '#0072ff'], ['#f7797d', '#fbd786'], ['#43cea2', '#185a9d']];
    if (editing) Object.assign(editing, { name, words, emoji: custEmoji, mode: custMode });
    else customDecks.push({ id: 'custom-' + Date.now(), custom: true, name, hindi: '', emoji: custEmoji, mode: custMode, words, desc: 'Your very own deck.', colors: pick(palettes) });
    store.set('custom', customDecks);
    closeSheets(); renderGrid(); toast('Deck saved! 🎉'); FX.burst({ count: 80 });
  });
  $('#btnEditDeck').addEventListener('click', () => openCustom(currentDeck));
  $('#btnDeleteDeck').addEventListener('click', () => {
    if (!currentDeck || !confirm(`Delete "${currentDeck.name}"?`)) return;
    customDecks = customDecks.filter(d => d.id !== currentDeck.id);
    store.set('custom', customDecks); closeSheets(); renderGrid();
  });

  /* ---------- Boot ---------- */
  document.body.dataset.screen = 'home';
  setViewportVars();
  renderGrid();
  if ('serviceWorker' in navigator && location.protocol.startsWith('http')) {
    addEventListener('load', () => navigator.serviceWorker.register('sw.js').catch(() => {}));
  }
  // Expose for debugging/tests
  window.Bujho = { answer, get round() { return round; }, findDeck, openDeck };
})();
