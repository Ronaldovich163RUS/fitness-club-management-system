const trainerSlotsState = { zones: [], halls: [], slots: [] };

function setSlotMessage(msg, isError) {
  if (msg) alert(msg);
}

function readError(err, fallback) {
  if (!err) return fallback;
  if (typeof err === "string") return err;
  if (err.fields && typeof err.fields === "object") {
    const first = Object.values(err.fields)[0];
    if (first) return String(first);
  }
  if (err.fieldErrors && typeof err.fieldErrors === "object") {
    const first = Object.values(err.fieldErrors)[0];
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

function fillZones() {
  const zSel = document.getElementById("slot-zone");
  zSel.innerHTML = "<option value=\"\">— Выберите зону —</option>" + trainerSlotsState.zones
    .map((z) => `<option value="${z.zone_id}">${esc(z.zone_name)}</option>`)
    .join("");
}

function fillHalls() {
  const sel = document.getElementById("slot-hall");
  const selectedZoneId = Number(document.getElementById("slot-zone").value || 0);
  if (!selectedZoneId) {
    sel.innerHTML = "<option value=\"\">— Сначала выберите зону —</option>";
    return;
  }
  const halls = trainerSlotsState.halls.filter((h) => h.zone_id === selectedZoneId);
  sel.innerHTML = "<option value=\"\">— Выберите зал —</option>" + halls
    .map((h) => `<option value="${h.hall_id}">${esc(h.hall_name)} (${esc(h.zone_name)})</option>`)
    .join("");
}

function renderMySlots() {
  const tbody = document.getElementById("trainer-slots-tbody");
  const list = [...trainerSlotsState.slots].sort((a, b) => String(a.start_datetime).localeCompare(String(b.start_datetime)));
  tbody.innerHTML = "";
  if (!list.length) {
    tbody.innerHTML = "<tr><td colspan=\"4\" class=\"muted\">Предстоящих слотов нет.</td></tr>";
    return;
  }
  list.forEach((s) => {
    const tr = document.createElement("tr");
    const clientName = (s.client_full_name && String(s.client_full_name).trim()) || "";
    tr.innerHTML = `
      <td>${esc(formatDateTime(s.start_datetime))} - ${esc(String(s.end_datetime).slice(11, 16))}</td>
      <td>${esc(s.hall_name || s.hall_id)}</td>
      <td>${esc(s.zone_name || "—")}</td>
      <td>${clientName ? esc(clientName) : "—"}</td>
    `;
    tbody.appendChild(tr);
  });
}

async function loadPageData() {
  const [meta, slotsRaw] = await Promise.all([
    api("/api/trainer/slots/meta"),
    api("/api/trainer/slots?personalOnly=true")
  ]);
  trainerSlotsState.zones = meta.zones || [];
  trainerSlotsState.halls = meta.halls || [];
  const now = Date.now();
  trainerSlotsState.slots = (slotsRaw || []).filter((s) => {
    if (s.is_cancelled) return false;
    const start = new Date(s.start_datetime).getTime();
    return Number.isFinite(start) && start > now;
  });
  fillZones();
  fillHalls();
  renderMySlots();
}

document.addEventListener("DOMContentLoaded", async () => {
  try {
    await loadPageData();
  } catch (err) {
    setSlotMessage(readError(err, "Не удалось загрузить данные слотов."), true);
  }
  document.getElementById("slot-zone")?.addEventListener("change", fillHalls);

  document.getElementById("slot-form")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    setSlotMessage("", false);
    const form = e.target;
    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }

    const date = document.getElementById("slot-date").value;
    const time = document.getElementById("slot-time").value;
    const hallId = Number(document.getElementById("slot-hall").value);
    if (!hallId) {
      setSlotMessage("Выберите зал.", true);
      return;
    }
    try {
      await api("/api/trainer/slots", {
        method: "POST",
        body: JSON.stringify({ date, time, hall_id: hallId })
      });
      await loadPageData();
      setSlotMessage("Слот успешно создан и доступен клиентам для записи.", false);
    } catch (err) {
      setSlotMessage(readError(err, "Не удалось создать слот."), true);
    }
  });
});
