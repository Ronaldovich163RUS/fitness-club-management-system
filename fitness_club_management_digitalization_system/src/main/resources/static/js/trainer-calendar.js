const trainerCalendarState = { slots: [] };

function setCalendarMessage(msg, isError) {
  if (msg) alert(msg);
}

function readError(err, fallback) {
  if (!err) return fallback;
  if (typeof err === "string") return err;
  if (err.fields && typeof err.fields === "object") {
    const first = Object.values(err.fields)[0];
    if (first) return String(first);
  }
  if (err.errors && typeof err.errors === "object") {
    const first = Object.values(err.errors)[0];
    if (first) return String(first);
  }
  return err.message || fallback;
}

async function api(url, options = {}) {
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json" },
    ...options
  });
  if (!res.ok) {
    let payload = null;
    try { payload = await res.json(); } catch (_) {}
    throw payload || { message: `HTTP ${res.status}` };
  }
  const type = res.headers.get("content-type") || "";
  if (type.includes("application/json")) return res.json();
  return null;
}

function renderCalendar() {
  const tbody = document.getElementById("trainer-calendar-tbody");
  const list = [...trainerCalendarState.slots].sort((a, b) => String(a.start_datetime).localeCompare(String(b.start_datetime)));
  tbody.innerHTML = "";
  if (!list.length) {
    tbody.innerHTML = "<tr><td colspan=\"5\" class=\"muted\">Предстоящих занятий нет.</td></tr>";
    return;
  }
  const now = Date.now();
  list.forEach((s) => {
    const isPersonal = s.service_type === "персональная тренировка";
    const canCancel = isPersonal && new Date(s.start_datetime).getTime() > now;
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc(formatDateTime(s.start_datetime))}</td>
      <td>${esc(s.service_name || s.service_id)}</td>
      <td>${esc(s.service_type || "—")}</td>
      <td>${esc(s.hall_name || s.hall_id)}</td>
      <td>${canCancel ? `<button type="button" class="admin-btn danger btn-cancel-slot" data-id="${s.schedule_id}">Отменить тренировку</button>` : "—"}</td>
    `;
    tbody.appendChild(tr);
  });
}

async function loadSlots() {
  const all = await api("/api/trainer/slots");
  const now = Date.now();
  trainerCalendarState.slots = (all || []).filter((s) => {
    if (s.is_cancelled) return false;
    const start = new Date(s.start_datetime).getTime();
    return Number.isFinite(start) && start > now;
  });
  renderCalendar();
}

async function cancelPersonalSlot(scheduleId) {
  await api(`/api/trainer/slots/${scheduleId}/cancel`, { method: "POST" });
  await loadSlots();
  setCalendarMessage("Персональная тренировка отменена.", false);
}

document.addEventListener("DOMContentLoaded", async () => {
  try {
    await loadSlots();
  } catch (err) {
    setCalendarMessage(readError(err, "Не удалось загрузить календарь."), true);
  }
  document.getElementById("trainer-calendar-tbody")?.addEventListener("click", async (e) => {
    const btn = e.target.closest(".btn-cancel-slot");
    if (!btn) return;
    try {
      await cancelPersonalSlot(btn.dataset.id);
    } catch (err) {
      setCalendarMessage(readError(err, "Не удалось отменить тренировку."), true);
    }
  });
});
