// App state, data refresh and event wiring.
import { api } from './api.js';
import * as R from './render.js';
import { sparkline, readingsChart, destroyCharts, legendHtml } from './charts.js';

const POLL_MS = 10_000;
const RETRY_MS = 5_000;

const state = {
  mission: null,
  crew: [],
  selectedId: null,
  modules: [],
  alerts: [],
  view: 'status',
  openAlertId: null, // action card shown in the modal
  openSystem: null, // module shown in the slide-over panel
};

const $ = (sel) => document.querySelector(sel);
const rendered = {}; // last rendered JSON per region, to skip no-op re-renders
let requestSeq = 0;
let retryTimer = null;

// ---------------------------------------------------------------- data
async function refreshAll() {
  const seq = ++requestSeq;
  try {
    let mission, crew, modules, alerts;
    if (state.selectedId) {
      [mission, crew, modules, alerts] = await Promise.all([
        api.mission(), api.crew(), api.modules(state.selectedId), api.alerts(state.selectedId)]);
    } else {
      [mission, crew] = await Promise.all([api.mission(), api.crew()]);
      state.selectedId = crew[0]?.id ?? null;
      [modules, alerts] = await Promise.all([api.modules(state.selectedId), api.alerts(state.selectedId)]);
    }
    if (seq !== requestSeq) return; // a newer refresh has started; drop stale data
    Object.assign(state, { mission, crew, modules, alerts });
    $('#error-banner').hidden = true;
    clearTimeout(retryTimer);
    retryTimer = null;
    renderAll();
  } catch (err) {
    if (seq !== requestSeq) return;
    console.warn('Refresh failed:', err.message);
    $('#error-banner').hidden = false;
    if (!retryTimer) retryTimer = setTimeout(() => { retryTimer = null; refreshAll(); }, RETRY_MS);
  }
}

// ---------------------------------------------------------------- render
function changed(region, data) {
  const json = JSON.stringify(data);
  if (rendered[region] === json) return false;
  rendered[region] = json;
  return true;
}

function renderAll() {
  renderHeader();
  renderCrew();
  renderTiles();
  renderAlerts();
  renderModal();
  renderTrendSelectors();
}

function renderHeader() {
  const m = state.mission;
  document.querySelectorAll('[data-env]').forEach((b) =>
    b.setAttribute('aria-checked', String(b.dataset.env === m.env)));
  const day = $('#mission-day');
  day.classList.remove('skeleton-text');
  day.textContent = `Day ${m.missionDay} of ${m.missionLengthDays}`;
  const comms = $('#comms-delay');
  comms.classList.remove('skeleton-text');
  comms.textContent = `Comms delay: ${R.commsText(m.commsDelaySeconds)}`;
  const sw = m.spaceWeather;
  const badge = $('#space-weather');
  if (sw?.activeSpe) {
    const t = sw.event?.eventTime ? sw.event.eventTime.replace('T', ' ').replace('Z', ' UTC') : '';
    badge.className = 'weather-badge spe';
    badge.innerHTML = `☀ SOLAR PARTICLE EVENT <small>${R.esc(t)} · Source: NASA DONKI</small>`;
  } else {
    badge.className = 'weather-badge quiet';
    badge.textContent = 'Space weather: quiet';
  }
}

function renderCrew() {
  if (!changed('crew', [state.crew, state.selectedId])) return;
  $('#crew-strip').innerHTML = state.crew.map((c) => R.crewCard(c, c.id === state.selectedId)).join('');
}

function sortedModules() {
  return [...state.modules].sort((a, b) => R.SYSTEM_ORDER.indexOf(a.bodySystem) - R.SYSTEM_ORDER.indexOf(b.bodySystem));
}

function renderTiles() {
  if (!changed('tiles', state.modules)) return;
  const grid = $('#tile-grid');
  destroyCharts(grid);
  const mods = sortedModules();
  grid.innerHTML = mods.map(R.tile).join('');
  mods.filter((m) => m.indicators).forEach((m) => {
    const w = R.worstIndicator(m);
    sparkline(grid.querySelector(`canvas[data-spark="${m.bodySystem}"]`), w.last7, w.status);
  });
}

const crewName = (id) => (id ? state.crew.find((c) => c.id === id)?.name ?? id : 'Whole crew');

function renderAlerts() {
  const open = state.alerts.filter((a) => !a.acknowledged).length;
  const badge = $('#alert-count');
  badge.textContent = open;
  badge.hidden = open === 0;
  $('#alerts-for').textContent = `· ${crewName(state.selectedId)} + whole crew`;
  if (!changed('alerts', [state.alerts, state.crew])) return;
  $('#alert-list').innerHTML = state.alerts.length
    ? state.alerts.map((a) => R.alertRow(a, crewName(a.crewId))).join('')
    : '<div class="empty">✓ No alerts. All indicators within personal baseline.</div>';
}

// ---------------------------------------------------------------- action card modal
function openActionCard(id) {
  state.openAlertId = id;
  rendered.modal = null;
  renderModal();
  const modal = $('#modal');
  modal.classList.add('open');
  modal.setAttribute('aria-hidden', 'false');
  $('#modal-card').querySelector('[data-close="modal"]')?.focus();
}

function closeModal() {
  state.openAlertId = null;
  rendered.modal = null;
  $('#modal').classList.remove('open');
  $('#modal').setAttribute('aria-hidden', 'true');
}

function renderModal() {
  if (!state.openAlertId) return;
  const a = state.alerts.find((x) => x.id === state.openAlertId);
  if (!a) { closeModal(); return; } // resolved (e.g. after reset)
  const prevEscalation = $('#ac-escalation .ac-escalation-text')?.textContent;
  if (!changed('modal', [a, state.mission, state.crew])) return;
  $('#modal-card').innerHTML = R.actionCard(a, state.mission, crewName(a.crewId));
  const box = $('#ac-escalation');
  if (prevEscalation && prevEscalation !== a.escalationText && box) {
    box.classList.add('flash'); // highlight the live mission-context change
    setTimeout(() => box.classList.remove('flash'), 700);
  }
}

async function acknowledge(id, btn) {
  btn.disabled = true;
  try {
    await api.ack(id);
    await refreshAll();
  } catch (e) {
    btn.disabled = false;
    toast('Could not acknowledge — try again.');
  }
}

// ---------------------------------------------------------------- module panel
function alertsForModule(mod) {
  if (mod.bodySystem === 'RADIATION') return state.alerts.filter((a) => a.indicator === 'RADIATION');
  const codes = new Set(mod.indicators.map((i) => i.code));
  return state.alerts.filter((a) => a.crewId === state.selectedId && codes.has(a.indicator));
}

function onTile(system) {
  const mod = state.modules.find((m) => m.bodySystem === system);
  if (!mod) return;
  const radAlert = system === 'RADIATION' ? alertsForModule(mod)[0] : null;
  if (radAlert) openActionCard(radAlert.id);
  else openPanel(mod);
}

function openPanel(mod) {
  state.openSystem = mod.bodySystem;
  const body = $('#panel-body');
  destroyCharts(body);
  $('#panel-title').innerHTML = `${R.SYSTEMS[mod.bodySystem]?.icon ?? ''} ${R.systemTitle(mod.bodySystem)} · ${R.esc(crewName(state.selectedId))}`;
  body.innerHTML = R.modulePanel(mod, alertsForModule(mod));
  $('#panel').classList.add('open');
  $('#panel').setAttribute('aria-hidden', 'false');
  $('#backdrop').hidden = false;
  const id = state.selectedId;
  body.querySelectorAll('canvas[data-readings]').forEach(async (canvas) => {
    const code = canvas.dataset.readings;
    try {
      const r = await api.readings(id, code, 30);
      if (!canvas.isConnected) return;
      body.querySelector(`[data-legend="${code}"]`).innerHTML = legendHtml(r);
      readingsChart(canvas, r);
    } catch (e) {
      canvas.parentElement.textContent = 'Chart unavailable.';
    }
  });
}

function closePanel() {
  state.openSystem = null;
  $('#panel').classList.remove('open');
  $('#panel').setAttribute('aria-hidden', 'true');
  $('#backdrop').hidden = true;
}

// ---------------------------------------------------------------- trends
function renderTrendSelectors() {
  const crewSel = $('#trend-crew');
  const indSel = $('#trend-indicator');
  const inds = sortedModules().flatMap((m) => m.indicators || []);
  if (!changed('trendSel', [state.crew.map((c) => c.id), inds.map((i) => i.code)])) return;
  const prevCrew = crewSel.value || state.selectedId;
  const prevInd = indSel.value;
  crewSel.innerHTML = state.crew.map((c) => `<option value="${R.esc(c.id)}">${R.esc(c.name)}</option>`).join('');
  indSel.innerHTML = inds.map((i) => `<option value="${R.esc(i.code)}">${R.esc(i.name)}</option>`).join('');
  if (prevCrew) crewSel.value = prevCrew;
  if (prevInd) indSel.value = prevInd;
}

async function drawTrend() {
  const crewId = $('#trend-crew').value;
  const code = $('#trend-indicator').value;
  if (!crewId || !code) return;
  try {
    const r = await api.readings(crewId, code, 30);
    const canvas = $('#trend-chart');
    destroyCharts(canvas.parentElement);
    $('#trend-legend').innerHTML = legendHtml(r);
    readingsChart(canvas, r);
  } catch (e) {
    toast('Could not load readings.');
  }
}

// ---------------------------------------------------------------- navigation, demo, toast
function setView(view) {
  state.view = view;
  document.querySelectorAll('.nav-item').forEach((b) => {
    b.classList.toggle('active', b.dataset.view === view);
    b.toggleAttribute('aria-current', b.dataset.view === view);
  });
  document.querySelectorAll('.view').forEach((v) => v.classList.toggle('active', v.id === `view-${view}`));
  if (view === 'trends') {
    if (!$('#trend-crew').value && state.selectedId) $('#trend-crew').value = state.selectedId;
    drawTrend();
  }
  window.scrollTo({ top: 0 });
}

const SCENARIO_TOAST = {
  'solar-storm': 'Replaying real solar particle event from NASA DONKI (Sep 2017)',
  'sleep-deficit': 'Sleep deficit applied: 3 short nights and low mood for the commander',
  reset: 'Simulation reset: ISS, day 30, quiet space weather',
};

async function runScenario(name, btn) {
  document.querySelectorAll('[data-scenario]').forEach((b) => { b.disabled = true; });
  try {
    await api.scenario(name);
    if (name === 'sleep-deficit') state.selectedId = state.crew[0]?.id ?? state.selectedId;
    await refreshAll();
    toast(SCENARIO_TOAST[name]);
    if (name === 'solar-storm' || name === 'sleep-deficit') setView('status');
    if (state.view === 'trends') drawTrend();
  } catch (e) {
    toast(`Scenario failed: ${e.message}`);
  } finally {
    document.querySelectorAll('[data-scenario]').forEach((b) => { b.disabled = false; });
  }
}

let toastTimer;
function toast(msg) {
  const t = $('#toast');
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.remove('show'), 3500);
}

async function setContext(env, btn) {
  if (state.mission?.env === env) return;
  document.querySelectorAll('[data-env]').forEach((b) => b.setAttribute('aria-checked', String(b === btn)));
  try {
    await api.setContext(env);
  } catch (e) {
    toast('Could not change mission context.');
  }
  await refreshAll();
}

function selectCrew(id) {
  if (id === state.selectedId) return;
  state.selectedId = id;
  renderCrew();
  refreshAll();
}

// ---------------------------------------------------------------- events
document.addEventListener('click', (e) => {
  const el = e.target.closest('button, #backdrop, #modal');
  if (!el) return;
  const d = el.dataset;
  if (el.id === 'modal' && e.target === el) closeModal();
  else if (el.id === 'backdrop') closePanel();
  else if (d.close === 'modal') closeModal();
  else if (d.close === 'panel') closePanel();
  else if (d.ack) acknowledge(d.ack, el);
  else if (d.alert) openActionCard(d.alert);
  else if (d.crew) selectCrew(d.crew);
  else if (d.system) onTile(d.system);
  else if (d.env) setContext(d.env, el);
  else if (d.view) setView(d.view);
  else if (d.scenario) runScenario(d.scenario, el);
});

document.addEventListener('keydown', (e) => {
  if (e.key !== 'Escape') return;
  if (state.openAlertId) closeModal();
  else if (state.openSystem) closePanel();
});

$('#trend-crew').addEventListener('change', drawTrend);
$('#trend-indicator').addEventListener('change', drawTrend);

// Keep --header-h in sync so the panel and modal open below the (always visible) mission header.
const header = $('.mission-header');
new ResizeObserver(() => document.documentElement.style.setProperty('--header-h', `${header.offsetHeight}px`)).observe(header);

refreshAll();
setInterval(refreshAll, POLL_MS);
