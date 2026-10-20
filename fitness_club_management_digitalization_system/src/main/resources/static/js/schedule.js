const API_BASE = (document.body.dataset.apiBase || "/api/public").replace(/\/$/, "");
const HOUR_START = 7;
const HOUR_END = 21;
const DAYS_COUNT = 7;
let scheduleItems = [];
let currentWeekStart = null;

function uniq(values) {
  return [...new Set(values)].sort((a, b) => a.localeCompare(b, "ru"));
}

function escapeHtml(s) {
  return String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#039;");
}

function dateOnly(value) {
  return String(value || "").slice(0, 10);
}

function timeOnly(value) {
  return String(value || "").slice(11, 16);
}

function startHour(value) {
  const t = timeOnly(value);
  if (!t || t.length < 2) return null;
  const h = Number(t.slice(0, 2));
  return Number.isFinite(h) ? h : null;
}

function formatDate(dateObj) {
  return dateObj.toLocaleDateString("ru-RU", { day: "2-digit", month: "2-digit" });
}

function weekdayRu(dateObj) {
  return dateObj.toLocaleDateString("ru-RU", { weekday: "long" });
}

function dateToIso(dateObj) {
  const y = dateObj.getFullYear();
  const m = String(dateObj.getMonth() + 1).padStart(2, "0");
  const d = String(dateObj.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function addDays(dateObj, days) {
  const d = new Date(dateObj);
  d.setDate(d.getDate() + days);
  return d;
}

function getWeekStart(dateObj) {
  const d = new Date(dateObj.getFullYear(), dateObj.getMonth(), dateObj.getDate());
  const jsDay = d.getDay();
  const mondayOffset = jsDay === 0 ? -6 : 1 - jsDay;
  d.setDate(d.getDate() + mondayOffset);
  return d;
}

function weekDays(start) {
  const out = [];
  for (let i = 0; i < DAYS_COUNT; i += 1) {
    out.push(addDays(start, i));
  }
  return out;
}

function fillSelect(select, values) {
  const current = select.value;
  const options = [`<option value="">Все</option>`, ...values.map((v) => `<option value="${escapeHtml(v)}">${escapeHtml(v)}</option>`)];
  select.innerHTML = options.join("");
  if ([...select.options].some((o) => o.value === current)) select.value = current;
}

function filteredItems() {
  const fDate = document.getElementById("f-date").value;
  const fService = document.getElementById("f-service").value;
  const fTrainer = document.getElementById("f-trainer").value;
  const fHall = document.getElementById("f-hall").value;
  const fTime = document.getElementById("f-time").value;
  return scheduleItems.filter((it) => {
    const hour = startHour(it.start_datetime);
    return (
      (!fDate || dateOnly(it.start_datetime) === fDate) &&
      (!fService || it.service_name === fService) &&
      (!fTrainer || it.trainer_name === fTrainer) &&
      (!fHall || it.hall_name === fHall) &&
      (!fTime || timeOnly(it.start_datetime) >= fTime) &&
      hour !== null &&
      hour >= HOUR_START &&
      hour <= HOUR_END
    );
  });
}

function renderGrid(items) {
  const head = document.getElementById("schedule-head-row");
  const body = document.getElementById("schedule-grid-body");
  head.innerHTML = "";
  body.innerHTML = "";

  const h0 = document.createElement("th");
  h0.textContent = "Время";
  head.appendChild(h0);

  const days = weekDays(currentWeekStart);
  for (const day of days) {
    const th = document.createElement("th");
    th.className = "time-header";
    th.innerHTML = `${escapeHtml(weekdayRu(day))}<br><span class="header-date">${escapeHtml(formatDate(day))}</span>`;
    head.appendChild(th);
  }

  for (let hour = HOUR_START; hour <= HOUR_END; hour += 1) {
    const tr = document.createElement("tr");
    const timeCell = document.createElement("td");
    timeCell.className = "time-cell";
    timeCell.textContent = `${String(hour).padStart(2, "0")}:00`;
    tr.appendChild(timeCell);

    for (const day of days) {
      const td = document.createElement("td");
      td.className = "grid-cell";
      const dayIso = dateToIso(day);
      const events = items
        .filter((it) => dateOnly(it.start_datetime) === dayIso && startHour(it.start_datetime) === hour)
        .sort((a, b) => String(a.start_datetime).localeCompare(String(b.start_datetime)));

      if (events.length) {
        td.innerHTML = events
          .map(
            (it) => `
              <div class="grid-item">
                <div class="grid-item-title">${escapeHtml(it.service_name)}</div>
                <div class="grid-item-gap"></div>
                <div class="grid-item-trainer">${escapeHtml(it.trainer_name)}</div>
                <div class="grid-item-gap"></div>
                <div class="grid-item-hall">${escapeHtml(it.hall_name || "—")}</div>
                <div class="grid-item-gap"></div>
                <div class="grid-item-time">${escapeHtml(`${timeOnly(it.start_datetime)} - ${timeOnly(it.end_datetime)}`)}</div>
              </div>
            `
          )
          .join("");
      }
      tr.appendChild(td);
    }
    body.appendChild(tr);
  }
}

function applyFilters(showEmptyAlert = false) {
  const selectedDate = document.getElementById("f-date").value;
  if (selectedDate) {
    const selected = new Date(`${selectedDate}T00:00:00`);
    if (!Number.isNaN(selected.getTime())) {
      currentWeekStart = getWeekStart(selected);
      updateWeekLabel();
    }
  }
  const items = filteredItems();
  renderGrid(items);
  const weekSet = new Set(weekDays(currentWeekStart).map(dateToIso));
  const hasWeekEvents = items.some((it) => weekSet.has(dateOnly(it.start_datetime)));
  if (!hasWeekEvents && showEmptyAlert) {
    alert("По выбранным фильтрам занятий не найдено.");
  }
}

function updateWeekLabel() {
  const end = addDays(currentWeekStart, 6);
  document.getElementById("week-range").textContent = `${formatDate(currentWeekStart)} - ${formatDate(end)}`;
}

function shiftWeek(deltaDays) {
  currentWeekStart = addDays(currentWeekStart, deltaDays);
  updateWeekLabel();
  applyFilters(false);
}

function resetFilters() {
  document.getElementById("f-date").value = "";
  document.getElementById("f-service").value = "";
  document.getElementById("f-trainer").value = "";
  document.getElementById("f-hall").value = "";
  document.getElementById("f-time").value = "";
  document.getElementById("f-date").value = "";
  applyFilters(false);
}

function init() {
  currentWeekStart = getWeekStart(new Date());
  updateWeekLabel();
  fetch(`${API_BASE}/schedule/group`)
    .then((r) => (r.ok ? r.json().catch(() => []) : []))
    .then((itemsRaw) => {
      scheduleItems = Array.isArray(itemsRaw) ? itemsRaw : [];
      fillSelect(document.getElementById("f-service"), uniq(scheduleItems.map((i) => i.service_name).filter(Boolean)));
      fillSelect(document.getElementById("f-trainer"), uniq(scheduleItems.map((i) => i.trainer_name).filter(Boolean)));
      fillSelect(document.getElementById("f-hall"), uniq(scheduleItems.map((i) => i.hall_name).filter(Boolean)));
      applyFilters(false);
    });

  document.getElementById("btn-find").addEventListener("click", () => applyFilters(true));
  document.getElementById("btn-reset").addEventListener("click", resetFilters);
  document.getElementById("btn-week-prev").addEventListener("click", () => shiftWeek(-7));
  document.getElementById("btn-week-next").addEventListener("click", () => shiftWeek(7));
}

document.addEventListener("DOMContentLoaded", init);

