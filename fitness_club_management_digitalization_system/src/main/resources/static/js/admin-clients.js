const API_BASE = (document.body.dataset.apiBase || "/api/admin").replace(/\/$/, "");

let clientsCache = [];
let editingClientId = null;
let membershipManageCache = null;

function esc(s) {
  return String(s ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#039;");
}

function fullName(c) {
  const m = c.middle_name ? ` ${c.middle_name}` : "";
  return `${c.last_name} ${c.first_name}${m}`.trim();
}

/** Фамилия для поиска: только поле фамилии, без имени/отчества. */
function clientLastNameNorm(c) {
  const raw = c?.last_name ?? c?.lastName ?? "";
  return String(raw).trim().toLowerCase().replace(/\s+/g, " ");
}

function clientIsActive(c) {
  if (typeof c?.is_active === "boolean") return c.is_active;
  if (typeof c?.active === "boolean") return c.active;
  if (typeof c?.isActive === "boolean") return c.isActive;
  const raw = c?.is_active ?? c?.active ?? c?.isActive ?? "";
  if (typeof raw === "number") return raw !== 0;
  const s = String(raw).trim().toLowerCase();
  return s === "true" || s === "1" || s === "yes" || s === "да";
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
  return data;
}

function formatApiErrors(fields) {
  if (!fields || typeof fields !== "object") return "";
  return Object.entries(fields)
    .map(([k, v]) => `${k}: ${v}`)
    .join("\n");
}

function showErrorAlert(msg) {
  if (msg) alert(msg);
}

async function loadClients() {
  clientsCache = await apiRequest("/clients");
  applySearch();
}

/** Типы документов, доступные в модальном окне клиента (остальные в БД не показываем здесь). */
const CLIENT_MODAL_DOCUMENT_TYPES = new Set(["PASSPORT_RF", "STUDENT_ID", "PENSION_CERT"]);

const DOCUMENT_TYPE_LABELS = {
  PASSPORT_RF: "Паспорт РФ",
  STUDENT_ID: "Студенческий билет",
  PENSION_CERT: "Пенсионное удостоверение",
};

function setModalDocumentFormError(msg) {
  showErrorAlert(msg);
}

function toggleFieldWrap(id, shown) {
  const el = document.getElementById(id);
  if (!el) return;
  el.style.display = shown ? "" : "none";
}

function applyClientDocumentTypeVisibility() {
  const type = document.getElementById("mdoc_type")?.value || "PASSPORT_RF";
  const isStudent = type === "STUDENT_ID";
  const isPension = type === "PENSION_CERT";
  const showSeries = !isStudent && !isPension;
  const showIssuedBy = !isPension;
  const showIssueDate = !isPension;
  const showExpiryDate = !isPension;

  toggleFieldWrap("mdoc_field_series", showSeries);
  toggleFieldWrap("mdoc_field_issued_by", showIssuedBy);
  toggleFieldWrap("mdoc_field_issue_date", showIssueDate);
  toggleFieldWrap("mdoc_field_expiry_date", showExpiryDate);
  toggleFieldWrap("mdoc_field_notes", true);

  if (!showSeries) document.getElementById("mdoc_series").value = "";
  if (!showIssuedBy) document.getElementById("mdoc_issued_by").value = "";
  if (!showIssueDate) document.getElementById("mdoc_issue_date").value = "";
  if (!showExpiryDate) document.getElementById("mdoc_expiry_date").value = "";
}

function getModalDocumentFormPayload() {
  return {
    document_type: document.getElementById("mdoc_type").value,
    series: document.getElementById("mdoc_series").value.trim() || null,
    number: document.getElementById("mdoc_number").value.trim(),
    issued_by: document.getElementById("mdoc_issued_by").value.trim() || null,
    issue_date: document.getElementById("mdoc_issue_date").value || null,
    expiry_date: document.getElementById("mdoc_expiry_date").value || null,
    notes: document.getElementById("mdoc_notes").value.trim() || null,
    is_verified: document.getElementById("mdoc_is_verified").checked,
  };
}

function normalizeModalDocumentType(type) {
  const t = type == null ? "" : String(type).trim();
  return CLIENT_MODAL_DOCUMENT_TYPES.has(t) ? t : "PASSPORT_RF";
}

function setModalDocumentFormFromDto(d) {
  document.getElementById("mdoc_id").value = d?.document_id ?? "";
  document.getElementById("mdoc_type").value = normalizeModalDocumentType(d?.document_type);
  document.getElementById("mdoc_series").value = d?.series ?? "";
  document.getElementById("mdoc_number").value = d?.number ?? "";
  document.getElementById("mdoc_issued_by").value = d?.issued_by ?? "";
  document.getElementById("mdoc_issue_date").value = d?.issue_date ?? "";
  document.getElementById("mdoc_expiry_date").value = d?.expiry_date ?? "";
  document.getElementById("mdoc_notes").value = d?.notes ?? "";
  document.getElementById("mdoc_is_verified").checked = !!d?.is_verified;
  document.getElementById("btn-delete-mdoc").style.display = d?.document_id ? "" : "none";
  applyClientDocumentTypeVisibility();
}

function clearModalDocumentForm() {
  setModalDocumentFormFromDto(null);
}

async function refreshDocumentsListModal(clientId) {
  const tbody = document.getElementById("documents-modal-tbody");
  if (!tbody) return;
  tbody.innerHTML = `<tr><td colspan="8" class="muted">Загрузка…</td></tr>`;
  try {
    const rawList = await apiRequest(`/clients/${clientId}/documents`);
    const list = (Array.isArray(rawList) ? rawList : []).filter((d) =>
      CLIENT_MODAL_DOCUMENT_TYPES.has(String(d?.document_type ?? "").trim())
    );
    tbody.innerHTML = "";
    if (!list.length) {
      tbody.innerHTML = `<tr><td colspan="8" class="muted">Нет документов.</td></tr>`;
      return;
    }
    for (const d of list) {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>${esc(DOCUMENT_TYPE_LABELS[d.document_type] ?? d.document_type)}</td>
        <td>${esc(d.series ?? "—")}</td>
        <td>${esc(d.number)}</td>
        <td>${esc(d.issued_by ?? "—")}</td>
        <td>${esc(d.issue_date ?? "—")}</td>
        <td>${esc(d.expiry_date ?? "—")}</td>
        <td>${d.is_verified ? "Да" : "Нет"}</td>
        <td>
          <div class="admin-actions" style="justify-content:flex-start;">
            <button class="admin-btn secondary" type="button" data-action="edit-doc" data-id="${d.document_id}">Изменить</button>
            <button class="admin-btn danger" type="button" data-action="delete-doc" data-id="${d.document_id}">Удалить</button>
          </div>
        </td>
      `;
      tbody.appendChild(tr);
    }
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="8" class="error-message">Ошибка загрузки документов</td></tr>`;
  }
}

function getAddClientPayload() {
  return {
    last_name: document.getElementById("add_last_name").value.trim(),
    first_name: document.getElementById("add_first_name").value.trim(),
    middle_name: document.getElementById("add_middle_name").value.trim() || null,
    birth_date: document.getElementById("add_birth_date").value,
    phone: document.getElementById("add_phone").value.trim(),
    email: document.getElementById("add_email").value.trim(),
    gender: document.getElementById("add_gender").value,
    is_active: true,
    registration_address: document.getElementById("add_registration_address").value.trim(),
    actual_address: document.getElementById("add_actual_address").value.trim() || null,
    password: document.getElementById("add_password").value,
    password_confirm: document.getElementById("add_password_confirm").value,
  };
}

function clearAddClientForm() {
  document.getElementById("add_last_name").value = "";
  document.getElementById("add_first_name").value = "";
  document.getElementById("add_middle_name").value = "";
  document.getElementById("add_birth_date").value = "";
  document.getElementById("add_phone").value = "";
  document.getElementById("add_email").value = "";
  document.getElementById("add_gender").value = "Мужской";
  document.getElementById("add_registration_address").value = "";
  document.getElementById("add_actual_address").value = "";
  document.getElementById("add_password").value = "";
  document.getElementById("add_password_confirm").value = "";
}

function getEditClientPayload() {
  const id = Number(document.getElementById("edit_client_id").value);
  const cached = clientsCache.find((x) => x.client_id === id);
  const np = document.getElementById("edit_new_password").value;
  const npc = document.getElementById("edit_new_password_confirm").value;
  const payload = {
    last_name: document.getElementById("edit_last_name").value.trim(),
    first_name: document.getElementById("edit_first_name").value.trim(),
    middle_name: document.getElementById("edit_middle_name").value.trim() || null,
    birth_date: document.getElementById("edit_birth_date").value,
    phone: document.getElementById("edit_phone").value.trim(),
    email: document.getElementById("edit_email").value.trim(),
    gender: document.getElementById("edit_gender").value,
    is_active: cached ? clientIsActive(cached) : false,
    registration_address: document.getElementById("edit_registration_address").value.trim(),
    actual_address: document.getElementById("edit_actual_address").value.trim() || null,
  };
  if (np) {
    payload.new_password = np;
    payload.new_password_confirm = npc;
  }
  return payload;
}

function setEditClientFormData(c) {
  document.getElementById("edit_client_id").value = c.client_id ?? "";
  document.getElementById("edit_personal_data_id").value = c.personal_data_id ?? "";
  document.getElementById("edit_last_name").value = c.last_name ?? "";
  document.getElementById("edit_first_name").value = c.first_name ?? "";
  document.getElementById("edit_middle_name").value = c.middle_name ?? "";
  document.getElementById("edit_birth_date").value = c.birth_date ?? "";
  document.getElementById("edit_phone").value = c.phone ?? "";
  document.getElementById("edit_email").value = c.email ?? "";
  document.getElementById("edit_gender").value = c.gender ?? "Мужской";
  document.getElementById("edit_registration_address").value = c.registration_address ?? "";
  document.getElementById("edit_actual_address").value = c.actual_address ?? "";
  document.getElementById("edit_new_password").value = "";
  document.getElementById("edit_new_password_confirm").value = "";
}

function getEditingClient() {
  if (!editingClientId) return null;
  return clientsCache.find((x) => x.client_id === editingClientId) ?? null;
}

const modalPanels = [
  "modal-edit-menu",
  "modal-personal",
  "modal-documents",
  "modal-add-from-trainer",
  "modal-membership-management",
  "modal-membership-rules",
  "modal-membership-freeze",
];

function showModalPanel(panelId) {
  for (const id of modalPanels) {
    const el = document.getElementById(id);
    if (!el) continue;
    el.hidden = id !== panelId;
  }
}

function openModalRoot() {
  const root = document.getElementById("admin-modal-root");
  if (root) {
    root.hidden = false;
    root.setAttribute("aria-hidden", "false");
  }
}

function closeModalRoot() {
  const root = document.getElementById("admin-modal-root");
  if (root) {
    root.hidden = true;
    root.setAttribute("aria-hidden", "true");
  }
  editingClientId = null;
  membershipManageCache = null;
  modalPanels.forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.hidden = true;
  });
}

function renderMembershipInfo(payload) {
  const infoTbody = document.getElementById("admin-membership-info-tbody");
  const rulesTbody = document.getElementById("admin-membership-rules-tbody");
  const detailsBtn = document.getElementById("btn-admin-membership-details");
  const freezeBtn = document.getElementById("btn-freeze-membership");
  const unfreezeBtn = document.getElementById("btn-unfreeze-membership");
  const annulBtn = document.getElementById("btn-annul-membership");
  if (!infoTbody || !rulesTbody) return;

  const purchase = payload?.membership || null;
  const rules = Array.isArray(payload?.rules) ? payload.rules : [];
  const isFrozen = !!payload?.is_frozen;

  if (!purchase) {
    infoTbody.innerHTML =
      '<tr><td colspan="6" class="muted">У клиента нет активного абонемента.</td></tr>';
    rulesTbody.innerHTML = "";
    if (detailsBtn) detailsBtn.disabled = true;
    if (freezeBtn) {
      freezeBtn.disabled = true;
      freezeBtn.style.display = "";
    }
    if (unfreezeBtn) unfreezeBtn.style.display = "none";
    if (annulBtn) annulBtn.disabled = true;
    return;
  }

  if (detailsBtn) detailsBtn.disabled = false;
  if (freezeBtn) {
    freezeBtn.disabled = false;
    freezeBtn.style.display = isFrozen ? "none" : "";
  }
  if (annulBtn) annulBtn.disabled = false;
  if (unfreezeBtn) {
    unfreezeBtn.style.display = isFrozen ? "" : "none";
    unfreezeBtn.disabled = !isFrozen;
  }

  infoTbody.innerHTML = `
    <tr>
      <td>${esc(purchase.membership_name ?? "—")}</td>
      <td>${esc(String(purchase.remaining_freeze_days ?? 0))}</td>
      <td>${esc(String(purchase.remaining_guest_visits ?? 0))}</td>
      <td>${esc(purchase.start_date ?? "—")}</td>
      <td>${esc(purchase.end_date ?? "—")}</td>
      <td>${esc(purchase.status ?? "—")}</td>
    </tr>
  `;

  rulesTbody.innerHTML = "";
  if (!rules.length) {
    rulesTbody.innerHTML = '<tr><td colspan="7" class="muted">Нет привязанных услуг по абонементу.</td></tr>';
    return;
  }
  rules.forEach((ms) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc(ms.service_name ?? "—")}</td>
      <td>${esc(ms.included_visits ?? "—")}</td>
      <td>${esc(ms.remaining ?? "—")}</td>
      <td>${esc(ms.max_per_week != null ? String(ms.max_per_week) : "—")}</td>
      <td>${esc(ms.days_of_week || "—")}</td>
      <td>${esc(ms.time_start || "—")}</td>
      <td>${esc(ms.time_end || "—")}</td>
    `;
    rulesTbody.appendChild(tr);
  });
}

async function openMembershipManagement() {
  const c = getEditingClient();
  if (!c) return;
  openModalRoot();
  try {
    const payload = await apiRequest(`/clients/${c.client_id}/membership`);
    membershipManageCache = payload || {};
    renderMembershipInfo(membershipManageCache);
    showModalPanel("modal-membership-management");
  } catch (err) {
    showErrorAlert(formatApiErrors(err.fields) || err.message);
  }
}

function openEditMenu(clientId) {
  const c = clientsCache.find((x) => x.client_id === clientId);
  if (!c) return;
  editingClientId = clientId;
  document.getElementById("modal-client-name").textContent = fullName(c);
  openModalRoot();
  showModalPanel("modal-edit-menu");
}

async function openAddFromTrainerModal() {
  const select = document.getElementById("select-trainer-for-client");
  if (!select) return;
  select.innerHTML = `<option value="">Загрузка...</option>`;
  openModalRoot();
  showModalPanel("modal-add-from-trainer");
  try {
    const trainers = await apiRequest("/clients/trainers-available");
    select.innerHTML = `<option value="">Выберите тренера</option>`;
    for (const t of trainers || []) {
      const option = document.createElement("option");
      option.value = String(t.trainer_id);
      option.textContent = `${t.full_name}${t.specialization ? ` (${t.specialization})` : ""}`;
      select.appendChild(option);
    }
  } catch (err) {
    select.innerHTML = `<option value="">Не удалось загрузить список тренеров</option>`;
  }
}

function renderClients(list) {
  const tbody = document.getElementById("clients-tbody");
  tbody.innerHTML = "";

  if (!list.length) {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td colspan="6" class="muted">Ничего не найдено</td>`;
    tbody.appendChild(tr);
    return;
  }

  for (const c of list) {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc(fullName(c))}</td>
      <td>${esc(c.birth_date)}</td>
      <td>${esc(c.phone)}</td>
      <td>${esc(c.email)}</td>
      <td>${esc(c.gender)}</td>
      <td>
        <div class="admin-actions" style="justify-content:flex-start;">
          <button class="admin-btn secondary" type="button" data-action="edit" data-id="${c.client_id}">Редактировать</button>
          <button class="admin-btn secondary" type="button" data-action="membership" data-id="${c.client_id}">Управлять абонементом</button>
          <button class="admin-btn danger" type="button" data-action="delete" data-id="${c.client_id}">Удалить</button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  }
}

function applySearch() {
  const qLast = document
    .getElementById("q-lastname")
    .value.trim()
    .toLowerCase()
    .replace(/\s+/g, " ");
  const qEmail = document.getElementById("q-email").value.trim().toLowerCase();
  const qPhone = document.getElementById("q-phone").value.trim().toLowerCase().replace(/\D/g, "");
  const qGender = document.getElementById("q-gender").value;

  const filtered = clientsCache.filter((c) => {
    const okLast = !qLast || clientLastNameNorm(c).includes(qLast);
    const okEmail = !qEmail || (c.email || "").toLowerCase().includes(qEmail);
    const phoneDigits = String(c.phone || "").replace(/\D/g, "");
    const okPhone = !qPhone || phoneDigits.includes(qPhone);
    const okGender = !qGender || c.gender === qGender;
    return okLast && okEmail && okPhone && okGender;
  });

  renderClients(filtered);
}

document.addEventListener("DOMContentLoaded", () => {
  loadClients().catch((e) => {
    showErrorAlert(e?.message || "Не удалось загрузить клиентов. Проверьте БД и авторизацию.");
    document.getElementById("clients-tbody").innerHTML =
      `<tr><td colspan="6" class="error-message">Не удалось загрузить клиентов. Проверьте БД и авторизацию.</td></tr>`;
  });

  document.getElementById("btn-search")?.addEventListener("click", applySearch);
  document.getElementById("btn-clear-search")?.addEventListener("click", () => {
    document.getElementById("q-lastname").value = "";
    document.getElementById("q-email").value = "";
    document.getElementById("q-phone").value = "";
    document.getElementById("q-gender").value = "";
    applySearch();
  });

  document.getElementById("clients-tbody")?.addEventListener("click", async (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;
    const action = btn.dataset.action;
    const id = Number(btn.dataset.id);
    const item = clientsCache.find((x) => x.client_id === id);
    if (!item) return;

    if (action === "edit") {
      openEditMenu(id);
      return;
    }

    if (action === "membership") {
      editingClientId = id;
      await openMembershipManagement();
      return;
    }

    if (action === "delete") {
      if (!confirm(`Удалить клиента ${fullName(item)}?`)) return;
      try {
        await apiRequest(`/clients/${id}`, { method: "DELETE" });
        await loadClients();
        if (editingClientId === id) closeModalRoot();
      } catch (err) {
        showErrorAlert(formatApiErrors(err.fields) || err.message);
      }
    }
  });

  document.querySelectorAll("[data-modal-dismiss=\"root\"]").forEach((el) => {
    el.addEventListener("click", () => closeModalRoot());
  });

  document.getElementById("btn-open-edit-personal")?.addEventListener("click", () => {
    const c = getEditingClient();
    if (!c) return;
    setEditClientFormData(c);
    showModalPanel("modal-personal");
  });

  document.getElementById("btn-back-personal-to-menu")?.addEventListener("click", () => {
    showModalPanel("modal-edit-menu");
  });

  document.getElementById("btn-open-edit-docs")?.addEventListener("click", async () => {
    const c = getEditingClient();
    if (!c) return;
    document.getElementById("mdoc_personal_data_id").value = String(c.personal_data_id);
    clearModalDocumentForm();
    await refreshDocumentsListModal(c.client_id);
    showModalPanel("modal-documents");
  });

  document.getElementById("btn-back-docs-to-menu")?.addEventListener("click", () => {
    showModalPanel("modal-edit-menu");
  });

  document.getElementById("btn-back-membership-to-menu")?.addEventListener("click", () => {
    showModalPanel("modal-edit-menu");
  });

  document.getElementById("btn-admin-membership-details")?.addEventListener("click", () => {
    showModalPanel("modal-membership-rules");
  });

  document.getElementById("btn-back-membership-rules")?.addEventListener("click", () => {
    showModalPanel("modal-membership-management");
  });

  document.getElementById("btn-freeze-membership")?.addEventListener("click", () => {
    const remaining = Number(membershipManageCache?.membership?.remaining_freeze_days ?? 0);
    if (remaining <= 0) {
      showErrorAlert("Количество дней заморозки истекло.");
      return;
    }
    const input = document.getElementById("membership-freeze-days");
    if (input) {
      input.value = "";
      input.min = "1";
      input.max = String(Math.max(0, remaining));
    }
    showModalPanel("modal-membership-freeze");
  });

  document.getElementById("btn-back-freeze-membership")?.addEventListener("click", () => {
    showModalPanel("modal-membership-management");
  });

  document.getElementById("btn-confirm-freeze-membership")?.addEventListener("click", async () => {
    const c = getEditingClient();
    if (!c) return;
    const remaining = Number(membershipManageCache?.membership?.remaining_freeze_days ?? 0);
    const raw = document.getElementById("membership-freeze-days")?.value ?? "";
    const days = Number(raw);
    if (!Number.isInteger(days) || days < 1 || days > remaining) {
      showErrorAlert(`Введите целое число от 1 до ${remaining}.`);
      return;
    }
    try {
      const payload = await apiRequest(`/clients/${c.client_id}/membership/freeze`, {
        method: "POST",
        body: JSON.stringify({ days }),
      });
      membershipManageCache = payload || {};
      renderMembershipInfo(membershipManageCache);
      showModalPanel("modal-membership-management");
    } catch (err) {
      showErrorAlert(formatApiErrors(err.fields) || err.message);
    }
  });

  document.getElementById("btn-unfreeze-membership")?.addEventListener("click", async () => {
    const c = getEditingClient();
    if (!c) return;
    try {
      const payload = await apiRequest(`/clients/${c.client_id}/membership/unfreeze`, {
        method: "POST",
      });
      membershipManageCache = payload || {};
      renderMembershipInfo(membershipManageCache);
    } catch (err) {
      showErrorAlert(formatApiErrors(err.fields) || err.message);
    }
  });

  document.getElementById("btn-annul-membership")?.addEventListener("click", async () => {
    const c = getEditingClient();
    if (!c) return;
    if (!confirm("Аннулировать абонемент клиента?")) return;
    try {
      await apiRequest(`/clients/${c.client_id}/membership`, { method: "DELETE" });
      membershipManageCache = { membership: null, rules: [], is_frozen: false };
      renderMembershipInfo(membershipManageCache);
    } catch (err) {
      showErrorAlert(formatApiErrors(err.fields) || err.message);
    }
  });

  document.getElementById("mdoc_type")?.addEventListener("change", () => applyClientDocumentTypeVisibility());

  document.getElementById("editClientForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.target;
    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }
    const id = Number(document.getElementById("edit_client_id").value);
    try {
      await apiRequest(`/clients/${id}`, {
        method: "PUT",
        body: JSON.stringify(getEditClientPayload()),
      });
      await loadClients();
      const updated = clientsCache.find((x) => x.client_id === id);
      if (updated) document.getElementById("modal-client-name").textContent = fullName(updated);
      applySearch();
    } catch (err) {
      showErrorAlert(formatApiErrors(err.fields) || err.message);
    }
  });

  document.getElementById("btn-reset-add-client")?.addEventListener("click", () => {
    clearAddClientForm();
  });

  document.getElementById("btn-open-add-from-trainer")?.addEventListener("click", () => {
    openAddFromTrainerModal();
  });

  document.getElementById("btn-create-client-from-trainer")?.addEventListener("click", async () => {
    const select = document.getElementById("select-trainer-for-client");
    const trainerId = Number(select?.value || "");
    if (!trainerId) {
      showErrorAlert("Выберите тренера.");
      return;
    }
    try {
      await apiRequest(`/clients/from-trainer/${trainerId}`, { method: "POST" });
      await loadClients();
      closeModalRoot();
    } catch (err) {
      showErrorAlert(formatApiErrors(err.fields) || err.message);
    }
  });

  document.getElementById("addClientForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.target;
    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }
    try {
      await apiRequest("/clients", {
        method: "POST",
        body: JSON.stringify(getAddClientPayload()),
      });
      clearAddClientForm();
      await loadClients();
    } catch (err) {
      showErrorAlert(formatApiErrors(err.fields) || err.message);
    }
  });

  document.getElementById("documentModalForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const c = getEditingClient();
    if (!c) {
      setModalDocumentFormError("Клиент не выбран.");
      return;
    }
    const form = e.target;
    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }
    const payload = getModalDocumentFormPayload();
    const docId = document.getElementById("mdoc_id").value;
    try {
      if (docId) {
        await apiRequest(`/clients/${c.client_id}/documents/${docId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        });
      } else {
        await apiRequest(`/clients/${c.client_id}/documents`, {
          method: "POST",
          body: JSON.stringify(payload),
        });
      }
      await refreshDocumentsListModal(c.client_id);
      clearModalDocumentForm();
    } catch (err) {
      showErrorAlert(formatApiErrors(err.fields) || err.message);
    }
  });

  document.getElementById("btn-reset-mdoc")?.addEventListener("click", () => {
    clearModalDocumentForm();
  });

  document.getElementById("btn-delete-mdoc")?.addEventListener("click", async () => {
    const c = getEditingClient();
    const docId = Number(document.getElementById("mdoc_id").value || "");
    if (!c || !docId) return;
    if (!confirm("Удалить этот документ?")) return;
    try {
      await apiRequest(`/clients/${c.client_id}/documents/${docId}`, { method: "DELETE" });
      await refreshDocumentsListModal(c.client_id);
      clearModalDocumentForm();
    } catch (err) {
      showErrorAlert(formatApiErrors(err.fields) || err.message);
    }
  });

  document.getElementById("documents-modal-tbody")?.addEventListener("click", async (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;
    const c = getEditingClient();
    if (!c) return;
    const id = Number(btn.dataset.id);
    if (btn.dataset.action === "edit-doc") {
      try {
        const list = await apiRequest(`/clients/${c.client_id}/documents`);
        const doc = list.find((d) => d.document_id === id);
        if (doc) setModalDocumentFormFromDto(doc);
      } catch (_) {}
      return;
    }
    if (btn.dataset.action === "delete-doc") {
      if (!confirm("Удалить этот документ?")) return;
      try {
        await apiRequest(`/clients/${c.client_id}/documents/${id}`, { method: "DELETE" });
        await refreshDocumentsListModal(c.client_id);
        clearModalDocumentForm();
      } catch (err) {
        showErrorAlert(formatApiErrors(err.fields) || err.message);
      }
    }
  });

  closeModalRoot();
  applyClientDocumentTypeVisibility();
});
