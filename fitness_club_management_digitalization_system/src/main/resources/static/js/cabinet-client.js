/** Клиенты — те же id, что в админке (для выбора «вход в кабинет»). */
const clients = [
  { client_id: 101, last_name: "Иванов", first_name: "Иван", middle_name: "Иванович", email: "ivanov.ivan@example.com" },
  { client_id: 102, last_name: "Петрова", first_name: "Анна", middle_name: "Сергеевна", email: "petrova.anna@example.com" },
  { client_id: 103, last_name: "Смирнов", first_name: "Дмитрий", middle_name: "Андреевич", email: "smirnov.dmitry@example.com" },
];

const STORAGE_KEY = "fc_notifications";

function loadNotifications() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch (_) {
    return [];
  }
}

function esc(s) {
  return String(s ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&#039;");
}

function clientLabel(c) {
  return [c.last_name, c.first_name, c.middle_name].filter(Boolean).join(" ");
}

function formatDateTime(iso) {
  if (typeof fcFormatDateTime === "function") return fcFormatDateTime(iso);
  if (!iso) return "—";
  const d = new Date(iso);
  return Number.isNaN(d.getTime())
    ? "—"
    : d.toLocaleString("ru-RU", { dateStyle: "short", timeStyle: "short", timeZone: "Europe/Samara" });
}

function fillClientSelect() {
  const sel = document.getElementById("client-select");
  sel.innerHTML = "<option value=\"\">— Выберите клиента —</option>" + clients.map((c) => `<option value="${c.client_id}">${esc(clientLabel(c))} (${esc(c.email)})</option>`).join("");
}

function renderClientNotifications(clientId) {
  const box = document.getElementById("client-notifications-list");
  const all = loadNotifications();
  const list = all.filter((n) => n.recipient_type === "client" && n.recipient_client_id === clientId && n.notification_type === "system");
  if (!list.length) {
    box.innerHTML = "<p class=\"muted\">Нет внутренних уведомлений.</p>";
    return;
  }
  const sorted = list.slice().sort((a, b) => (b.sent_datetime || "").localeCompare(a.sent_datetime || ""));
  box.innerHTML = "<table class=\"admin-table\"><thead><tr><th>Дата</th><th>Заголовок</th><th>Сообщение</th></tr></thead><tbody id=\"client-notif-tbody\"></tbody></table>";
  const tbody = document.getElementById("client-notif-tbody");
  sorted.forEach((n) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td>${esc(formatDateTime(n.sent_datetime))}</td><td>${esc(n.title)}</td><td>${esc(n.message || "")}</td>`;
    tbody.appendChild(tr);
  });
}

function onClientChange() {
  const clientId = document.getElementById("client-select").value;
  const content = document.getElementById("cabinet-content");
  if (!clientId) {
    content.style.display = "none";
    return;
  }
  content.style.display = "block";
  renderClientNotifications(Number(clientId));
}

document.addEventListener("DOMContentLoaded", () => {
  fillClientSelect();
  document.getElementById("client-select")?.addEventListener("change", onClientChange);
});
