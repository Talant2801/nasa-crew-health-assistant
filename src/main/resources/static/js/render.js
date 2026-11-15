// Pure HTML builders. No fetching, no state mutation.

export const SYSTEMS = {
  RADIATION: { label: 'Radiation', icon: '☢️', tag: '(estimate)' },
  CARDIOVASCULAR: { label: 'Cardiovascular', icon: '❤️', tag: '' },
  BONE_MUSCLE: { label: 'Bone & Muscle', icon: '🦴', tag: '' },
  SLEEP: { label: 'Sleep', icon: '🌙', tag: '' },
  MENTAL: { label: 'Mental', icon: '🧠', tag: '(self-check)' },
  EYES: { label: 'Eyes', icon: '👁️', tag: '(self-check)' },
};
export const SYSTEM_ORDER = Object.keys(SYSTEMS);

const STATUS_ICON = { GREEN: '●', AMBER: '▲', RED: '■' };
const TREND = { UP: '↑ rising', DOWN: '↓ falling', FLAT: '→ steady' };
const RANK = { GREEN: 0, AMBER: 1, RED: 2 };

export const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) =>
  ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);

export const num = (v) => (v == null ? '—' : Number.isInteger(v) ? String(v) : (Math.round(v * 10) / 10).toString());

export function pill(status) {
  return `<span class="pill ${status}"><span aria-hidden="true">${STATUS_ICON[status]}</span>${status}</span>`;
}

export function commsText(sec) {
  if (!sec) return 'live';
  if (sec < 60) return `${num(sec)} s`;
  return `${Math.round(sec / 60)} min one-way`;
}

export function systemTitle(bodySystem) {
  const s = SYSTEMS[bodySystem] || { label: bodySystem, icon: '•', tag: '' };
  return `${s.label}${s.tag ? ` <small>${s.tag}</small>` : ''}`;
}

/** Worst indicator in a module (first one wins ties). */
export function worstIndicator(mod) {
  return mod.indicators.reduce((w, i) => (RANK[i.status] > RANK[w.status] ? i : w), mod.indicators[0]);
}

// ---------- Crew strip ----------
export function crewCard(c, selected) {
  return `<button type="button" class="crew-card ${selected ? 'selected' : ''}" data-crew="${esc(c.id)}" aria-pressed="${selected}">
    <div class="crew-top"><span class="crew-name">${esc(c.name)}</span>${pill(c.overallStatus)}</div>
    <div class="crew-role">${esc(c.role)}</div>
    <div class="crew-issue">${c.topIssue ? esc(c.topIssue) : 'All systems within personal baseline.'}</div>
  </button>`;
}

// ---------- Tiles ----------
export function radiationGauge(m) {
  const TRACK = 78; // % of width that represents 0..careerLimit
  const fill = Math.min(m.currentDoseMSv / m.careerLimitMSv, 1) * 100;
  const over = m.projectedDoseMSv > m.careerLimitMSv;
  const marker = over ? 98 : (m.projectedDoseMSv / m.careerLimitMSv) * TRACK;
  const markerLabel = over ? `${num(m.projectedDoseMSv)} mSv · exceeds career limit` : `projected ${num(m.projectedDoseMSv)}`;
  return `<div class="dose" role="img" aria-label="Dose ${num(m.currentDoseMSv)} of ${num(m.careerLimitMSv)} mSv career limit, projected ${num(m.projectedDoseMSv)} mSv">
      <span class="dose-limit-label">limit ${num(m.careerLimitMSv)}</span>
      <div class="dose-track"><div class="dose-fill ${m.status === 'RED' ? 'RED' : ''}" style="width:${fill}%"></div></div>
      <div class="dose-limit"></div>
      <div class="dose-marker ${over ? 'over' : ''}" style="left:${marker}%"><span class="dose-marker-label">${markerLabel}</span></div>
    </div>
    <div class="dose-text">Now: <b>${num(m.currentDoseMSv)} mSv</b> · Projected at mission end: <b>${num(m.projectedDoseMSv)} mSv</b> · Limit: ${num(m.careerLimitMSv)} mSv</div>`;
}

export function tile(mod) {
  const meta = SYSTEMS[mod.bodySystem] || { icon: '•' };
  const head = `<div class="tile-head"><span class="tile-icon" aria-hidden="true">${meta.icon}</span>
      <span class="tile-name">${systemTitle(mod.bodySystem)}</span>${pill(mod.status)}</div>`;
  if (mod.bodySystem === 'RADIATION') {
    return `<button type="button" class="tile ${mod.status} ${mod.activeSpe ? 'pulse' : ''}" data-system="RADIATION">
      ${head}
      ${radiationGauge(mod)}
      <div class="tile-summary">${esc(mod.summary)}</div>
      <div class="dose-model">${esc(mod.model)}</div>
    </button>`;
  }
  const w = worstIndicator(mod);
  return `<button type="button" class="tile ${mod.status}" data-system="${esc(mod.bodySystem)}">
    ${head}
    <div class="tile-value">${num(w.value)} <small>${esc(w.unit)} · ${esc(w.name)}</small></div>
    <div class="tile-summary">${esc(w.summary)}</div>
    <div class="spark"><canvas data-spark="${esc(mod.bodySystem)}" aria-hidden="true"></canvas></div>
  </button>`;
}

// ---------- Alerts ----------
export function alertRow(a, crewName) {
  return `<button type="button" class="alert-row ${a.severity} ${a.acknowledged ? 'acked' : ''}" data-alert="${esc(a.id)}">
    ${pill(a.severity)}
    <div class="alert-main">
      <div class="alert-title">${esc(a.actionCard?.title || a.indicator)}${a.acknowledged ? ' <span class="muted">· acknowledged</span>' : ''}</div>
      <div class="alert-sub">${esc(crewName)} · ${esc(a.summary)}</div>
    </div>
    <span class="chev" aria-hidden="true">›</span>
  </button>`;
}

// ---------- Action card ----------
export function actionCard(a, mission, crewName) {
  const card = a.actionCard || {};
  const urgent = a.severity === 'RED' || card.escalateLevel === 'URGENT';
  const steps = (card.doNow || []).map((s) => `<li>${esc(s)}</li>`).join('');
  return `<div class="ac-banner ${a.severity}">
      <span class="ac-sev-icon" aria-hidden="true">${a.severity === 'RED' ? '⛔' : '⚠️'}</span>
      <div class="ac-banner-text">
        <div class="ac-sev">${a.severity} · ${esc(card.escalateLevel || '')}</div>
        <h2 id="ac-title">${esc(card.title)}</h2>
        <div class="ac-who">${esc(crewName)}</div>
      </div>
      <button type="button" class="icon-btn" data-close="modal" aria-label="Close action card">✕</button>
    </div>
    <div class="ac-body">
      <section class="ac-section"><h3>What's happening</h3>
        <p>${esc(card.what)}</p><p class="ac-summary">${esc(a.summary)}</p></section>
      <section class="ac-section"><h3>Why it matters in space</h3><p>${esc(card.whySpace)}</p></section>
      <section class="ac-section"><h3>Do now</h3><ol class="ac-steps">${steps}</ol></section>
      <section class="ac-section"><h3>Escalation</h3>
        <div class="ac-escalation ${urgent ? 'urgent' : ''}" id="ac-escalation">
          <div class="ac-escalation-text">${esc(a.escalationText)}</div>
          <div class="ac-escalation-meta">${esc(mission.label)} · Comms delay: <b>${commsText(mission.commsDelaySeconds)}</b></div>
        </div>
      </section>
      <div class="ac-source">Source: ${card.sourceUrl
        ? `<a href="${esc(card.sourceUrl)}" target="_blank" rel="noopener noreferrer">${esc(card.source)} ↗</a>`
        : esc(card.source)}</div>
      <div class="ac-actions">
        <button type="button" class="btn" data-close="modal">Close</button>
        <button type="button" class="btn ${a.severity === 'RED' ? 'btn-danger' : 'btn-primary'}" data-ack="${esc(a.id)}" ${a.acknowledged ? 'disabled' : ''}>
          ${a.acknowledged ? 'Acknowledged ✓' : 'Acknowledge'}</button>
      </div>
    </div>`;
}

// ---------- Module panel ----------
function alertButtons(alerts) {
  return alerts.map((a) => `<button type="button" class="btn ${a.severity === 'RED' ? 'btn-danger' : 'btn-primary'}" data-alert="${esc(a.id)}">
      Open action card: ${esc(a.actionCard?.title)}</button>`).join('');
}

export function modulePanel(mod, alerts) {
  if (mod.bodySystem === 'RADIATION') {
    return `${alertButtons(alerts)}
      <div class="ind"><div class="ind-head"><span class="ind-name">Crew radiation dose</span>${pill(mod.status)}</div>
        ${radiationGauge(mod)}
        <div class="ind-stats"><span>Short-term (SPE) limit <b>${num(mod.speLimitMSv)}</b> mSv</span>
          <span>Solar particle event <b>${mod.activeSpe ? 'ACTIVE' : 'none'}</b></span></div>
        <div>${esc(mod.summary)}</div><div class="dose-model">${esc(mod.model)}</div></div>`;
  }
  return alertButtons(alerts) + mod.indicators.map((i) => `<div class="ind">
      <div class="ind-head"><span class="ind-name">${esc(i.name)}</span>${pill(i.status)}</div>
      <div class="ind-stats"><span>Now <b>${num(i.value)}</b> ${esc(i.unit)}</span>
        <span>Baseline <b>${num(i.baseline)}</b></span><span>Trend <b>${TREND[i.trend] || i.trend}</b></span></div>
      <div>${esc(i.summary)}</div>
      <div class="chart-legend" data-legend="${esc(i.code)}"></div>
      <div class="chart-box"><canvas data-readings="${esc(i.code)}"></canvas></div>
    </div>`).join('');
}
