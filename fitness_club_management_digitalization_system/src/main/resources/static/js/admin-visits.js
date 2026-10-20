const API_ADMIN = (document.body.dataset.apiBase || "/api/admin").replace(/\/$/, "");

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
  const r = await fetch(`${API_ADMIN}${path}`, { ...options, headers });
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

/** Всплывающее окно браузера (как на странице расписания). */
function notify(msg, title) {
  if (!msg) return;
  const body = String(msg).trim();
  const head = title && String(title).trim();
  alert(head ? `${head}\n\n${body}` : body);
}

function readDataInt(el, name) {
  if (!el) return null;
  const raw = el.getAttribute(`data-${name}`);
  if (raw === null || raw === "") return null;
  const n = Number(raw);
  return Number.isFinite(n) ? n : null;
}

function readSelectInt(selectEl) {
  if (!selectEl || !selectEl.value) return null;
  const n = Number(selectEl.value);
  return Number.isFinite(n) && n > 0 ? n : null;
}

function clientLabel(c) {
  const mn = c.middle_name ?? c.middleName ?? "";
  const m = mn ? ` ${mn}` : "";
  return `${c.last_name ?? c.lastName ?? ""} ${c.first_name ?? c.firstName ?? ""}${m}`.trim();
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

let clientsCache = [];
const visitsModalPanels = ["modal-entry", "modal-services", "modal-exit"];

function openVisitsModal(panelId) {
  const root = document.getElementById("visits-modal-root");
  if (!root) return;
  root.hidden = false;
  root.setAttribute("aria-hidden", "false");
  for (const id of visitsModalPanels) {
    const panel = document.getElementById(id);
    if (panel) panel.hidden = id !== panelId;
  }
}

function closeVisitsModal() {
  const root = document.getElementById("visits-modal-root");
  if (!root) return;
  root.hidden = true;
  root.setAttribute("aria-hidden", "true");
  for (const id of visitsModalPanels) {
    const panel = document.getElementById(id);
    if (panel) panel.hidden = true;
  }
}

async function loadClients() {
  const list = await apiRequest("/clients");
  clientsCache = Array.isArray(list) ? list : [];
  const opts =
    '<option value="">— Выберите клиента —</option>' +
    clientsCache.map((c) => `<option value="${esc(c.client_id ?? c.clientId)}">${esc(clientLabel(c))}</option>`).join("");
  const entry = document.getElementById("entry-client");
  if (entry) entry.innerHTML = opts;
}

async function loadClubStats() {
  const s = await apiRequest("/visits/club-stats");
  const n = s?.current_visitors ?? s?.currentVisitors ?? 0;
  const el = document.getElementById("club-stats-count");
  if (el) el.textContent = String(n);
}

async function loadJournal() {
  const rows = await apiRequest("/visits/journal");
  const tbody = document.getElementById("visits-journal-tbody");
  if (!tbody) return;
  tbody.innerHTML = "";
  const list = Array.isArray(rows) ? rows : [];
  if (!list.length) {
    tbody.innerHTML = '<tr><td colspan="4" class="muted">Нет записей о посещениях.</td></tr>';
    return;
  }
  for (const v of list) {
    const tr = document.createElement("tr");
    const vt = v.visit_type ?? v.visitType ?? "";
    tr.innerHTML = `
      <td>${esc(v.client_full_name ?? v.clientFullName ?? "")}</td>
      <td>${esc(formatDateTime(v.entry_datetime ?? v.entryDatetime))}</td>
      <td>${esc((v.exit_datetime ?? v.exitDatetime) ? formatDateTime(v.exit_datetime ?? v.exitDatetime) : "—")}</td>
      <td>${esc(vt === "гостевой" ? "гостевой" : "обычный")}</td>
    `;
    tbody.appendChild(tr);
  }
}

async function refreshOpenClientSelects() {
  const openList = await apiRequest("/visits/open-clients");
  const open = Array.isArray(openList) ? openList : [];
  const opt = (id, text) => `<option value="${esc(id)}">${esc(text)}</option>`;
  const base = '<option value="">— Выберите клиента —</option>';
  const inner = base + open.map((o) => opt(o.client_id ?? o.clientId, o.client_full_name ?? o.clientFullName ?? "")).join("");
  const selExit = document.getElementById("exit-client");
  const selSvc = document.getElementById("services-client");
  if (selExit) selExit.innerHTML = inner;
  if (selSvc) selSvc.innerHTML = inner;
}

function renderAvailableServices(clientId) {
  const placeholder = document.getElementById("available-services-placeholder");
  const list = document.getElementById("available-services-list");
  const tbody = document.getElementById("available-services-tbody");
  if (!clientId) {
    if (placeholder) placeholder.style.display = "block";
    if (list) list.style.display = "none";
    return;
  }
  apiRequest(`/visits/available-services/${clientId}`)
    .then((items) => {
      const arr = Array.isArray(items) ? items : [];
      if (placeholder) placeholder.style.display = "none";
      if (list) list.style.display = "block";
      if (!tbody) return;
      tbody.innerHTML = "";
      if (!arr.length) {
        tbody.innerHTML =
          '<tr><td colspan="5" class="muted">Нет доступных услуг по правилам абонемента и разовых покупок.</td></tr>';
        return;
      }
      for (const item of arr) {
        const tr = document.createElement("tr");
        const src = item.source ?? "";
        const mid = item.membership_purchase_id ?? item.membershipPurchaseId;
        const sid = item.service_purchase_id ?? item.servicePurchaseId;
        const key =
          src === "абонемент" ? `m-${mid}-${item.service_id ?? item.serviceId}` : `s-${sid}`;
        const sch = item.schedule_id ?? item.scheduleId;
        const usable = item.usable !== false;
        tr.innerHTML = `
        <td>${esc(src)}</td>
        <td>${esc(item.service_name ?? item.serviceName ?? "")}</td>
        <td>${esc(item.service_type ?? item.serviceType ?? "")}</td>
        <td>${esc(item.details ?? "")}</td>
        <td><button type="button" class="admin-btn primary btn-use-service" data-key="${esc(key)}"
          data-source="${esc(src)}" data-service-id="${item.service_id ?? item.serviceId}"
          data-schedule-id="${sch != null && sch !== "" ? esc(sch) : ""}"
          data-membership-purchase-id="${mid != null && mid !== "" ? esc(mid) : ""}"
          data-service-purchase-id="${sid != null && sid !== "" ? esc(sid) : ""}"
          data-usable="${usable ? "1" : "0"}"
          ${usable ? "" : "disabled"}>Использовать</button></td>`;
        tbody.appendChild(tr);
      }
    })
    .catch((e) => notify(formatApiErrors(e.fields) || e.message, "Не удалось загрузить услуги"));
}

document.addEventListener("DOMContentLoaded", () => {
  document.getElementById("btn-open-entry-modal")?.addEventListener("click", () => openVisitsModal("modal-entry"));
  document.getElementById("btn-open-services-modal")?.addEventListener("click", () => openVisitsModal("modal-services"));
  document.getElementById("btn-open-exit-modal")?.addEventListener("click", () => openVisitsModal("modal-exit"));
  document.querySelectorAll("[data-visits-modal-dismiss]").forEach((el) => {
    el.addEventListener("click", () => closeVisitsModal());
  });

  Promise.all([loadClients(), loadClubStats(), loadJournal(), refreshOpenClientSelects()]).catch((e) => {
    notify(formatApiErrors(e.fields) || e.message, "Не удалось загрузить данные");
  });

  document.getElementById("entryForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const clientId = Number(document.getElementById("entry-client").value);
    const visitType = document.querySelector('input[name="visit_type"]:checked')?.value || "обычный";
    if (!clientId) {
      notify("Выберите клиента.");
      return;
    }
    try {
      await apiRequest("/visits/entry", {
        method: "POST",
        body: JSON.stringify({ client_id: clientId, visit_type: visitType }),
      });
      await Promise.all([loadClubStats(), loadJournal(), refreshOpenClientSelects()]);
      const selSvc = document.getElementById("services-client");
      if (selSvc) selSvc.value = String(clientId);
      openVisitsModal("modal-services");
      notify(
        "Клиент успешно вошёл в клуб. Выберите услугу и нажмите «Использовать».",
        "Успешно"
      );
      renderAvailableServices(clientId);
    } catch (err) {
      notify(formatApiErrors(err.fields) || err.message, "Не удалось выполнить вход");
    }
  });

  document.getElementById("services-client")?.addEventListener("change", () => {
    const clientId = Number(document.getElementById("services-client").value);
    renderAvailableServices(clientId || null);
  });

  document.addEventListener("click", async (e) => {
    const btn = e.target.closest(".btn-use-service");
    if (!btn || !document.getElementById("available-services-tbody")?.contains(btn)) return;
    if (btn.disabled || btn.getAttribute("data-usable") === "0") {
      notify("Сейчас нельзя зафиксировать эту услугу.");
      return;
    }
    const clientSel = document.getElementById("services-client");
    const clientId = readSelectInt(clientSel);
    if (!clientId) {
      notify("Выберите клиента с открытым визитом.");
      return;
    }
    const source = btn.getAttribute("data-source") || "";
    const serviceId = readDataInt(btn, "service-id");
    if (!serviceId) {
      notify("Не удалось определить услугу. Обновите список.");
      return;
    }
    const body = {
      client_id: clientId,
      source,
      service_id: serviceId,
      schedule_id: readDataInt(btn, "schedule-id"),
    };
    if (source === "абонемент") {
      const membershipPurchaseId = readDataInt(btn, "membership-purchase-id");
      if (!membershipPurchaseId) {
        notify("Не указана покупка абонемента. Обновите список.");
        return;
      }
      body.membership_purchase_id = membershipPurchaseId;
    } else {
      const servicePurchaseId = readDataInt(btn, "service-purchase-id");
      if (!servicePurchaseId) {
        notify("Не указана разовая покупка. Обновите список.");
        return;
      }
      body.service_purchase_id = servicePurchaseId;
    }
    btn.disabled = true;
    try {
      await apiRequest("/visits/use-service", { method: "POST", body: JSON.stringify(body) });
      notify("Данные доступа к услуге зафиксированы.", "Успешно");
      renderAvailableServices(clientId);
    } catch (err) {
      notify(formatApiErrors(err.fields) || err.message, "Не удалось зафиксировать услугу");
      btn.disabled = btn.getAttribute("data-usable") === "1";
    }
  });

  document.getElementById("btn-exit")?.addEventListener("click", async () => {
    const exitSel = document.getElementById("exit-client");
    const clientId = Number(exitSel.value);
    if (!clientId) {
      notify("Выберите клиента с открытым визитом.");
      return;
    }
    try {
      await apiRequest("/visits/exit", { method: "POST", body: JSON.stringify({ client_id: clientId }) });
      notify("Выход зафиксирован.", "Успешно");
      await Promise.all([loadClubStats(), loadJournal(), refreshOpenClientSelects()]);
      exitSel.value = "";
      renderAvailableServices(null);
    } catch (err) {
      notify(formatApiErrors(err.fields) || err.message, "Не удалось зафиксировать выход");
    }
  });
});
