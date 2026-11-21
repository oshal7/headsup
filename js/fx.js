/* Bujho Toh! — visual effects: marigold petal confetti + floating diyas. */
(function () {
  const reduce = window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches;
  const cvs = document.getElementById('confetti');
  const ctx = cvs.getContext('2d');
  let parts = [], raf = null, W = 0, H = 0, dpr = 1;

  function resize() {
    dpr = Math.min(2, window.devicePixelRatio || 1);
    W = innerWidth; H = innerHeight;
    cvs.width = W * dpr; cvs.height = H * dpr;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  }
  addEventListener('resize', resize); resize();

  const COLORS = ['#ff9a00', '#ffb400', '#ffd200', '#ff6a00', '#ff3d6e', '#ffffff', '#38ef7d', '#e0218a'];

  function burst(opts = {}) {
    if (reduce) return;
    const n = opts.count || 80;
    const x = opts.x !== undefined ? opts.x : W / 2;
    const y = opts.y !== undefined ? opts.y : H / 2;
    for (let i = 0; i < n; i++) {
      const a = Math.random() * Math.PI * 2, s = 4 + Math.random() * (opts.power || 9);
      parts.push({
        x, y, vx: Math.cos(a) * s, vy: Math.sin(a) * s - (opts.lift || 4),
        r: 4 + Math.random() * 6, rot: Math.random() * 6.28, vr: (Math.random() - .5) * .3,
        c: COLORS[(Math.random() * COLORS.length) | 0], life: 1, shape: Math.random() < 0.7 ? 'petal' : 'dot',
      });
    }
    if (!raf) raf = requestAnimationFrame(tick);
  }

  function rain(seconds = 3) {
    if (reduce) return;
    const end = performance.now() + seconds * 1000;
    (function drop() {
      for (let i = 0; i < 4; i++) parts.push({
        x: Math.random() * W, y: -20, vx: (Math.random() - .5) * 2, vy: 2 + Math.random() * 3,
        r: 5 + Math.random() * 6, rot: Math.random() * 6.28, vr: (Math.random() - .5) * .2,
        c: COLORS[(Math.random() * COLORS.length) | 0], life: 1.6, shape: 'petal',
      });
      if (!raf) raf = requestAnimationFrame(tick);
      if (performance.now() < end) setTimeout(drop, 60);
    })();
  }

  function tick() {
    ctx.clearRect(0, 0, W, H);
    parts = parts.filter(p => p.life > 0 && p.y < H + 40);
    for (const p of parts) {
      p.vy += 0.18; p.vx *= 0.99; p.x += p.vx; p.y += p.vy; p.rot += p.vr; p.life -= 0.006;
      ctx.save(); ctx.translate(p.x, p.y); ctx.rotate(p.rot);
      ctx.globalAlpha = Math.max(0, Math.min(1, p.life));
      ctx.fillStyle = p.c;
      if (p.shape === 'petal') {
        ctx.beginPath(); ctx.ellipse(0, 0, p.r, p.r * 0.5, 0, 0, Math.PI * 2); ctx.fill();
      } else {
        ctx.beginPath(); ctx.arc(0, 0, p.r * 0.45, 0, Math.PI * 2); ctx.fill();
      }
      ctx.restore();
    }
    raf = parts.length ? requestAnimationFrame(tick) : null;
    if (!raf) ctx.clearRect(0, 0, W, H);
  }

  // Floating diyas in the background
  const diyas = document.querySelector('.diyas');
  if (diyas && !reduce) {
    for (let i = 0; i < 9; i++) {
      const d = document.createElement('span');
      d.className = 'diya';
      d.textContent = i % 3 === 0 ? '🪔' : '✨';
      d.style.left = (Math.random() * 100) + '%';
      d.style.animationDelay = (-Math.random() * 18) + 's';
      d.style.animationDuration = (14 + Math.random() * 10) + 's';
      d.style.fontSize = (14 + Math.random() * 16) + 'px';
      diyas.appendChild(d);
    }
  }

  window.FX = { burst, rain };
})();
