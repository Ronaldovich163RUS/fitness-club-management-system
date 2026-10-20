const trainerNotificationsState = { clients: [] };

function setNotificationMessage(msg, isError) {
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

function fillClients() {
  const sel = document.getElementById("trainer-client-id");
  sel.innerHTML = "<option value=\"\">— Выберите клиента —</option>" + trainerNotificationsState.clients
    .map((c) => `<option value="${c.client_id}">${esc(c.full_name)}</option>`).join("");
}

document.addEventListener("DOMContentLoaded", async () => {
  try {
    trainerNotificationsState.clients = await api("/api/trainer/clients");
    fillClients();
  } catch (err) {
    setNotificationMessage(readError(err, "Не удалось загрузить список клиентов."), true);
  }
  document.getElementById("trainer-notification-form")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    setNotificationMessage("", false);
    const cid = Number(document.getElementById("trainer-client-id").value || 0);
    if (!cid) {
      setNotificationMessage("Выберите клиента.", true);
      return;
    }
    const title = document.getElementById("notification_title").value.trim();
    const msg = document.getElementById("notification_message").value.trim();
    if (!title || !msg) {
      setNotificationMessage("Заполните заголовок и сообщение.", true);
      return;
    }
    try {
      await api("/api/trainer/notifications", {
        method: "POST",
        body: JSON.stringify({
          client_id: cid,
          notification_type: "system",
          title,
          message: msg
        })
      });
      document.getElementById("notification_title").value = "";
      document.getElementById("notification_message").value = "";
      setNotificationMessage("Уведомление отправлено клиенту.", false);
    } catch (err) {
      setNotificationMessage(readError(err, "Не удалось отправить уведомление."), true);
    }
  });
});
