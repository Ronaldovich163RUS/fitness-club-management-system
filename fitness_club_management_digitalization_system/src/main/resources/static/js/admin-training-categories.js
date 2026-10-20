const API_BASE = (document.body.dataset.apiBase || "/api/admin").replace(/\/$/, "");

let servicesCache = [];
let zonesForFilter = [];
let membershipsBrief = [];

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

/** Ответ API: is_active, isActive или active (Jackson для boolean isActive). */
function readServiceActive(s) {
  if (s == null) return false;
  if (typeof s.is_active === "boolean") return s.is_active;
  if (typeof s.isActive === "boolean") return s.isActive;
  if (typeof s.active === "boolean") return s.active;
  return false;
}

function readMembershipIds(s) {
  if (Array.isArray(s.membership_ids)) return s.membership_ids;
  if (Array.isArray(s.membershipIds)) return s.membershipIds;
  return [];
}

function membershipName(id) {
  const m = membershipsBrief.find((x) => (x.membership_id ?? x.membershipId) === id);
  return m?.membership_name ?? m?.membershipName ?? String(id);
}

function zoneName(zoneId) {
  return zonesForFilter.find((z) => z.zone_id === zoneId)?.zone_name || "";
}

function selectedMembershipIds() {
  return [...document.querySelectorAll("#membership-checkboxes input[type='checkbox']:checked")].map((cb) =>
    Number(cb.dataset.mid),
  );
}

function setFormError(msg) {
  const box = document.getElementById("serviceFormError");
  if (!box) return;
  box.style.display = msg ? "block" : "none";
  box.textContent = msg || "";
}

function renderServices(list) {
  const tbody = document.getElementById("services-tbody");
  tbody.innerHTML = "";

  if (!list.length) {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td colspan="7" class="muted">Ничего не найдено</td>`;
    tbody.appendChild(tr);
    return;
  }

  for (const s of list) {
    const mids = s.membership_ids || [];
    const names = mids.map((id) => membershipName(id)).filter(Boolean);
    const includedText = names.length ? `Включено: ${names.slice(0, 3).join(", ")}${names.length > 3 ? "…" : ""}` : "Не включено";

    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>
        <div style="font-weight:950;">${esc(s.service_name)}</div>
        <div class="admin-small">${esc(includedText)}</div>
      </td>
      <td>${esc(s.service_type)}</td>
      <td>${esc(s.duration_minutes)} мин</td>
      <td>${esc(String(s.base_price))} ₽</td>
      <td>${esc(s.zone_name || zoneName(s.zone_id))}</td>
      <td>${esc(readServiceActive(s) ? "Да" : "Нет")}</td>
      <td>
        <div class="admin-actions" style="justify-content:flex-start;">
          <button class="admin-btn secondary" type="button" data-action="edit" data-id="${esc(s.service_id)}">Редактировать</button>
          <button class="admin-btn danger" type="button" data-action="delete" data-id="${esc(s.service_id)}">Удалить</button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  }
}

function renderZoneSelect() {
  const sel = document.getElementById("zone_id");
  if (!sel) return;
  sel.innerHTML = zonesForFilter.map((z) => `<option value="${esc(z.zone_id)}">${esc(z.zone_name)}</option>`).join("");
}

function renderMembershipCheckboxes() {
  const box = document.getElementById("membership-checkboxes");
  if (!box) return;
  box.innerHTML = "";
  for (const m of membershipsBrief) {
    const label = document.createElement("label");
    label.innerHTML = `<input type="checkbox" data-mid="${esc(m.membership_id)}" /> ${esc(m.membership_name)}`;
    box.appendChild(label);
  }
}

function setServiceFormData(s) {
  document.getElementById("service_id").value = s?.service_id ?? "";
  document.getElementById("service_name").value = s?.service_name ?? "";
  document.getElementById("duration_minutes").value = s?.duration_minutes ?? 0;
  document.getElementById("base_price").value = s?.base_price ?? 0;
  document.getElementById("zone_id").value = s?.zone_id ?? zonesForFilter[0]?.zone_id ?? "";
  document.getElementById("service_type").value = s?.service_type ?? "групповая тренировка";
  document.getElementById("description").value = s?.description ?? "";
  document.getElementById("is_active").value = String(readServiceActive(s));

  const linked = new Set(readMembershipIds(s));
  document.querySelectorAll("#membership-checkboxes input[type='checkbox']").forEach((cb) => {
    const mid = Number(cb.dataset.mid);
    cb.checked = linked.has(mid);
  });
}

function clearServiceForm() {
  document.getElementById("service_id").value = "";
  document.getElementById("service_name").value = "";
  document.getElementById("duration_minutes").value = 0;
  document.getElementById("base_price").value = 0;
  document.getElementById("zone_id").value = zonesForFilter[0]?.zone_id ?? "";
  document.getElementById("service_type").value = "групповая тренировка";
  document.getElementById("description").value = "";
  document.getElementById("is_active").value = "true";
  document.querySelectorAll("#membership-checkboxes input[type='checkbox']").forEach((cb) => (cb.checked = false));
}

function getServiceFormPayload() {
  return {
    service_name: document.getElementById("service_name").value.trim(),
    duration_minutes: Number(document.getElementById("duration_minutes").value),
    base_price: Number(document.getElementById("base_price").value),
    zone_id: Number(document.getElementById("zone_id").value),
    service_type: document.getElementById("service_type").value,
    description: document.getElementById("description").value.trim(),
    is_active: document.getElementById("is_active").value === "true",
    membership_ids: selectedMembershipIds(),
  };
}

function applySearch() {
  const qName = document.getElementById("q-service-name").value.trim().toLowerCase();
  const qZone = document.getElementById("q-zone").value;
  const qType = document.getElementById("q-service-type")?.value || "";

  const filtered = servicesCache.filter((s) => {
    const okName = !qName || (s.service_name || "").toLowerCase().includes(qName);
    const okZone = !qZone || String(s.zone_id) === qZone;
    const okType = !qType || s.service_type === qType;
    return okName && okZone && okType;
  });
  renderServices(filtered);
}

async function loadAll() {
  const [services, zones, mem] = await Promise.all([
    apiRequest("/services"),
    apiRequest("/zones"),
    apiRequest("/memberships/brief"),
  ]);
  servicesCache = (Array.isArray(services) ? services : []).map((s) => ({
    ...s,
    is_active: readServiceActive(s),
    membership_ids: readMembershipIds(s),
  }));
  zonesForFilter = Array.isArray(zones) ? zones : [];
  membershipsBrief = Array.isArray(mem) ? mem : [];
  const qZoneSel = document.getElementById("q-zone");
  if (qZoneSel) {
    qZoneSel.innerHTML = [`<option value="">Все</option>`, ...zonesForFilter.map((z) => `<option value="${esc(z.zone_id)}">${esc(z.zone_name)}</option>`)].join("");
  }
  renderZoneSelect();
  renderMembershipCheckboxes();
  renderServices(servicesCache);
  applySearch();
}

document.addEventListener("DOMContentLoaded", () => {
  (async () => {
    try {
      await loadAll();
    } catch (e) {
      document.getElementById("services-tbody").innerHTML =
        `<tr><td colspan="7" class="error-message">${esc(formatApiErrors(e.fields) || e.message || "Ошибка загрузки")}</td></tr>`;
    }
  })();

  document.getElementById("btn-search")?.addEventListener("click", applySearch);
  document.getElementById("btn-clear-search")?.addEventListener("click", () => {
    document.getElementById("q-service-name").value = "";
    document.getElementById("q-zone").value = "";
    if (document.getElementById("q-service-type")) document.getElementById("q-service-type").value = "";
    renderServices(servicesCache);
    applySearch();
  });

  document.getElementById("services-tbody")?.addEventListener("click", async (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;
    const action = btn.dataset.action;
    const id = Number(btn.dataset.id);
    const item = servicesCache.find((x) => x.service_id === id);
    if (!item) return;

    if (action === "edit") {
      setServiceFormData(item);
      setFormError("");
      document.getElementById("serviceForm")?.scrollIntoView({ behavior: "smooth", block: "start" });
      return;
    }

    if (action === "delete") {
      if (!confirm(`Удалить услугу «${item.service_name}»?`)) return;
      try {
        await apiRequest(`/services/${id}`, { method: "DELETE" });
        await loadAll();
        clearServiceForm();
      } catch (err) {
        alert(formatApiErrors(err.fields) || err.message);
      }
    }
  });

  document.getElementById("btn-reset-service")?.addEventListener("click", () => {
    clearServiceForm();
    setFormError("");
  });

  document.getElementById("serviceForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    setFormError("");
    if (!e.target.checkValidity()) {
      e.target.reportValidity();
      return;
    }
    const sid = document.getElementById("service_id").value;
    const payload = getServiceFormPayload();
    if (!payload.service_name) {
      setFormError("Укажите название услуги.");
      return;
    }
    try {
      let saved;
      if (!sid) {
        saved = await apiRequest("/services", { method: "POST", body: JSON.stringify(payload) });
      } else {
        saved = await apiRequest(`/services/${Number(sid)}`, { method: "PUT", body: JSON.stringify(payload) });
      }
      setServiceFormData(saved);
      await loadAll();
    } catch (err) {
      setFormError(formatApiErrors(err.fields) || err.message);
    }
  });
});
