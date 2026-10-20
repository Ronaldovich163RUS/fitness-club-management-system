const API_BASE = (document.body.dataset.apiBase || "/api/client").replace(/\/$/, "");
function esc(v) { return String(v ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;").replaceAll("'", "&#039;"); }
function formatDateTime(iso) {
  if (typeof fcFormatDateTime === "function") return fcFormatDateTime(iso);
  if (!iso) return "—";
  const d = new Date(iso);
  return Number.isNaN(d.getTime())
    ? String(iso)
    : d.toLocaleString("ru-RU", { dateStyle: "short", timeStyle: "short", timeZone: "Europe/Samara" });
}

function render(list) {
  const tbody = document.getElementById("notifications-tbody");
  tbody.innerHTML = "";
  if (!list.length) {
    tbody.innerHTML = "<tr><td colspan=\"5\" class=\"muted\">Нет уведомлений.</td></tr>";
    return;
  }
  list.forEach((n) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc(formatDateTime(n.sent_datetime))}</td>
      <td>${esc(n.sender || "Администрация клуба")}</td>
      <td>${esc(n.sender_role || "Администратор")}</td>
      <td>${esc(n.title || "")}</td>
      <td>${esc((n.message || "").slice(0, 200))}${(n.message || "").length > 200 ? "…" : ""}</td>
    `;
    tbody.appendChild(tr);
  });
}

document.addEventListener("DOMContentLoaded", () => {
  fetch(`${API_BASE}/notifications`).then((r) => r.ok ? r.json() : []).then((list) => render(Array.isArray(list) ? list : []));
});
