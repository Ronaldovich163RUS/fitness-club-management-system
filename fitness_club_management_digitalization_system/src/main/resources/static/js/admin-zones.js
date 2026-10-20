const API_BASE = (document.body.dataset.apiBase || "/api/admin").replace(/\/$/, "");

let zonesCache = [];

function esc(s) {
  return String(s ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#039;");
}

async function apiRequest(path, options = {}) {
  const headers = { ...options.headers };
  if (options.body && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }
  const r = await fetch(`${API_BASE}${path}`, { ...options, headers });
  const text = await r.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch (_) {
      data = { error: text.slice(0, 200) };
    }
  }
  if (!r.ok) {
    const err = new Error(data?.error || r.statusText || "Ошибка запроса");
    err.fields = data?.fields || null;
    err.status = r.status;
    throw err;
  }
  if (r.status === 204) return null;
  return data;
}

function formatApiErrors(fields) {
  if (!fields || typeof fields !== "object") return "";
  return Object.entries(fields)
    .map(([k, v]) => `${k}: ${v}`)
    .join("\n");
}

function setZoneFormError(msg) {
  const box = document.getElementById("zoneFormError");
  if (!box) return;
  box.style.display = msg ? "block" : "none";
  box.textContent = msg || "";
}

function fullDescriptionShort(desc) {
  const s = String(desc ?? "");
  return s.length > 60 ? `${s.slice(0, 60)}...` : s;
}

function renderZones(list) {
  const tbody = document.getElementById("zones-tbody");
  if (!tbody) return;
  tbody.innerHTML = "";

  if (!list.length) {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td colspan="3" class="muted">Ничего не найдено</td>`;
    tbody.appendChild(tr);
    return;
  }

  for (const z of list) {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc(z.zone_name)}</td>
      <td>${esc(fullDescriptionShort(z.description))}</td>
      <td>
        <div class="admin-actions" style="justify-content:flex-start;">
          <button class="admin-btn secondary" type="button" data-action="edit-zone" data-id="${esc(z.zone_id)}">Редактировать</button>
          <button class="admin-btn danger" type="button" data-action="delete-zone" data-id="${esc(z.zone_id)}">Удалить</button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  }
}

function getZoneFormPayload() {
  return {
    zone_name: document.getElementById("zone_name").value.trim(),
    description: document.getElementById("zone_description").value.trim() || null,
  };
}

function setZoneFormData(z) {
  document.getElementById("zone_id").value = z?.zone_id ?? "";
  document.getElementById("zone_name").value = z?.zone_name ?? "";
  document.getElementById("zone_description").value = z?.description ?? "";
}

function clearZoneForm() {
  document.getElementById("zone_id").value = "";
  document.getElementById("zone_name").value = "";
  document.getElementById("zone_description").value = "";
}

function applyZoneSearch() {
  const q = document.getElementById("q-zone-name").value.trim().toLowerCase();
  const filtered = zonesCache.filter((z) => !q || (z.zone_name || "").toLowerCase().includes(q));
  renderZones(filtered);
}

async function loadZonesFromApi() {
  zonesCache = await apiRequest("/zones");
  if (!Array.isArray(zonesCache)) zonesCache = [];
  applyZoneSearch();
}

document.addEventListener("DOMContentLoaded", () => {
  (async () => {
    try {
      await loadZonesFromApi();
    } catch (e) {
      const ztb = document.getElementById("zones-tbody");
      if (ztb) ztb.innerHTML = `<tr><td colspan="3" class="error-message">${esc(e.fields ? formatApiErrors(e.fields) : e.message || "Ошибка загрузки")}</td></tr>`;
    }
  })();

  document.getElementById("btn-zone-search")?.addEventListener("click", applyZoneSearch);
  document.getElementById("btn-clear-zone-search")?.addEventListener("click", () => {
    document.getElementById("q-zone-name").value = "";
    applyZoneSearch();
  });

  document.getElementById("zones-tbody")?.addEventListener("click", async (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;
    const action = btn.dataset.action;
    const id = Number(btn.dataset.id);
    const item = zonesCache.find((x) => x.zone_id === id);
    if (!item) return;

    if (action === "edit-zone") {
      setZoneFormData(item);
      setZoneFormError("");
      document.getElementById("zoneForm")?.scrollIntoView({ behavior: "smooth", block: "start" });
      return;
    }

    if (action === "delete-zone") {
      if (!confirm(`Удалить зону ${item.zone_name}? Связи тренеров с этой зоной будут удалены.`)) return;
      try {
        await apiRequest(`/zones/${id}`, { method: "DELETE" });
        if (Number(document.getElementById("zone_id").value) === id) clearZoneForm();
        await loadZonesFromApi();
      } catch (err) {
        alert(formatApiErrors(err.fields) || err.message);
      }
    }
  });

  document.getElementById("btn-reset-zone")?.addEventListener("click", () => {
    clearZoneForm();
    setZoneFormError("");
  });

  document.getElementById("zoneForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    setZoneFormError("");
    const form = e.target;
    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }
    const zoneIdVal = document.getElementById("zone_id").value;
    const payload = getZoneFormPayload();
    try {
      let saved;
      if (!zoneIdVal) {
        saved = await apiRequest("/zones", { method: "POST", body: JSON.stringify(payload) });
      } else {
        saved = await apiRequest(`/zones/${Number(zoneIdVal)}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        });
      }
      setZoneFormData(saved);
      await loadZonesFromApi();
    } catch (err) {
      setZoneFormError(formatApiErrors(err.fields) || err.message);
    }
  });
});
