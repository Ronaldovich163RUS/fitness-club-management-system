const API_BASE = (document.body.dataset.apiBase || "/api/admin").replace(/\/$/, "");

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

function fillAdminAlertError(box, titleText, msg) {
  if (!box) return;
  if (!msg) {
    box.style.display = "none";
    box.innerHTML = "";
    return;
  }
  box.style.display = "block";
  box.innerHTML = "";
  const t = document.createElement("span");
  t.className = "admin-alert-title";
  t.textContent = titleText;
  const b = document.createElement("div");
  b.className = "admin-alert-body";
  b.textContent = msg;
  box.appendChild(t);
  box.appendChild(b);
}

function setNotificationFormError(msg) {
  fillAdminAlertError(document.getElementById("notificationFormError"), "Не удалось отправить", msg);
}

function clientLabel(c) {
  const mn = c.middle_name ?? c.middleName ?? "";
  const m = mn ? ` ${mn}` : "";
  return `${c.last_name ?? c.lastName ?? ""} ${c.first_name ?? c.firstName ?? ""}${m}`.trim();
}

function trainerLabel(t) {
  const mn = t.middle_name ?? t.middleName ?? "";
  const m = mn ? ` ${mn}` : "";
  return `${t.last_name ?? t.lastName ?? ""} ${t.first_name ?? t.firstName ?? ""}${m}`.trim();
}

function formatDateTime(iso) {
  if (typeof fcFormatDateTime === "function") return fcFormatDateTime(iso);
  if (!iso) return "—";
  const s = typeof iso === "string" ? iso : "";
  if (!s) return "—";
  const d = new Date(s);
  if (Number.isNaN(d.getTime())) return s.replace("T", " ").slice(0, 19);
  return d.toLocaleString("ru-RU", { dateStyle: "short", timeStyle: "short", timeZone: "Europe/Samara" });
}

let clientsOptions = [];
let trainersOptions = [];

async function fillRecipientSelect() {
  const sel = document.getElementById("recipient_id");
  const recipientType = document.getElementById("recipient_type")?.value || "client";
  if (recipientType === "trainer") {
    if (!trainersOptions.length) {
      const list = await apiRequest("/trainers");
      trainersOptions = Array.isArray(list) ? list : [];
    }
    sel.innerHTML =
      '<option value="">Все тренеры</option>' +
      trainersOptions.map((t) => {
        const id = t.trainer_id ?? t.trainerId;
        return `<option value="${esc(id)}">${esc(trainerLabel(t))}</option>`;
      }).join("");
  } else {
    if (!clientsOptions.length) {
      const list = await apiRequest("/clients");
      clientsOptions = Array.isArray(list) ? list : [];
    }
    sel.innerHTML =
      '<option value="">Все клиенты</option>' +
      clientsOptions.map((c) => {
        const id = c.client_id ?? c.clientId;
        return `<option value="${esc(id)}">${esc(clientLabel(c))}</option>`;
      }).join("");
  }
}

async function renderNotificationsTable() {
  const tbody = document.getElementById("notifications-tbody");
  if (!tbody) return;
  tbody.innerHTML = "";
  let list;
  try {
    list = await apiRequest("/notifications");
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="5" class="muted">${esc(formatApiErrors(e.fields) || e.message)}</td></tr>`;
    return;
  }
  const arr = Array.isArray(list) ? list : [];
  if (!arr.length) {
    tbody.innerHTML = '<tr><td colspan="5" class="muted">Нет уведомлений</td></tr>';
    return;
  }
  for (const n of arr) {
    const rt = n.recipient_type ?? n.recipientType ?? "";
    const role = rt === "trainer" ? "Тренер" : "Клиент";
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc(role)}</td>
      <td>${esc(n.recipient_label ?? n.recipientLabel ?? "—")}</td>
      <td>${esc((n.title || "").slice(0, 80))}${(n.title || "").length > 80 ? "…" : ""}</td>
      <td>${esc((n.message || "").slice(0, 200))}${(n.message || "").length > 200 ? "…" : ""}</td>
      <td>${esc(formatDateTime(n.sent_datetime ?? n.sentDatetime))}</td>
    `;
    tbody.appendChild(tr);
  }
}

document.addEventListener("DOMContentLoaded", () => {
  fillRecipientSelect()
    .then(() => renderNotificationsTable())
    .catch((e) => setNotificationFormError(formatApiErrors(e.fields) || e.message));

  document.getElementById("recipient_type")?.addEventListener("change", () => {
    fillRecipientSelect().catch((e) => setNotificationFormError(formatApiErrors(e.fields) || e.message));
  });

  document.getElementById("notificationForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    setNotificationFormError("");
    const form = e.target;
    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }
    const recipient_type = document.getElementById("recipient_type").value;
    const rid = document.getElementById("recipient_id").value;
    const title = document.getElementById("notification_title").value.trim();
    const message = document.getElementById("notification_message").value.trim();
    const body = {
      recipient_type,
      recipient_id: rid === "" ? null : Number(rid),
      title,
      message,
    };
    try {
      await apiRequest("/notifications", { method: "POST", body: JSON.stringify(body) });
      document.getElementById("notification_title").value = "";
      document.getElementById("notification_message").value = "";
      await renderNotificationsTable();
    } catch (err) {
      setNotificationFormError(formatApiErrors(err.fields) || err.message);
    }
  });
});
