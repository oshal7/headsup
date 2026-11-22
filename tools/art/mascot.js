/*
 * "Headzy" mascot: a round buddy with a phone on its forehead, plus tilt arrows.
 * Shared by tools/render-icons.js (launcher icon, store icon, feature graphic, in-app art).
 * Everything is drawn in a 512x512 box.
 */
const DEFS = `
  <radialGradient id="hzBg" cx="50%" cy="28%" r="85%"><stop offset="0" stop-color="#8a3cff"/><stop offset=".55" stop-color="#3a0d78"/><stop offset="1" stop-color="#1a0638"/></radialGradient>
  <radialGradient id="hzFace" cx="40%" cy="35%" r="70%"><stop offset="0" stop-color="#ffe680"/><stop offset=".6" stop-color="#ffc21a"/><stop offset="1" stop-color="#ff9500"/></radialGradient>
  <linearGradient id="hzPhone" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#ff4f8b"/><stop offset="1" stop-color="#ff8a3d"/></linearGradient>
  <linearGradient id="hzUp" x1="0" y1="1" x2="0" y2="0"><stop offset="0" stop-color="#ff7a1a"/><stop offset="1" stop-color="#ffb347"/></linearGradient>
  <linearGradient id="hzDown" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#4dffa6"/><stop offset="1" stop-color="#14c46a"/></linearGradient>`;

/* Curved arrow: up (pass) on the left, down (correct) on the right. */
const arrows = (mono) => `
  <g stroke="#1b0638" stroke-width="${mono ? 0 : 9}" stroke-linejoin="round">
    <path d="M64 330 C 52 260 62 205 96 160 L 74 150 L 138 120 L 140 190 L 120 178 C 96 214 90 262 100 318 Z" fill="${mono ? '#fff' : 'url(#hzUp)'}"/>
    <path d="M448 182 C 460 252 450 307 416 352 L 438 362 L 374 392 L 372 322 L 392 334 C 416 298 422 250 412 194 Z" fill="${mono ? '#fff' : 'url(#hzDown)'}"/>
  </g>`;

/* The buddy. */
const buddy = (mono) => mono ? `
  <circle cx="256" cy="318" r="128" fill="#fff"/>
  <rect x="150" y="128" width="212" height="122" rx="24" fill="#fff"/>` : `
  <!-- body / head -->
  <ellipse cx="256" cy="452" rx="120" ry="18" fill="#000" opacity=".25"/>
  <circle cx="256" cy="320" r="128" fill="url(#hzFace)" stroke="#1b0638" stroke-width="10"/>
  <ellipse cx="206" cy="262" rx="34" ry="20" fill="#fff" opacity=".45" transform="rotate(-25 206 262)"/>
  <!-- cheeks -->
  <ellipse cx="180" cy="356" rx="22" ry="13" fill="#ff5c8a" opacity=".55"/>
  <ellipse cx="332" cy="356" rx="22" ry="13" fill="#ff5c8a" opacity=".55"/>
  <!-- eyes looking up at the phone -->
  <ellipse cx="214" cy="316" rx="25" ry="30" fill="#fff" stroke="#1b0638" stroke-width="7"/>
  <ellipse cx="298" cy="316" rx="25" ry="30" fill="#fff" stroke="#1b0638" stroke-width="7"/>
  <circle cx="218" cy="303" r="12" fill="#1b0638"/><circle cx="302" cy="303" r="12" fill="#1b0638"/>
  <circle cx="222" cy="298" r="4" fill="#fff"/><circle cx="306" cy="298" r="4" fill="#fff"/>
  <!-- big grin -->
  <path d="M208 370 Q256 420 304 370 Q256 384 208 370Z" fill="#7a1035" stroke="#1b0638" stroke-width="7" stroke-linejoin="round"/>
  <path d="M232 392 Q256 404 280 392 Q256 384 232 392Z" fill="#ff6f91"/>
  <!-- phone on the forehead -->
  <g transform="rotate(-6 256 190)">
    <rect x="146" y="126" width="220" height="128" rx="26" fill="#1b0638"/>
    <rect x="160" y="139" width="192" height="102" rx="16" fill="url(#hzPhone)"/>
    <circle cx="153" cy="190" r="3.5" fill="#5b3a8f"/>
    <text x="256" y="216" font-family="Arial Black, Arial, sans-serif" font-weight="900" font-size="82" text-anchor="middle" fill="#fff">?</text>
  </g>
  <!-- little hands holding the phone -->
  <g fill="url(#hzFace)" stroke="#1b0638" stroke-width="8">
    <ellipse cx="150" cy="214" rx="22" ry="27" transform="rotate(-20 150 214)"/>
    <ellipse cx="364" cy="190" rx="22" ry="27" transform="rotate(20 364 190)"/>
  </g>`;

const art = (mono = false) => `${arrows(mono)}${buddy(mono)}`;

module.exports = { DEFS, art };
