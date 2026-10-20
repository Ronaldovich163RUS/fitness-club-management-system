/** Тренеры и расписание — те же структуры, что в админке (без бэкенда — мок). */
const trainers = [
  { trainer_id: 201, last_name: "Морозова", first_name: "Анна", middle_name: "Павловна" },
  { trainer_id: 202, last_name: "Ильина", first_name: "Мария", middle_name: "Олеговна" },
  { trainer_id: 203, last_name: "Беляев", first_name: "Иван", middle_name: "Алексеевич" },
];

const services = [
  { service_id: 1, service_name: "Функциональный тренинг" },
  { service_id: 2, service_name: "Йога (Hatha)" },
  { service_id: 3, service_name: "Сайкл" },
  { service_id: 4, service_name: "Пилатес" },
];

const halls = [
  { hall_id: 1, hall_name: "Main Hall" },
  { hall_id: 2, hall_name: "Studio Hall" },
];

/** Расписание (можно подгружать из localStorage или оставить мок). Для синхронизации с админкой можно использовать общий ключ. */
function loadSchedule() {
  try {
    const raw = localStorage.getItem("fc_schedule");
    return raw ? JSON.parse(raw) : getDefaultSchedule();
  } catch (_) {
    return getDefaultSchedule();
  }
}

function getDefaultSchedule() {
  return [
    { schedule_id: 1, service_id: 1, trainer_id: 201, hall_id: 1, start_datetime: "2025-03-20T10:00", end_datetime: "2025-03-20T10:55", max_participants: 20, current_participants: 12, is_cancelled: false },
    { schedule_id: 2, service_id: 2, trainer_id: 202, hall_id: 2, start_datetime: "2025-03-20T11:00", end_datetime: "2025-03-20T12:00", max_participants: 15, current_participants: 8, is_cancelled: false },
    { schedule_id: 3, service_id: 3, trainer_id: 203, hall_id: 1, start_datetime: "2025-03-21T09:00", end_datetime: "2025-03-21T09:45", max_participants: 25, current_participants: 0, is_cancelled: true },
  ];
}

/** Уведомления тренерам (recipient_type='trainer') — мок или из localStorage */
function loadTrainerNotifications() {
  try {
    const raw = localStorage.getItem("fc_trainer_notifications");
    return raw ? JSON.parse(raw) : [];
  } catch (_) {
    return [];
  }
}

function esc(s) {
  return String(s ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&#039;");
}

function trainerName(t) {
  return [t.last_name, t.first_name, t.middle_name].filter(Boolean).join(" ");
}

function serviceName(id) {
  return services.find((s) => s.service_id === id)?.service_name ?? id;
}

function hallName(id) {
  return halls.find((h) => h.hall_id === id)?.hall_name ?? id;
}

function formatDate(iso) {
  return iso ? iso.slice(0, 10) : "—";
}

function formatTime(iso) {
  return iso ? iso.slice(11, 16) : "—";
}

function formatDateTime(iso) {
  if (typeof fcFormatDateTime === "function") return fcFormatDateTime(iso);
  if (!iso) return "—";
  const d = new Date(iso);
  return Number.isNaN(d.getTime())
    ? "—"
    : d.toLocaleString("ru-RU", { dateStyle: "short", timeStyle: "short", timeZone: "Europe/Samara" });
}

function fillTrainerSelect() {
  const sel = document.getElementById("trainer-select");
  sel.innerHTML = "<option value=\"\">— Выберите тренера —</option>" + trainers.map((t) => `<option value="${t.trainer_id}">${esc(trainerName(t))}</option>`).join("");
}

function renderTrainerSchedule(trainerId) {
  const tbody = document.getElementById("trainer-schedule-tbody");
  tbody.innerHTML = "";
  const scheduleList = loadSchedule();
  const list = scheduleList.filter((s) => String(s.trainer_id) === String(trainerId));
  if (!list.length) {
    tbody.innerHTML = "<tr><td colspan=\"6\" class=\"muted\">Нет назначенных занятий</td></tr>";
    return;
  }
  list.forEach((s) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc(formatDate(s.start_datetime))}</td>
      <td>${esc(serviceName(s.service_id))}</td>
      <td>${esc(hallName(s.hall_id))}</td>
      <td>${esc(formatTime(s.start_datetime))} – ${esc(formatTime(s.end_datetime))}</td>
      <td>${esc(s.current_participants)} / ${esc(s.max_participants)}</td>
      <td>${s.is_cancelled ? "Отменено" : "Активно"}</td>
    `;
    tbody.appendChild(tr);
  });
}

function renderTrainerNotifications(trainerId) {
  const box = document.getElementById("trainer-notifications-list");
  const list = loadTrainerNotifications().filter((n) => n.recipient_type === "trainer" && n.recipient_trainer_id === trainerId);
  if (!list.length) {
    box.innerHTML = "<p class=\"muted\">Нет уведомлений.</p>";
    return;
  }
  box.innerHTML = "<table class=\"admin-table\"><thead><tr><th>Дата</th><th>Заголовок</th><th>Текст</th></tr></thead><tbody id=\"trainer-notif-tbody\"></tbody></table>";
  const tbody = document.getElementById("trainer-notif-tbody");
  list.forEach((n) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td>${esc(formatDateTime(n.sent_datetime || n.created_at))}</td><td>${esc(n.title)}</td><td>${esc((n.message || "").slice(0, 100))}${(n.message || "").length > 100 ? "…" : ""}</td>`;
    tbody.appendChild(tr);
  });
}

function onTrainerChange() {
  const trainerId = document.getElementById("trainer-select").value;
  const content = document.getElementById("cabinet-content");
  if (!trainerId) {
    content.style.display = "none";
    return;
  }
  content.style.display = "block";
  renderTrainerSchedule(trainerId);
  renderTrainerNotifications(Number(trainerId));
}

document.addEventListener("DOMContentLoaded", () => {
  fillTrainerSelect();
  document.getElementById("trainer-select")?.addEventListener("change", onTrainerChange);
});
