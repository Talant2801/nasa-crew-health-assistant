// Record a captioned ~30 s demo of the running app (headless Chrome, DevTools screencast).
// Needs: app on :8080, google-chrome, Node 22+, ffmpeg.
//   node tools/record-demo.mjs /tmp/frames
//   cd /tmp/frames && ffmpeg -f concat -safe 0 -i frames.txt -vf "fps=30,format=yuv420p" \
//     -c:v libx264 -crf 20 -movflags +faststart docs/demo/crew-health-demo.mp4
// Captions and click rings are injected into the page only while recording; the app itself is unchanged.
import { spawn } from 'node:child_process';
import { writeFileSync, mkdtempSync, mkdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const FRAMES = process.argv[2];
const APP = 'http://localhost:8080';
const PORT = 9334;
const W = 1280, H = 884; // 800 px app + 84 px caption bar
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
mkdirSync(FRAMES, { recursive: true });

const profile = mkdtempSync(join(process.argv[3] || tmpdir(), 'chrome-rec-'));
const chrome = spawn('google-chrome', [
  '--headless=new', `--remote-debugging-port=${PORT}`, `--user-data-dir=${profile}`,
  '--no-first-run', '--hide-scrollbars', '--force-color-profile=srgb', 'about:blank',
], { stdio: 'ignore' });

let ws, id = 0;
const pending = new Map();
const frames = []; // { file, ts }
const send = (method, params = {}) => new Promise((resolve, reject) => {
  const msgId = ++id;
  pending.set(msgId, { resolve, reject });
  ws.send(JSON.stringify({ id: msgId, method, params }));
});
async function js(expr) {
  const r = await send('Runtime.evaluate', { expression: `(async () => { ${expr} })()`, awaitPromise: true, returnByValue: true });
  if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description || 'eval failed');
  return r.result.value;
}
const api = async (path, method = 'GET') => {
  const r = await fetch(APP + path, { method });
  if (!r.ok) throw new Error(`${method} ${path} → ${r.status}`);
};

// Recording-only overlay: caption bar + click ring (injected at runtime, not part of the app).
const OVERLAY = `
  if (!document.getElementById('rec-cap')) {
    const st = document.createElement('style');
    st.textContent = \`
      body { padding-bottom: 84px; }
      .modal, .panel, .backdrop { bottom: 84px !important; }
      .toast { bottom: 104px !important; }
      #rec-bar { position: fixed; left: 0; right: 0; bottom: 0; height: 84px; z-index: 9999; display: grid; place-items: center;
        background: #03060f; border-top: 2px solid rgba(96,165,250,.55); }
      #rec-cap { color: #fff; font: 600 23px/1.3 system-ui, sans-serif; text-align: center; padding: 0 32px;
        transition: opacity .25s; }
      .rec-ring { position: fixed; z-index: 9998; width: 56px; height: 56px; margin: -28px 0 0 -28px; border-radius: 50%;
        border: 4px solid #60a5fa; background: rgba(96,165,250,.25); pointer-events: none;
        animation: rec-ring .7s ease-out forwards; }
      @keyframes rec-ring { from { transform: scale(.4); opacity: 1; } to { transform: scale(1.4); opacity: 0; } }\`;
    document.head.appendChild(st);
    const bar = document.createElement('div'); bar.id = 'rec-bar';
    const c = document.createElement('div'); c.id = 'rec-cap'; c.style.opacity = 0;
    bar.appendChild(c); document.body.appendChild(bar);
  }`;
const caption = (text) => js(`${OVERLAY}; const c = document.getElementById('rec-cap');
  c.style.opacity = ${text ? 1 : 0}; ${text ? `c.textContent = ${JSON.stringify(text)};` : ''}`);
const tap = (sel) => js(`${OVERLAY};
  const el = document.querySelector(${JSON.stringify(sel)}); const r = el.getBoundingClientRect();
  const ring = document.createElement('div'); ring.className = 'rec-ring';
  ring.style.left = (r.left + r.width / 2) + 'px'; ring.style.top = (r.top + r.height / 2) + 'px';
  document.body.appendChild(ring); setTimeout(() => ring.remove(), 800);
  await new Promise(r => setTimeout(r, 250)); el.click();`);

try {
  let target;
  for (let i = 0; i < 40 && !target; i++) {
    try { target = await (await fetch(`http://127.0.0.1:${PORT}/json/new?about:blank`, { method: 'PUT' })).json(); }
    catch { await sleep(250); }
  }
  ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((r) => ws.addEventListener('open', r));
  ws.addEventListener('message', (e) => {
    const m = JSON.parse(e.data);
    if (m.id && pending.has(m.id)) {
      const p = pending.get(m.id); pending.delete(m.id);
      m.error ? p.reject(new Error(m.error.message)) : p.resolve(m.result);
    } else if (m.method === 'Page.screencastFrame') {
      const file = `f${String(frames.length).padStart(5, '0')}.jpg`;
      writeFileSync(join(FRAMES, file), Buffer.from(m.params.data, 'base64'));
      frames.push({ file, ts: m.params.metadata.timestamp });
      send('Page.screencastFrameAck', { sessionId: m.params.sessionId });
    }
  });
  await send('Page.enable');
  await send('Emulation.setDeviceMetricsOverride', { width: W, height: H, deviceScaleFactor: 1, mobile: false });

  await api('/api/scenarios/reset', 'POST');
  await send('Page.navigate', { url: APP + '/' });
  await sleep(2500);
  await caption('');

  await send('Page.startScreencast', { format: 'jpeg', quality: 92, maxWidth: W, maxHeight: H, everyNthFrame: 1 });
  const t0 = Date.now();

  await caption('Each astronaut is compared with their own pre-flight baseline');
  await sleep(3000);
  await tap('[data-crew="navarro"]');
  await caption("Dr. Navarro's heart-rate variability fell 3 days in a row → AMBER");
  await sleep(3200);

  await tap('[data-view="demo"]');
  await caption('Replay a real solar particle event from NASA DONKI (September 2017)');
  await sleep(1600);
  await tap('[data-scenario="solar-storm"]');
  await sleep(1200);
  await caption('Radiation turns RED for the whole crew');
  await sleep(2800);

  await tap('[data-system="RADIATION"]');
  await caption('Every alert becomes an action card — with its NASA source');
  await sleep(2600);
  await js(`document.getElementById('modal-card').scrollTo({ top: 99999, behavior: 'smooth' }); await new Promise(r => setTimeout(r, 1200));`);
  await caption('On the ISS: call the flight surgeon on the live link');
  await sleep(2600);

  await caption('Now switch to Mars transit — 12 minutes each way');
  await sleep(1200);
  await tap('[data-env="MARS"]');
  await sleep(900);
  await caption('The card now tells the crew to act first and message ground in parallel');
  await sleep(4200);

  await tap('#modal-card [data-close="modal"]');
  await caption('Crew Health Assistant — personal baselines, explainable rules, mission-aware escalation');
  await sleep(3800);

  await send('Page.stopScreencast');
  await sleep(300);
  const end = frames.at(-1).ts + 0.5;
  // ffmpeg concat list with per-frame durations from screencast timestamps
  let list = '';
  frames.forEach((f, i) => {
    const next = i + 1 < frames.length ? frames[i + 1].ts : end;
    list += `file '${f.file}'\nduration ${Math.max(next - f.ts, 0.001).toFixed(4)}\n`;
  });
  list += `file '${frames.at(-1).file}'\n`;
  writeFileSync(join(FRAMES, 'frames.txt'), list);
  console.log(`frames=${frames.length} seconds=${((Date.now() - t0) / 1000).toFixed(1)}`);

  await api('/api/scenarios/reset', 'POST');
} catch (e) {
  console.error('FAILED:', e.message);
  process.exitCode = 1;
} finally {
  ws?.close();
  chrome.kill();
}
