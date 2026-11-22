#!/usr/bin/env node
/* Exports the web game's decks (js/data.js) to JSON for the Android app.
 *   node tools/build-decks.js
 * Output: android/app/src/main/assets/decks.json
 * js/data.js stays the single source of truth for cards. */
const fs = require('fs');
const path = require('path');
const ROOT = path.resolve(__dirname, '..');
global.window = {};
require(path.join(ROOT, 'js/data.js'));
const decks = window.DECKS.map(d => ({
  id: d.id, name: d.name, hindi: d.hindi || '', emoji: d.emoji, colors: d.colors,
  desc: d.desc || '', mode: d.mode || 'say', words: Array.from(new Set(d.words)),
}));
const out = path.join(ROOT, 'android/app/src/main/assets/decks.json');
fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, JSON.stringify({ version: 1, decks }, null, 1));
console.log(`${decks.length} decks, ${decks.reduce((n, d) => n + d.words.length, 0)} cards -> ${path.relative(ROOT, out)}`);
