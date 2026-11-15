// Chart.js helpers: tiny sparklines and the 30-day readings chart.
/* global Chart */

const COLORS = { GREEN: '#22c55e', AMBER: '#f59e0b', RED: '#ef4444' };
const LINE = '#60a5fa';

if (window.Chart) {
  Chart.defaults.color = '#8f9bb6';
  Chart.defaults.borderColor = 'rgba(143,155,182,.15)';
  Chart.defaults.font.family = 'system-ui, -apple-system, "Segoe UI", Roboto, sans-serif';
}

/** Destroy every chart whose canvas lives inside `root`. */
export function destroyCharts(root) {
  if (!window.Chart || !root) return;
  root.querySelectorAll('canvas').forEach((c) => Chart.getChart(c)?.destroy());
}

export function sparkline(canvas, points, status) {
  if (!window.Chart || !canvas) return;
  const color = COLORS[status] || LINE;
  new Chart(canvas, {
    type: 'line',
    data: {
      labels: points.map((p) => p.date),
      datasets: [{
        data: points.map((p) => p.value), borderColor: color, borderWidth: 2,
        pointRadius: 0, tension: 0.35, fill: { target: 'origin' }, backgroundColor: color + '22',
      }],
    },
    options: {
      responsive: true, maintainAspectRatio: false, animation: false,
      plugins: { legend: { display: false }, tooltip: { enabled: false } },
      scales: { x: { display: false }, y: { display: false, grace: '10%' } },
      events: [],
    },
  });
}

/** Full readings chart: value line, shaded personal baseline band, dashed red absolute limit. */
export function readingsChart(canvas, r) {
  if (!window.Chart || !canvas) return;
  const labels = r.series.map((p) => p.date.slice(5));
  const n = labels.length;
  const datasets = [
    { label: 'Band low', data: Array(n).fill(r.bandLow), borderWidth: 0, pointRadius: 0, fill: false },
    {
      label: 'Personal baseline band', data: Array(n).fill(r.bandHigh), borderWidth: 0, pointRadius: 0,
      fill: '-1', backgroundColor: 'rgba(34,197,94,.14)',
    },
    {
      label: 'Baseline', data: Array(n).fill(r.baseline), borderColor: 'rgba(34,197,94,.6)',
      borderWidth: 1, borderDash: [2, 3], pointRadius: 0,
    },
    {
      label: r.indicator, data: r.series.map((p) => p.value), borderColor: LINE, backgroundColor: LINE,
      borderWidth: 2.5, pointRadius: 2, pointHoverRadius: 5, tension: 0.25,
    },
  ];
  if (r.absoluteLimit != null) {
    datasets.push({
      label: 'Safety limit', data: Array(n).fill(r.absoluteLimit), borderColor: COLORS.RED,
      borderWidth: 2, borderDash: [6, 5], pointRadius: 0,
    });
  }
  new Chart(canvas, {
    type: 'line',
    data: { labels, datasets },
    options: {
      responsive: true, maintainAspectRatio: false, animation: { duration: 200 },
      interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: { display: false },
        tooltip: {
          filter: (item) => item.dataset.label !== 'Band low',
          callbacks: { label: (c) => ` ${c.dataset.label}: ${c.formattedValue} ${r.unit}` },
        },
      },
      scales: {
        x: { ticks: { maxTicksLimit: 8 } },
        y: { grace: '8%', title: { display: true, text: r.unit } },
      },
    },
  });
}

export function legendHtml(r) {
  return `<span><i style="background:#60a5fa"></i>Reading</span>
    <span><i style="background:rgba(34,197,94,.35)"></i>Your baseline ±band (${r.bandLow}–${r.bandHigh})</span>
    ${r.absoluteLimit != null ? `<span><i style="background:repeating-linear-gradient(90deg,#ef4444 0 5px,transparent 5px 8px)"></i>Safety limit ${r.absoluteLimit}</span>` : ''}`;
}
