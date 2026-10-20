const API_BASE = (document.body.dataset.apiBase || "/api/admin").replace(/\/$/, "");

let zonesCache = [];
let hallsCache = [];

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

function readHallActive(h) {
  if (h == null) return true;
  if (typeof h.is_active === "boolean") return h.is_active;
  if (typeof h.isActive === "boolean") return h.isActive;
  if (typeof h.active === "boolean") return h.active;
  return true;
}

function zoneNameById(zoneId) {
  if (zoneId == null) return "—";
  const z = zonesCache.find((x) => x.zone_id === zoneId);
  return z ? z.zone_name : String(zoneId);
}

function setHallFormError(msg) {
  const box = document.getElementById("hallFormError");
  if (!box) return;
  box.style.display = msg ? "block" : "none";
  box.textContent = msg || "";
}

function fullDescriptionShort(desc) {
  const s = String(desc ?? "");
  return s.length > 60 ? `${s.slice(0, 60)}...` : s;
}

function renderHalls(list) {
  const tbody = document.getElementById("halls-tbody");
  if (!tbody) return;
  tbody.innerHTML = "";

  if (!list.length) {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td colspan="6" class="muted">Ничего не найдено</td>`;
    tbody.appendChild(tr);
    return;
  }

  for (const h of list) {
    const zoneLabel = h.zone_name != null && h.zone_name !== "" ? h.zone_name : zoneNameById(h.zone_id);
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc(h.hall_name)}</td>
      <td>${esc(zoneLabel)}</td>
      <td>${esc(h.capacity)}</td>
      <td>${esc(fullDescriptionShort(h.description))}</td>
      <td>${esc(readHallActive(h) ? "Да" : "Нет")}</td>
      <td>
        <div class="admin-actions" style="justify-content:flex-start;">
          <button class="admin-btn secondary" type="button" data-action="edit-hall" data-id="${esc(h.hall_id)}">Редактировать</button>
          <button class="admin-btn danger" type="button" data-action="delete-hall" data-id="${esc(h.hall_id)}">Удалить</button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  }
}

function fillHallZoneSelect() {
  const sel = document.getElementById("hall_zone_id");
  if (!sel) return;
  const opt = (v, text) => `<option value="${v}">${esc(text)}</option>`;
  sel.innerHTML =
    '<option value="">— Не привязана —</option>' +
    zonesCache.map((z) => opt(z.zone_id, z.zone_name)).join("");
}

function getHallFormPayload() {
  const zoneVal = document.getElementById("hall_zone_id").value;
  return {
    hall_name: document.getElementById("hall_name").value.trim(),
    zone_id: zoneVal ? Number(zoneVal) : null,
    capacity: Number(document.getElementById("capacity").value),
    description: document.getElementById("hall_description").value.trim() || null,
    is_active: document.getElementById("hall_is_active").value === "true",
  };
}

function setHallFormData(h) {
  document.getElementById("hall_id").value = h?.hall_id ?? "";
  document.getElementById("hall_name").value = h?.hall_name ?? "";
  const zid = h?.zone_id;
  document.getElementById("hall_zone_id").value = zid != null && zid !== "" ? String(zid) : "";
  document.getElementById("capacity").value = h?.capacity ?? "";
  document.getElementById("hall_description").value = h?.description ?? "";
  document.getElementById("hall_is_active").value = String(readHallActive(h));
}

function clearHallForm() {
  document.getElementById("hall_id").value = "";
  document.getElementById("hall_name").value = "";
  document.getElementById("hall_zone_id").value = "";
  document.getElementById("capacity").value = "";
  document.getElementById("hall_description").value = "";
  document.getElementById("hall_is_active").value = "true";
}

function applyHallSearch() {
  const qName = document.getElementById("q-hall-name").value.trim().toLowerCase();
  const qActive = document.getElementById("q-hall-is_active").value;
  const filtered = hallsCache.filter((h) => {
    const okName = !qName || (h.hall_name || "").toLowerCase().includes(qName);
    const okActive = !qActive || String(readHallActive(h)) === qActive;
    return okName && okActive;
  });
  renderHalls(filtered);
}

async function loadZonesFromApi() {
  zonesCache = await apiRequest("/zones");
  if (!Array.isArray(zonesCache)) zonesCache = [];
  fillHallZoneSelect();
}

async function loadHallsFromApi() {
  hallsCache = await apiRequest("/halls");
  if (!Array.isArray(hallsCache)) hallsCache = [];
  applyHallSearch();
}

document.addEventListener("DOMContentLoaded", () => {
  (async () => {
    try {
      await loadZonesFromApi();
      await loadHallsFromApi();
    } catch (e) {
      const htb = document.getElementById("halls-tbody");
      if (htb) htb.innerHTML = `<tr><td colspan="6" class="error-message">${esc(e.fields ? formatApiErrors(e.fields) : e.message || "Ошибка загрузки")}</td></tr>`;
    }
  })();

  document.getElementById("btn-hall-search")?.addEventListener("click", applyHallSearch);
  document.getElementById("btn-clear-hall-search")?.addEventListener("click", () => {
    document.getElementById("q-hall-name").value = "";
    document.getElementById("q-hall-is_active").value = "";
    applyHallSearch();
  });

  document.getElementById("halls-tbody")?.addEventListener("click", async (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;
    const action = btn.dataset.action;
    const id = Number(btn.dataset.id);
    const item = hallsCache.find((x) => x.hall_id === id);
    if (!item) return;

    if (action === "edit-hall") {
      setHallFormData(item);
      setHallFormError("");
      document.getElementById("hallForm")?.scrollIntoView({ behavior: "smooth", block: "start" });
      return;
    }

    if (action === "delete-hall") {
      if (!confirm(`Удалить зал ${item.hall_name}?`)) return;
      try {
        await apiRequest(`/halls/${id}`, { method: "DELETE" });
        if (Number(document.getElementById("hall_id").value) === id) clearHallForm();
        await loadHallsFromApi();
      } catch (err) {
        alert(formatApiErrors(err.fields) || err.message);
      }
    }
  });

  document.getElementById("btn-reset-hall")?.addEventListener("click", () => {
    clearHallForm();
    setHallFormError("");
  });

  document.getElementById("hallForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    setHallFormError("");
    const form = e.target;
    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }
    const hallIdVal = document.getElementById("hall_id").value;
    const payload = getHallFormPayload();
    try {
      let saved;
      if (!hallIdVal) {
        saved = await apiRequest("/halls", { method: "POST", body: JSON.stringify(payload) });
      } else {
        saved = await apiRequest(`/halls/${Number(hallIdVal)}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        });
      }
      setHallFormData(saved);
      await loadHallsFromApi();
    } catch (err) {
      setHallFormError(formatApiErrors(err.fields) || err.message);
    }
  });
});
