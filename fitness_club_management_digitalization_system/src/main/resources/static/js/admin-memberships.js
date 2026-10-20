const API_BASE = (document.body.dataset.apiBase || "/api/admin").replace(/\/$/, "");

let membershipsCache = [];
let servicesCache = [];

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
    err.status = r.status;
    throw err;
  }
  if (r.status === 204) return null;
  return data;
}

const MEMBERSHIP_FIELD_LABELS = {
  membership_name: "Название",
  description: "Описание",
  price: "Цена",
  duration_days: "Срок (дней)",
  visits_limit: "Лимит посещений",
  freeze_days: "Заморозка",
  guest_visits: "Гостевые визиты",
  parking: "Парковка",
  towel: "Полотенце",
  is_active: "Активен",
  service_ids: "Услуги",
  membership_id: "Абонемент",
};

function formatApiErrors(fields) {
  if (!fields || typeof fields !== "object") return "";
  return Object.entries(fields)
    .map(([k, v]) => `${MEMBERSHIP_FIELD_LABELS[k] || k}: ${v}`)
    .join("\n");
}

const NAME_ALLOWED_RE = /^[\p{L}\p{N}]+$/u;
/** Буквы, цифры, пробелы и знаки . , ; : */
const DESC_ALLOWED_RE = /^[\p{L}\p{N}\s.,;:]+$/u;

function validateMembershipClientSide(payload) {
  const lines = [];
  const name = payload.membership_name || "";
  if (name.length > 50) lines.push("Название: не длиннее 50 символов.");
  if (name && !NAME_ALLOWED_RE.test(name)) lines.push("Название: только буквы и цифры.");
  const desc = payload.description || "";
  if (desc.length > 100) lines.push("Описание: не длиннее 100 символов.");
  if (desc && !DESC_ALLOWED_RE.test(desc))
    lines.push("Описание: только буквы, цифры, пробелы и знаки . , ; :");
  if (payload.price < 0 || payload.price > 1_000_000) lines.push("Цена: от 0 до 1 000 000 ₽.");
  if (payload.duration_days < 1 || payload.duration_days > 5 * 365) lines.push("Срок: от 1 до 1825 дней (5 лет).");
  const fd = payload.freeze_days;
  if (fd != null && (fd < 0 || fd > 365)) lines.push("Заморозка: от 0 до 365 дней.");
  if (fd != null && fd >= payload.duration_days) lines.push("Заморозка должна быть меньше срока действия.");
  const gv = payload.guest_visits;
  const vl = payload.visits_limit;
  if (gv != null && vl != null && gv > vl) lines.push("Гостевые визиты не больше лимита посещений.");
  return lines.length ? lines.join("\n") : "";
}

function money(v) {
  return `${Number(v).toLocaleString("ru-RU")} ₽`;
}

function membershipIsActive(m) {
  if (typeof m?.active === "boolean") return m.active;
  const raw = m?.is_active ?? m?.isActive ?? m?.active;
  if (raw === true || raw === false) return raw;
  if (raw === "true" || raw === "1") return true;
  if (raw === "false" || raw === "0") return false;
  return true;
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

function setFormError(msg) {
  fillAdminAlertError(document.getElementById("membershipFormError"), "Не удалось сохранить", msg);
}

function serviceIdOf(s) {
  return s.service_id ?? s.serviceId;
}

function serviceNameOf(s) {
  return s.service_name ?? s.serviceName ?? "";
}

function getMembershipFormPayload() {
  const serviceIds = [...document.querySelectorAll("input[data-service-id]:checked")].map((el) =>
    Number(el.dataset.serviceId),
  );
  return {
    membership_name: document.getElementById("membership_name").value.trim(),
    description: document.getElementById("description").value.trim(),
    price: Number(document.getElementById("price").value),
    duration_days: Number(document.getElementById("duration_days").value),
    visits_limit: document.getElementById("visits_limit").value === "" ? null : Number(document.getElementById("visits_limit").value),
    freeze_days:
      document.getElementById("freeze_days").value === "" ? null : Number(document.getElementById("freeze_days").value),
    guest_visits:
      document.getElementById("guest_visits").value === "" ? null : Number(document.getElementById("guest_visits").value),
    parking: document.getElementById("parking").value === "true",
    towel: document.getElementById("towel").value === "true",
    is_active: document.getElementById("is_active").value === "true",
    service_ids: serviceIds,
  };
}

function setMembershipFormData(m) {
  document.getElementById("membership_id").value = m?.membership_id ?? m?.membershipId ?? "";
  document.getElementById("membership_name").value = m?.membership_name ?? m?.membershipName ?? "";
  document.getElementById("description").value = m?.description ?? "";
  document.getElementById("price").value = m?.price ?? 0;
  document.getElementById("duration_days").value = m?.duration_days ?? m?.durationDays ?? 0;
  document.getElementById("visits_limit").value =
    m?.visits_limit != null && m?.visits_limit !== undefined
      ? String(m.visits_limit)
      : m?.visitsLimit != null
        ? String(m.visitsLimit)
        : "";
  const fd = m?.freeze_days ?? m?.freezeDays;
  document.getElementById("freeze_days").value = fd != null ? String(fd) : "";
  const gv = m?.guest_visits ?? m?.guestVisits;
  document.getElementById("guest_visits").value = gv != null ? String(gv) : "";
  document.getElementById("parking").value = String(Boolean(m?.parking));
  document.getElementById("towel").value = String(Boolean(m?.towel));
  document.getElementById("is_active").value = String(membershipIsActive(m));

  const ids = new Set((m?.service_ids ?? m?.serviceIds ?? []).map(Number));
  document.querySelectorAll("input[data-service-id]").forEach((el) => {
    el.checked = ids.has(Number(el.dataset.serviceId));
  });
}

function clearMembershipForm() {
  document.getElementById("membership_id").value = "";
  document.getElementById("membership_name").value = "";
  document.getElementById("description").value = "";
  document.getElementById("price").value = 0;
  document.getElementById("duration_days").value = 30;
  document.getElementById("visits_limit").value = "";
  document.getElementById("freeze_days").value = "";
  document.getElementById("guest_visits").value = "";
  document.getElementById("parking").value = "false";
  document.getElementById("towel").value = "false";
  document.getElementById("is_active").value = "true";
  document.querySelectorAll("input[data-service-id]").forEach((el) => (el.checked = false));
}

function renderMembershipTable(list) {
  const tbody = document.getElementById("memberships-tbody");
  tbody.innerHTML = "";

  if (!list.length) {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td colspan="10" class="muted">Ничего не найдено</td>`;
    tbody.appendChild(tr);
    return;
  }

  for (const m of list) {
    const mid = m.membership_id ?? m.membershipId;
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>
        <div style="font-weight:950;">${esc(m.membership_name ?? m.membershipName)}</div>
        <div class="admin-small">${esc((m.description || "").slice(0, 50))}</div>
      </td>
      <td>${esc(money(m.price))}</td>
      <td>${esc(m.duration_days ?? m.durationDays)}</td>
      <td>${esc((m.visits_limit ?? m.visitsLimit) == null ? "Безлимит" : m.visits_limit ?? m.visitsLimit)}</td>
      <td>${esc(m.freeze_days ?? m.freezeDays ?? "—")}</td>
      <td>${esc(m.guest_visits ?? m.guestVisits ?? "—")}</td>
      <td>${esc((m.parking ? "Да" : "Нет"))}</td>
      <td>${esc((m.towel ? "Да" : "Нет"))}</td>
      <td>${esc(membershipIsActive(m) ? "Да" : "Нет")}</td>
      <td>
        <div class="admin-actions" style="justify-content:flex-start;">
          <button class="admin-btn secondary" type="button" data-action="edit" data-id="${esc(mid)}">Редактировать</button>
          <button class="admin-btn danger" type="button" data-action="delete" data-id="${esc(mid)}">Удалить</button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  }
}

function renderServiceCheckboxes() {
  const box = document.getElementById("membership-services-checkboxes");
  box.innerHTML = "";
  for (const s of servicesCache) {
    const id = serviceIdOf(s);
    const label = document.createElement("label");
    label.innerHTML = `
      <input type="checkbox" data-service-id="${esc(id)}" /> ${esc(serviceNameOf(s))}
    `;
    box.appendChild(label);
  }
}

function applySearch() {
  const q = document.getElementById("q-name").value.trim().toLowerCase();
  const filtered = membershipsCache.filter((m) => {
    const name = (m.membership_name ?? m.membershipName ?? "").toLowerCase();
    return !q || name.includes(q);
  });
  renderMembershipTable(filtered);
}

async function loadAll() {
  fillAdminAlertError(document.getElementById("membership-load-error"), "", "");
  const [memberships, services] = await Promise.all([apiRequest("/memberships"), apiRequest("/services")]);
  membershipsCache = Array.isArray(memberships) ? memberships : [];
  servicesCache = Array.isArray(services) ? services : [];
  renderServiceCheckboxes();
  applySearch();
}

document.addEventListener("DOMContentLoaded", () => {
  loadAll().catch((e) => {
    fillAdminAlertError(
      document.getElementById("membership-load-error"),
      "Ошибка загрузки",
      formatApiErrors(e.fields) || e.message,
    );
    document.getElementById("memberships-tbody").innerHTML =
      `<tr><td colspan="10" class="muted">Не удалось загрузить данные</td></tr>`;
  });

  document.getElementById("btn-search")?.addEventListener("click", applySearch);
  document.getElementById("btn-clear-search")?.addEventListener("click", () => {
    document.getElementById("q-name").value = "";
    applySearch();
  });

  document.getElementById("memberships-tbody")?.addEventListener("click", async (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;
    const action = btn.dataset.action;
    const id = Number(btn.dataset.id);
    const item = membershipsCache.find((x) => (x.membership_id ?? x.membershipId) === id);
    if (!item) return;

    if (action === "edit") {
      setMembershipFormData(item);
      setFormError("");
      document.getElementById("membershipForm")?.scrollIntoView({ behavior: "smooth", block: "start" });
      return;
    }

    if (action === "delete") {
      if (!confirm(`Удалить абонемент «${item.membership_name ?? item.membershipName}»?`)) return;
      try {
        await apiRequest(`/memberships/${id}`, { method: "DELETE" });
        await loadAll();
        clearMembershipForm();
        setFormError("");
      } catch (err) {
        alert(formatApiErrors(err.fields) || err.message);
      }
    }
  });

  document.getElementById("btn-reset-membership")?.addEventListener("click", () => {
    clearMembershipForm();
    setFormError("");
  });

  document.getElementById("membershipForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    setFormError("");
    if (!e.target.checkValidity()) {
      e.target.reportValidity();
      return;
    }
    const sid = document.getElementById("membership_id").value;
    const isCreate = !sid;
    const payload = getMembershipFormPayload();
    const clientErr = validateMembershipClientSide(payload);
    if (clientErr) {
      if (isCreate) {
        alert(clientErr);
        setFormError("");
      } else {
        setFormError(clientErr);
      }
      return;
    }
    try {
      if (isCreate) {
        await apiRequest("/memberships", { method: "POST", body: JSON.stringify(payload) });
      } else {
        await apiRequest(`/memberships/${Number(sid)}`, { method: "PUT", body: JSON.stringify(payload) });
      }
      clearMembershipForm();
      await loadAll();
    } catch (err) {
      const msg = formatApiErrors(err.fields) || err.message;
      if (isCreate) {
        alert(msg);
        setFormError("");
      } else {
        setFormError(msg);
      }
    }
  });
});
