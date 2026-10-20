const API_BASE = (document.body.dataset.apiBase || "/api/client").replace(/\/$/, "");
let services = [];

function escapeHtml(s) {
  return String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#039;");
}

function renderCard(svc) {
  const el = document.createElement("article");
  el.className = "card service-card";
  el.dataset.name = svc.service_name.toLowerCase();
  el.dataset.type = svc.service_type;
  el.dataset.zone = svc.zone_name;

  el.innerHTML = `
    <div class="service-top">
      <div>
        <div class="service-name">${escapeHtml(svc.service_name)}</div>
        <div class="muted">${escapeHtml(svc.zone_name)} • ${escapeHtml(svc.service_type)} • ${escapeHtml(String(svc.duration_minutes))} мин</div>
      </div>
    </div>

    <div class="service-desc">${escapeHtml(svc.description)}</div>
  `;

  return el;
}

function apply() {
  const q = document.getElementById("q").value.trim().toLowerCase();
  const type = document.getElementById("type").value;
  const zone = document.getElementById("zone").value;

  const filtered = services.filter((svc) =>
    (!q || String(svc.service_name || "").toLowerCase().includes(q)) &&
    (!type || svc.service_type === type) &&
    (!zone || svc.zone_name === zone)
  );

  const grid = document.getElementById("grid");
  grid.innerHTML = "";
  for (const svc of filtered) grid.appendChild(renderCard(svc));

  const isEmpty = filtered.length === 0;
  document.getElementById("empty").hidden = !isEmpty;
  grid.hidden = isEmpty;
}

function clearAll() {
  document.getElementById("q").value = "";
  document.getElementById("type").value = "";
  document.getElementById("zone").value = "";
  apply();
}

function init() {
  fetch(`${API_BASE}/training-types`)
    .then((r) => r.json())
    .then((list) => {
      services = Array.isArray(list) ? list : [];
      const zones = [...new Set(services.map((x) => x.zone_name).filter(Boolean))].sort((a, b) => a.localeCompare(b, "ru"));
      const zoneSel = document.getElementById("zone");
      zoneSel.innerHTML = '<option value="">Все</option>' + zones.map((z) => `<option value="${escapeHtml(z)}">${escapeHtml(z)}</option>`).join("");
      const grid = document.getElementById("grid");
      grid.innerHTML = "";
      for (const s of services) grid.appendChild(renderCard(s));
      apply();
    });

  document.getElementById("btn-find").addEventListener("click", apply);
  document.getElementById("btn-clear").addEventListener("click", clearAll);
}

document.addEventListener("DOMContentLoaded", init);

