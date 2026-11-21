/* Bujho Toh! — forehead tilt detection.
 *
 * From the DeviceOrientation Euler angles (beta, gamma) we compute how much the
 * SCREEN faces the sky:  up = cos(beta) * cos(gamma)
 *   +1  screen facing the ceiling  (head tilted back)   -> PASS
 *    0  screen vertical            (phone on forehead)  -> neutral
 *   -1  screen facing the floor    (head nodded down)   -> CORRECT
 * This is independent of portrait/landscape and of which way the phone is
 * turned, and it behaves the same on iOS and Android (unlike raw accelerometer
 * signs, which differ between browsers).
 */
(function () {
  const RAD = Math.PI / 180;
  const T = {
    listening: false, supported: null, lastEventAt: 0,
    up: 0, sideX: 0, baseline: 0, armed: true, enabled: false,
    threshold: 0.55, rearm: 0.3, onCorrect: null, onPass: null, onReading: null,
    _samples: [],
  };

  T.setSensitivity = function (level) {
    // threshold on sin(angle): low ≈ 40°, medium ≈ 33°, high ≈ 25°
    T.threshold = { low: 0.65, medium: 0.55, high: 0.42 }[level] || 0.55;
    T.rearm = T.threshold * 0.55;
  };

  T.needsPermission = function () {
    return typeof DeviceOrientationEvent !== 'undefined' &&
      typeof DeviceOrientationEvent.requestPermission === 'function';
  };

  // Must be called from a user gesture on iOS 13+.
  T.requestPermission = async function () {
    if (typeof DeviceOrientationEvent === 'undefined') return 'unsupported';
    if (T.needsPermission()) {
      try { return (await DeviceOrientationEvent.requestPermission()) === 'granted' ? 'granted' : 'denied'; }
      catch (e) { return 'denied'; }
    }
    return 'granted';
  };

  function handle(e) {
    if (e.beta === null || e.gamma === null || e.beta === undefined) return;
    T.supported = true;
    T.lastEventAt = performance.now();
    const b = e.beta * RAD, g = e.gamma * RAD;
    const up = Math.cos(b) * Math.cos(g);
    // world "up" direction projected on the device x axis (used to rotate UI when the page is stuck in portrait)
    T.sideX = -Math.cos(b) * Math.sin(g);
    // light smoothing
    T.up = T.up * 0.35 + up * 0.65;
    T._samples.push(T.up); if (T._samples.length > 30) T._samples.shift();
    if (T.onReading) T.onReading(T.up, T.sideX);
    if (!T.enabled) return;

    const d = T.up - T.baseline;
    if (T.armed) {
      if (d < -T.threshold) { T.armed = false; T.onCorrect && T.onCorrect(); }
      else if (d > T.threshold) { T.armed = false; T.onPass && T.onPass(); }
    } else if (Math.abs(d) < T.rearm) {
      T.armed = true;
    }
  }

  T.listen = function () {
    if (T.listening) return;
    T.listening = true;
    window.addEventListener('deviceorientation', handle);
  };
  T.unlisten = function () {
    T.listening = false; T.enabled = false;
    window.removeEventListener('deviceorientation', handle);
  };

  // Probe whether real sensor data arrives within `ms`.
  T.probe = function (ms = 1200) {
    T.listen();
    return new Promise(res => {
      if (T.supported) return res(true);
      const start = performance.now();
      const iv = setInterval(() => {
        if (T.supported) { clearInterval(iv); res(true); }
        else if (performance.now() - start > ms) { clearInterval(iv); res(false); }
      }, 50);
    });
  };

  // Use recent readings as "neutral" (phone on forehead). If the reading is
  // implausible (e.g. phone still lying flat), assume vertical.
  T.calibrate = function () {
    const s = T._samples.slice(-10);
    const avg = s.length ? s.reduce((a, b) => a + b, 0) / s.length : 0;
    T.baseline = Math.abs(avg) < 0.45 ? avg : 0;
    T.armed = Math.abs(T.up - T.baseline) < T.rearm;
  };

  T.isUpright = function () { return Math.abs(T.up) < 0.35; };

  T.enable = function () { T.calibrate(); T.enabled = true; };
  T.disable = function () { T.enabled = false; };

  window.Tilt = T;
})();
