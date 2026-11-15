// Capture the README screenshots of the running app (headless Chrome, DevTools protocol).
// Needs: app on :8080, google-chrome, Node 22+.   Usage: node tools/screenshots.mjs docs/screenshots
import { spawn } from 'node:child_process';
import { writeFileSync, mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const OUT = process.argv[2];
const APP = 'http://localhost:8080';
const PORT = 9333;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const profile = mkdtempSync(join(process.argv[3] || tmpdir(), 'chrome-'));
const chrome = spawn('google-chrome', [
  '--headless=new', `--remote-debugging-port=${PORT}`, `--user-data-dir=${profile}`,
  '--no-first-run', '--hide-scrollbars', '--force-color-profile=srgb', 'about:blank',
], { stdio: 'ignore' });

async function api(path, method = 'GET') {
  const r = await fetch(APP + path, { method });
  if (!r.ok) throw new Error(`${method} ${path} → ${r.status}`);
}

let ws, id = 0;
const pending = new Map();
function send(method, params = {}) {
  return new Promise((resolve, reject) => {
    const msgId = ++id;
    pending.set(msgId, { resolve, reject });
    ws.send(JSON.stringify({ id: msgId, method, params }));
  });
}
async function js(expr) {
  const r = await send('Runtime.evaluate', { expression: `(async () => { ${expr} })()`, awaitPromise: true, returnByValue: true });
  if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description || 'eval failed');
  return r.result.value;
}
async function viewport(width, height, mobile = false) {
  await send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile });
}
async function load() {
  await send('Page.navigate', { url: APP + '/' });
  await sleep(2200);
}
async function shot(name) {
  await sleep(500);
  const { data } = await send('Page.captureScreenshot', { format: 'png' });
  writeFileSync(join(OUT, name), Buffer.from(data, 'base64'));
  console.log('saved', name);
}
const click = (sel) => js(`document.querySelector(${JSON.stringify(sel)}).click(); await new Promise(r => setTimeout(r, 900));`);

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
    }
  });
  await send('Page.enable');
  await send('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-reduced-motion', value: 'reduce' }] });

  // 1. Baseline status (ISS, all nominal except Navarro's HRV trend)
  await api('/api/scenarios/reset', 'POST');
  await viewport(1280, 800);
  await load();
  await shot('status.png');

  // 2. Module panel with 30-day chart (Navarro, cardiovascular)
  await click('[data-crew="navarro"]');
  await click('[data-system="CARDIOVASCULAR"]');
  await sleep(800);
  await shot('module-panel.png');

  // 3. Solar storm: red radiation tile + header badge
  await api('/api/scenarios/solar-storm', 'POST');
  await load();
  await shot('solar-storm.png');
  await viewport(1280, 1240);
  await load();

  // 4. Action card after switching to Mars (escalation text + comms delay)
  await click('[data-system="RADIATION"]');
  await click('[data-env="MARS"]');
  await sleep(600);
  await shot('action-card-mars.png');

  // 5. Phone layout
  await viewport(390, 844, true);
  await load();
  await shot('phone.png');

  await api('/api/scenarios/reset', 'POST');
} catch (e) {
  console.error('FAILED:', e.message);
  process.exitCode = 1;
} finally {
  ws?.close();
  chrome.kill();
}
