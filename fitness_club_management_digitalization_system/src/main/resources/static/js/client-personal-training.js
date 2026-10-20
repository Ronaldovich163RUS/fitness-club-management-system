const API_BASE = (document.body.dataset.apiBase || "/api/client").replace(/\/$/, "");
const personalState = { payload: null };

function setMessage(text, isError) {
  if (text) alert(text);
}
function esc(v) { return String(v ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;").replaceAll("'", "&#039;"); }
function readApiError(payload, fallback) {
  if (!payload) return fallback;
  if (typeof payload === "string") return payload;
  if (payload.fields && typeof payload.fields === "object") {
    const first = Object.values(payload.fields)[0];
    if (first) return String(first);
  }
  if (payload.message) return String(payload.message);
  return fallback;
}

async function parseJsonSafe(response) {
  try {
    return await response.json();
  } catch (_) {
    return null;
  }
}
function formatDateTime(iso) {
  if (typeof fcFormatDateTime === "function") return fcFormatDateTime(iso);
  if (!iso) return "—";
  const d = new Date(iso);
  return Number.isNaN(d.getTime())
    ? String(iso)
    : d.toLocaleString("ru-RU", { dateStyle: "short", timeStyle: "short", timeZone: "Europe/Samara" });
}

function fillFilterSelects() {
  const trainerSel = document.getElementById("filter-trainer-id");
  const zoneSel = document.getElementById("filter-zone-id");
  const opt = (v, text) => `<option value="${v}">${esc(text)}</option>`;
  const trainers = personalState.payload?.trainers || [];
  trainerSel.innerHTML = "<option value=\"\">— Любой —</option>" + trainers
    .map((t) => opt(t.trainer_id, t.full_name)).join("");
  zoneSel.innerHTML = "<option value=\"\">— Любая —</option>" + (personalState.payload?.zones || [])
    .map((z) => opt(z.zone_id, z.zone_name)).join("");
}

function getAvailableSlots() {
  const dateFrom = document.getElementById("filter-date-from")?.value || null;
  const dateTo = document.getElementById("filter-date-to")?.value || null;
  const timeFrom = document.getElementById("filter-time-from")?.value || null;
  const timeTo = document.getElementById("filter-time-to")?.value || null;
  const trainerId = document.getElementById("filter-trainer-id")?.value ? Number(document.getElementById("filter-trainer-id").value) : null;
  const zoneId = document.getElementById("filter-zone-id")?.value ? Number(document.getElementById("filter-zone-id").value) : null;

  return (personalState.payload?.slots || []).filter((s) => {
    const sStart = String(s.start_datetime || "");
    if (dateFrom && sStart.slice(0, 10) < dateFrom) return false;
    if (dateTo && sStart.slice(0, 10) > dateTo) return false;
    const sTime = sStart.slice(11, 16);
    if (timeFrom && sTime < timeFrom) return false;
    if (timeTo && sTime > timeTo) return false;
    if (trainerId && s.trainer_id !== trainerId) return false;
    if (zoneId && s.zone_id !== zoneId) return false;
    return true;
  }).sort((a, b) => String(a.start_datetime).localeCompare(b.start_datetime));
}

function doSearchSlots() {
  setMessage("", false);
  const slots = getAvailableSlots();
  renderAvailableSlots(slots);
}

function renderAvailableSlots(slots) {
  const tbody = document.getElementById("available-slots-tbody");
  tbody.innerHTML = "";
  if (!slots.length) {
    tbody.innerHTML = "<tr><td colspan=\"7\" class=\"muted\">Нет доступных слотов по заданным фильтрам.</td></tr>";
    return;
  }
  slots.forEach((s) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc(formatDateTime(s.start_datetime))}</td>
      <td>${esc(s.trainer_name || "—")}</td>
      <td>—</td>
      <td>${esc(s.hall_name || "—")}</td>
      <td>${esc(s.zone_name || "—")}</td>
      <td>${esc(s.current_participants)} / ${esc(s.max_participants)}</td>
      <td><button type="button" class="admin-btn primary btn-book" data-schedule-id="${s.schedule_id}">Записаться</button></td>
    `;
    tbody.appendChild(tr);
  });
}

function doRegister(scheduleId) {
  setMessage("", false);
  fetch(`${API_BASE}/personal-trainings/register`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ schedule_id: Number(scheduleId) }) })
    .then(async (r) => {
      if (!r.ok) {
        const payload = await parseJsonSafe(r);
        throw new Error(readApiError(payload, "Не удалось записаться"));
      }
      setMessage("Запись успешно создана.", false);
      loadData();
    })
    .catch((e) => setMessage(e.message, true));
}

function doCancel(purchaseId) {
  setMessage("", false);
  fetch(`${API_BASE}/personal-trainings/cancel`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ purchase_id: Number(purchaseId) }) })
    .then(async (r) => {
      if (!r.ok) {
        const payload = await parseJsonSafe(r);
        throw new Error(readApiError(payload, "Не удалось отменить запись"));
      }
      setMessage("Запись отменена.", false);
      loadData();
    })
    .catch((e) => setMessage(e.message, true));
}

function renderMyRegistrations() {
  const tbody = document.getElementById("my-registrations-tbody");
  const active = personalState.payload?.my_registrations || [];
  tbody.innerHTML = "";
  if (!active.length) {
    tbody.innerHTML = "<tr><td colspan=\"6\" class=\"muted\">Активных записей нет.</td></tr>";
    return;
  }
  active.forEach((r) => {
    const cancelBtn = `<button type="button" class="admin-btn danger btn-cancel" data-purchase-id="${r.purchase_id}">Отменить</button>`;
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc(formatDateTime(r.start_datetime || ""))}</td>
      <td>${esc(r.trainer_name || "—")}</td>
      <td>${esc(r.hall_name || "—")}</td>
      <td>${esc(r.zone_name || "—")}</td>
      <td>${esc(formatDateTime(r.purchase_date ?? r.created_at))}</td>
      <td>${cancelBtn}</td>
    `;
    tbody.appendChild(tr);
  });
}

function loadData() {
  fetch(`${API_BASE}/personal-trainings`).then(async (r) => {
    if (!r.ok) {
      const payload = await parseJsonSafe(r);
      throw new Error(readApiError(payload, "Не удалось загрузить данные персональных тренировок."));
    }
    return r.json();
  }).then((payload) => {
    personalState.payload = payload || { trainers: [], zones: [], slots: [], my_registrations: [] };
    fillFilterSelects();
    doSearchSlots();
    renderMyRegistrations();
  }).catch((e) => setMessage(e.message, true));
}

document.addEventListener("DOMContentLoaded", () => {
  loadData();

  document.getElementById("btn-search-slots")?.addEventListener("click", doSearchSlots);
  document.getElementById("btn-reset-filters")?.addEventListener("click", () => {
    document.getElementById("filter-date-from").value = "";
    document.getElementById("filter-date-to").value = "";
    document.getElementById("filter-time-from").value = "";
    document.getElementById("filter-time-to").value = "";
    document.getElementById("filter-trainer-id").value = "";
    document.getElementById("filter-zone-id").value = "";
    setMessage("", false);
  });

  document.getElementById("available-slots-tbody")?.addEventListener("click", (e) => {
    const btn = e.target.closest(".btn-book");
    if (btn) doRegister(btn.dataset.scheduleId);
  });

  document.getElementById("my-registrations-tbody")?.addEventListener("click", (e) => {
    const btn = e.target.closest(".btn-cancel");
    if (btn) doCancel(btn.dataset.purchaseId);
  });
});
