#!/usr/bin/env node
/*
 * Builds the Android app's card content: android/app/src/main/assets/content.json
 *   node tools/build-content.js
 *
 * Sources:
 *   content/categories.js   categories, regions and which India deck feeds each category
 *   content/world.js        universal decks ("world")
 *   content/us.js, gb.js, cn.js   country packs
 *   js/data.js              India packs (shared with the website)
 */
const fs = require('fs');
const path = require('path');
const ROOT = path.resolve(__dirname, '..');
const C = require(path.join(ROOT, 'content/categories.js'));

global.window = {};
require(path.join(ROOT, 'js/data.js'));
const indiaDecks = Object.fromEntries(window.DECKS.map(d => [d.id, d.words]));

const packs = {
  world: require(path.join(ROOT, 'content/world.js')),
  in: Object.fromEntries(Object.entries(C.india).map(([cat, deckId]) => {
    if (!indiaDecks[deckId]) throw new Error(`js/data.js has no deck "${deckId}"`);
    return [cat, indiaDecks[deckId]];
  })),
  us: require(path.join(ROOT, 'content/us.js')),
  gb: require(path.join(ROOT, 'content/gb.js')),
  cn: require(path.join(ROOT, 'content/cn.js')),
};

const regionIds = C.regions.map(r => r.id);
const categories = C.categories.map(c => {
  const variants = {};
  for (const r of regionIds) {
    const words = packs[r] && packs[r][c.id];
    if (words && words.length) variants[r] = Array.from(new Set(words.map(w => w.trim()).filter(Boolean)));
  }
  if (!Object.keys(variants).length) throw new Error(`category "${c.id}" has no cards`);
  for (const [r, w] of Object.entries(variants)) if (w.length < 20) throw new Error(`${c.id}/${r} has only ${w.length} cards`);
  return { ...c, variants };
});
// every pack key must belong to a category
for (const [r, p] of Object.entries(packs)) for (const k of Object.keys(p)) {
  if (!C.categories.some(c => c.id === k)) throw new Error(`content for unknown category "${k}" in region "${r}"`);
}

const out = path.join(ROOT, 'android/app/src/main/assets/content.json');
fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, JSON.stringify({ version: 2, regions: C.regions, categories }, null, 1) + '\n');
const total = categories.reduce((n, c) => n + Object.values(c.variants).reduce((m, w) => m + w.length, 0), 0);
console.log(`${categories.length} categories, ${total} cards -> ${path.relative(ROOT, out)}`);
for (const c of categories) console.log(`  ${c.emoji} ${c.name.padEnd(22)} ${Object.entries(c.variants).map(([r, w]) => `${r}:${w.length}`).join('  ')}`);
