const API_BASE = (document.body.dataset.apiBase || "/api/admin").replace(/\/$/, "");

let trainersCache = [];
let editingTrainerId = null;

function esc(s) {
  return String(s ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#039;");
}

function fullName(t) {
  const m = t.middle_name ? ` ${t.middle_name}` : "";
  return `${t.last_name} ${t.first_name}${m}`.trim();
}

function trainerIsActive(t) {
  if (typeof t?.is_active === "boolean") return t.is_active;
  if (typeof t?.active === "boolean") return t.active;
  if (typeof t?.isActive === "boolean") return t.isActive;
  const raw = t?.is_active ?? t?.active ?? t?.isActive ?? "";
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

function getSelectedZoneIds(selectId) {
  const sel = document.getElementById(selectId);
  if (!sel) return [];
  return Array.from(sel.selectedOptions)
    .map((o) => Number(o.value))
    .filter((n) => Number.isFinite(n) && n > 0);
}

function setTrainerZonesSelect(selectId, zoneIds) {
  const sel = document.getElementById(selectId);
  if (!sel) return;
  const want = new Set((zoneIds || []).map((id) => Number(id)));
  for (const opt of sel.options) {
    opt.selected = want.has(Number(opt.value));
  }
}

async function loadTrainers() {
  trainersCache = await apiRequest("/trainers");
  applySearch();
}

const DOCUMENT_TYPE_LABELS = {
  PASSPORT_RF: "Паспорт РФ",
  INN: "ИНН",
  SNILS: "СНИЛС",
  MEDICAL_BOOK_PAPER: "Медкнижка (бумажная)",
  MEDICAL_BOOK_ELECTRONIC: "Медкнижка (электронная)",
  DIPLOMA_HIGHER: "Диплом ВО",
  DIPLOMA_SECONDARY: "Диплом СПО",
  CERTIFICATE: "Сертификат",
  WORK_RECORD_PAPER: "Трудовая книжка",
  STUDENT_ID: "Студенческий билет",
  PENSION_CERT: "Пенсионное удостоверение",
};

function setModalDocumentFormError(msg) {
  showErrorAlert(msg);
}

function toggleTrainerFieldWrap(id, shown) {
  const el = document.getElementById(id);
  if (!el) return;
  el.style.display = shown ? "" : "none";
}

/**
 * Поля по типу документа — в соответствии с DocumentRulesService (серия/даты/кем выдан и т.д.).
 */
function applyTrainerDocumentTypeVisibility() {
  const type = document.getElementById("tdoc_type")?.value || "PASSPORT_RF";

  let showSeries = false;
  let showIssuedBy = false;
  let showIssueDate = false;
  let showExpiryDate = false;

  switch (type) {
    case "PASSPORT_RF":
      showSeries = true;
      showIssuedBy = true;
      showIssueDate = true;
      showExpiryDate = true;
      break;
    case "INN":
    case "SNILS":
      break;
    case "MEDICAL_BOOK_PAPER":
    case "MEDICAL_BOOK_ELECTRONIC":
      showIssuedBy = true;
      showIssueDate = true;
      showExpiryDate = true;
      break;
    case "DIPLOMA_HIGHER":
    case "DIPLOMA_SECONDARY":
      showSeries = true;
      showIssuedBy = true;
      showIssueDate = true;
      break;
    case "CERTIFICATE":
      showIssuedBy = true;
      showIssueDate = true;
      break;
    case "WORK_RECORD_PAPER":
      showSeries = true;
      showIssuedBy = true;
      break;
    case "STUDENT_ID":
      showIssueDate = true;
      showExpiryDate = true;
      break;
    case "PENSION_CERT":
      break;
    default:
      showSeries = true;
      showIssuedBy = true;
      showIssueDate = true;
      showExpiryDate = true;
  }

  toggleTrainerFieldWrap("tdoc_field_series", showSeries);
  toggleTrainerFieldWrap("tdoc_field_issued_by", showIssuedBy);
  toggleTrainerFieldWrap("tdoc_field_issue_date", showIssueDate);
  toggleTrainerFieldWrap("tdoc_field_expiry_date", showExpiryDate);
  toggleTrainerFieldWrap("tdoc_field_notes", true);

  if (!showSeries) document.getElementById("tdoc_series").value = "";
  if (!showIssuedBy) document.getElementById("tdoc_issued_by").value = "";
  if (!showIssueDate) document.getElementById("tdoc_issue_date").value = "";
  if (!showExpiryDate) document.getElementById("tdoc_expiry_date").value = "";
}

function getModalDocumentFormPayload() {
  const docType = document.getElementById("tdoc_type").value;
  const expiryRaw = document.getElementById("tdoc_expiry_date").value || null;
  return {
    document_type: docType,
    series: document.getElementById("tdoc_series").value.trim() || null,
    number: document.getElementById("tdoc_number").value.trim(),
    issued_by: document.getElementById("tdoc_issued_by").value.trim() || null,
    issue_date: document.getElementById("tdoc_issue_date").value || null,
    expiry_date: docType === "CERTIFICATE" ? null : expiryRaw,
    notes: document.getElementById("tdoc_notes").value.trim() || null,
    is_verified: document.getElementById("tdoc_is_verified").checked,
  };
}

function setModalDocumentFormFromDto(d) {
  document.getElementById("tdoc_id").value = d?.document_id ?? "";
  document.getElementById("tdoc_type").value = d?.document_type ?? "PASSPORT_RF";
  document.getElementById("tdoc_series").value = d?.series ?? "";
  document.getElementById("tdoc_number").value = d?.number ?? "";
  document.getElementById("tdoc_issued_by").value = d?.issued_by ?? "";
  document.getElementById("tdoc_issue_date").value = d?.issue_date ?? "";
  document.getElementById("tdoc_expiry_date").value = d?.expiry_date ?? "";
  document.getElementById("tdoc_notes").value = d?.notes ?? "";
  document.getElementById("tdoc_is_verified").checked = !!d?.is_verified;
  document.getElementById("btn-delete-tdoc").style.display = d?.document_id ? "" : "none";
  applyTrainerDocumentTypeVisibility();
}

function clearModalDocumentForm() {
  setModalDocumentFormFromDto(null);
}

async function refreshDocumentsListModal(trainerId) {
  const tbody = document.getElementById("documents-modal-tbody");
  if (!tbody) return;
  tbody.innerHTML = `<tr><td colspan="8" class="muted">Загрузка…</td></tr>`;
  try {
    const list = await apiRequest(`/trainers/${trainerId}/documents`);
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

function getAddTrainerPayload() {
  const r = document.getElementById("add_rating").value;
  return {
    last_name: document.getElementById("add_last_name").value.trim(),
    first_name: document.getElementById("add_first_name").value.trim(),
    middle_name: document.getElementById("add_middle_name").value.trim() || null,
    birth_date: document.getElementById("add_birth_date").value,
    phone: document.getElementById("add_phone").value.trim(),
    email: document.getElementById("add_email").value.trim(),
    gender: document.getElementById("add_gender").value,
    registration_address: document.getElementById("add_registration_address").value.trim(),
    actual_address: document.getElementById("add_actual_address").value.trim() || null,
    password: document.getElementById("add_password").value,
    password_confirm: document.getElementById("add_password_confirm").value,
    specialization: document.getElementById("add_specialization").value.trim(),
    experience_years: Number(document.getElementById("add_experience_years").value),
    rating: r === "" ? null : Number(r),
    biography: document.getElementById("add_biography").value.trim(),
    zone_ids: getSelectedZoneIds("add_trainer_zones"),
  };
}

function clearAddTrainerForm() {
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
  document.getElementById("add_specialization").value = "";
  document.getElementById("add_experience_years").value = "0";
  document.getElementById("add_rating").value = "";
  document.getElementById("add_biography").value = "";
  setTrainerZonesSelect("add_trainer_zones", []);
}

/** Тело PUT /trainers/:id из кэша строки (для переключения активности из таблицы). */
function trainerRowToUpdatePayload(t, overrides = {}) {
  const r = t.rating;
  return {
    last_name: t.last_name ?? "",
    first_name: t.first_name ?? "",
    middle_name: t.middle_name && String(t.middle_name).trim() ? String(t.middle_name).trim() : null,
    birth_date: t.birth_date ?? "",
    phone: String(t.phone ?? "").trim(),
    email: String(t.email ?? "").trim(),
    gender: t.gender ?? "Мужской",
    registration_address: String(t.registration_address ?? "").trim(),
    actual_address: t.actual_address && String(t.actual_address).trim() ? String(t.actual_address).trim() : null,
    specialization: String(t.specialization ?? "").trim(),
    experience_years: Number(t.experience_years ?? 0),
    rating: r == null || r === "" ? null : Number(r),
    biography: String(t.biography ?? "").trim(),
    is_active: trainerIsActive(t),
    zone_ids: Array.isArray(t.zone_ids) ? [...t.zone_ids] : [],
    ...overrides,
  };
}

function getEditTrainerPayload() {
  const r = document.getElementById("edit_rating").value;
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
    registration_address: document.getElementById("edit_registration_address").value.trim(),
    actual_address: document.getElementById("edit_actual_address").value.trim() || null,
    specialization: document.getElementById("edit_specialization").value.trim(),
    experience_years: Number(document.getElementById("edit_experience_years").value),
    rating: r === "" ? null : Number(r),
    biography: document.getElementById("edit_biography").value.trim(),
    zone_ids: getSelectedZoneIds("edit_trainer_zones"),
  };
  if (np) {
    payload.new_password = np;
    payload.new_password_confirm = npc;
  }
  return payload;
}

function setEditTrainerFormData(t) {
  document.getElementById("edit_trainer_id").value = t.trainer_id ?? "";
  document.getElementById("edit_personal_data_id").value = t.personal_data_id ?? "";
  document.getElementById("edit_last_name").value = t.last_name ?? "";
  document.getElementById("edit_first_name").value = t.first_name ?? "";
  document.getElementById("edit_middle_name").value = t.middle_name ?? "";
  document.getElementById("edit_birth_date").value = t.birth_date ?? "";
  document.getElementById("edit_phone").value = t.phone ?? "";
  document.getElementById("edit_email").value = t.email ?? "";
  document.getElementById("edit_gender").value = t.gender ?? "Мужской";
  document.getElementById("edit_registration_address").value = t.registration_address ?? "";
  document.getElementById("edit_actual_address").value = t.actual_address ?? "";
  document.getElementById("edit_new_password").value = "";
  document.getElementById("edit_new_password_confirm").value = "";
  document.getElementById("edit_specialization").value = t.specialization ?? "";
  document.getElementById("edit_experience_years").value = String(t.experience_years ?? 0);
  document.getElementById("edit_rating").value = t.rating != null ? String(t.rating) : "";
  document.getElementById("edit_biography").value = t.biography ?? "";
  setTrainerZonesSelect("edit_trainer_zones", t.zone_ids || []);
}

function setAddFormError(msg) {
  showErrorAlert(msg);
}

/** Локальная дата YYYY-MM-DD без UTC-сдвига */
function toLocalIsoDate(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

/** Границы даты рождения: от 18 до 70 лет (поле «Добавление»). */
function refreshAddTrainerBirthDateBounds() {
  const inp = document.getElementById("add_birth_date");
  if (!inp) return;
  const today = new Date();
  const minD = new Date(today.getFullYear() - 70, today.getMonth(), today.getDate());
  const maxD = new Date(today.getFullYear() - 18, today.getMonth(), today.getDate());
  inp.min = toLocalIsoDate(minD);
  inp.max = toLocalIsoDate(maxD);
}

function validateAddTrainerBirthDateOrAlert() {
  const inp = document.getElementById("add_birth_date");
  if (!inp || !inp.value) return true;
  const v = inp.value;
  if (inp.min && v < inp.min) {
    setAddFormError("Дата рождения слишком ранняя: возраст тренера не может превышать 70 лет.");
    return false;
  }
  if (inp.max && v > inp.max) {
    setAddFormError("Дата рождения слишком поздняя: тренер должен быть не младше 18 лет.");
    return false;
  }
  return true;
}

function getEditingTrainer() {
  if (!editingTrainerId) return null;
  return trainersCache.find((x) => x.trainer_id === editingTrainerId) ?? null;
}

const trainerModalPanels = ["modal-trainer-edit-menu", "modal-trainer-personal", "modal-trainer-documents", "modal-add-from-client"];

function showTrainerModalPanel(panelId) {
  for (const id of trainerModalPanels) {
    const el = document.getElementById(id);
    if (!el) continue;
    el.hidden = id !== panelId;
  }
}

function openTrainerModalRoot() {
  const root = document.getElementById("admin-modal-root");
  if (root) {
    root.hidden = false;
    root.setAttribute("aria-hidden", "false");
  }
}

function closeTrainerModalRoot() {
  const root = document.getElementById("admin-modal-root");
  if (root) {
    root.hidden = true;
    root.setAttribute("aria-hidden", "true");
  }
  editingTrainerId = null;
  trainerModalPanels.forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.hidden = true;
  });
}

function openTrainerEditMenu(trainerId) {
  const t = trainersCache.find((x) => x.trainer_id === trainerId);
  if (!t) return;
  editingTrainerId = trainerId;
  document.getElementById("modal-trainer-name").textContent = fullName(t);
  openTrainerModalRoot();
  showTrainerModalPanel("modal-trainer-edit-menu");
}

async function openAddFromClientModal() {
  const select = document.getElementById("select-client-for-trainer");
  if (!select) return;
  select.innerHTML = `<option value="">Загрузка...</option>`;
  openTrainerModalRoot();
  showTrainerModalPanel("modal-add-from-client");
  try {
    const clients = await apiRequest("/trainers/clients-available");
    select.innerHTML = `<option value="">Выберите клиента</option>`;
    for (const c of clients || []) {
      const option = document.createElement("option");
      option.value = String(c.client_id);
      option.textContent = c.full_name;
      select.appendChild(option);
    }
  } catch (_) {
    select.innerHTML = `<option value="">Не удалось загрузить список клиентов</option>`;
  }
}

function renderTrainers(list) {
  const tbody = document.getElementById("trainers-tbody");
  tbody.innerHTML = "";

  if (!list.length) {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td colspan="7" class="muted">Ничего не найдено</td>`;
    tbody.appendChild(tr);
    return;
  }

  for (const t of list) {
    const active = trainerIsActive(t);
    const toggleLabel = active ? "Деактивировать" : "Активировать";
    const tr = document.createElement("tr");
    const zonesCell =
      Array.isArray(t.zone_names) && t.zone_names.length > 0 ? esc(t.zone_names.join(", ")) : "—";
    tr.innerHTML = `
      <td>${esc(fullName(t))}</td>
      <td>${esc(t.specialization)}</td>
      <td class="admin-cell-zones">${zonesCell}</td>
      <td>${esc(t.experience_years)} лет</td>
      <td>${esc(t.rating ?? "—")}</td>
      <td>${esc(active ? "Да" : "Нет")}</td>
      <td>
        <div class="admin-actions admin-actions--table-row">
          <button class="admin-btn secondary admin-btn--table-edit" type="button" data-action="edit" data-id="${t.trainer_id}">Редактировать</button>
          <button class="admin-btn secondary admin-btn--toggle-active" type="button" data-action="toggle-active" data-id="${t.trainer_id}">${esc(toggleLabel)}</button>
          <button class="admin-btn danger admin-btn--table-delete" type="button" data-action="delete" data-id="${t.trainer_id}">Удалить</button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  }
}

function applySearch() {
  const qName = document.getElementById("q-name").value.trim().toLowerCase();
  const qMin = Number(document.getElementById("q-minRating").value || "0");
  const qActive = document.getElementById("q-is_active").value;

  const filtered = trainersCache.filter((t) => {
    const okName = !qName || fullName(t).toLowerCase().includes(qName) || (t.specialization || "").toLowerCase().includes(qName);
    const okRating = Number(t.rating ?? 0) >= qMin;
    const okActive = !qActive || String(trainerIsActive(t)) === qActive;
    return okName && okRating && okActive;
  });

  renderTrainers(filtered);
}

document.addEventListener("DOMContentLoaded", async () => {
  refreshAddTrainerBirthDateBounds();
  document.getElementById("add_birth_date")?.addEventListener("change", () => {
    if (!validateAddTrainerBirthDateOrAlert()) {
      document.getElementById("add_birth_date").value = "";
    }
  });

  try {
    await loadTrainers();
  } catch (e) {
    showErrorAlert(formatApiErrors(e.fields) || e.message || "Не удалось загрузить список тренеров.");
    document.getElementById("trainers-tbody").innerHTML =
      `<tr><td colspan="7" class="error-message">Ошибка загрузки данных. Проверьте БД.</td></tr>`;
  }

  document.getElementById("btn-search")?.addEventListener("click", applySearch);
  document.getElementById("btn-clear-search")?.addEventListener("click", () => {
    document.getElementById("q-name").value = "";
    document.getElementById("q-minRating").value = "0";
    document.getElementById("q-is_active").value = "";
    renderTrainers(trainersCache);
  });

  document.querySelectorAll("[data-modal-dismiss=\"root\"]").forEach((el) => {
    el.addEventListener("click", () => closeTrainerModalRoot());
  });

  document.getElementById("trainers-tbody")?.addEventListener("click", async (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;
    const action = btn.dataset.action;
    const id = Number(btn.dataset.id);
    const item = trainersCache.find((x) => x.trainer_id === id);
    if (!item) return;

    if (action === "edit") {
      openTrainerEditMenu(id);
      return;
    }
    if (action === "toggle-active") {
      const nextActive = !trainerIsActive(item);
      try {
        await apiRequest(`/trainers/${id}`, {
          method: "PUT",
          body: JSON.stringify(trainerRowToUpdatePayload(item, { is_active: nextActive })),
        });
        await loadTrainers();
        applySearch();
        if (editingTrainerId === id) {
          const updated = trainersCache.find((x) => x.trainer_id === id);
          if (updated) document.getElementById("modal-trainer-name").textContent = fullName(updated);
        }
      } catch (err) {
        showErrorAlert(formatApiErrors(err.fields) || err.message);
      }
      return;
    }
    if (action === "delete") {
      if (!confirm(`Удалить тренера ${fullName(item)}?`)) return;
      try {
        await apiRequest(`/trainers/${id}`, { method: "DELETE" });
        await loadTrainers();
        if (editingTrainerId === id) closeTrainerModalRoot();
      } catch (err) {
        showErrorAlert(formatApiErrors(err.fields) || err.message);
      }
    }
  });

  document.getElementById("btn-reset-add-trainer")?.addEventListener("click", () => {
    clearAddTrainerForm();
  });

  document.getElementById("addTrainerForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.target;
    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }
    if (!validateAddTrainerBirthDateOrAlert()) return;
    try {
      await apiRequest("/trainers", { method: "POST", body: JSON.stringify(getAddTrainerPayload()) });
      clearAddTrainerForm();
      refreshAddTrainerBirthDateBounds();
      await loadTrainers();
    } catch (err) {
      showErrorAlert(formatApiErrors(err.fields) || err.message);
    }
  });

  document.getElementById("btn-open-trainer-personal")?.addEventListener("click", () => {
    const t = getEditingTrainer();
    if (!t) return;
    setEditTrainerFormData(t);
    showTrainerModalPanel("modal-trainer-personal");
  });

  document.getElementById("btn-back-trainer-personal-to-menu")?.addEventListener("click", () => {
    showTrainerModalPanel("modal-trainer-edit-menu");
  });

  document.getElementById("btn-open-trainer-docs")?.addEventListener("click", async () => {
    const t = getEditingTrainer();
    if (!t) return;
    document.getElementById("tdoc_personal_data_id").value = String(t.personal_data_id);
    clearModalDocumentForm();
    await refreshDocumentsListModal(t.trainer_id);
    showTrainerModalPanel("modal-trainer-documents");
  });

  document.getElementById("btn-back-trainer-docs-to-menu")?.addEventListener("click", () => {
    showTrainerModalPanel("modal-trainer-edit-menu");
  });

  document.getElementById("editTrainerForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.target;
    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }
    const id = Number(document.getElementById("edit_trainer_id").value);
    try {
      await apiRequest(`/trainers/${id}`, { method: "PUT", body: JSON.stringify(getEditTrainerPayload()) });
      await loadTrainers();
      const updated = trainersCache.find((x) => x.trainer_id === id);
      if (updated) document.getElementById("modal-trainer-name").textContent = fullName(updated);
      applySearch();
    } catch (err) {
      showErrorAlert(formatApiErrors(err.fields) || err.message);
    }
  });

  document.getElementById("trainerDocumentModalForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const t = getEditingTrainer();
    if (!t) {
      setModalDocumentFormError("Тренер не выбран.");
      return;
    }
    const form = e.target;
    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }
    const payload = getModalDocumentFormPayload();
    const docId = document.getElementById("tdoc_id").value;
    try {
      if (docId) {
        await apiRequest(`/trainers/${t.trainer_id}/documents/${docId}`, { method: "PUT", body: JSON.stringify(payload) });
      } else {
        await apiRequest(`/trainers/${t.trainer_id}/documents`, { method: "POST", body: JSON.stringify(payload) });
      }
      await refreshDocumentsListModal(t.trainer_id);
      clearModalDocumentForm();
    } catch (err) {
      showErrorAlert(formatApiErrors(err.fields) || err.message);
    }
  });

  document.getElementById("btn-reset-tdoc")?.addEventListener("click", () => clearModalDocumentForm());

  document.getElementById("btn-delete-tdoc")?.addEventListener("click", async () => {
    const t = getEditingTrainer();
    const docId = Number(document.getElementById("tdoc_id").value || "");
    if (!t || !docId) return;
    if (!confirm("Удалить этот документ?")) return;
    try {
      await apiRequest(`/trainers/${t.trainer_id}/documents/${docId}`, { method: "DELETE" });
      await refreshDocumentsListModal(t.trainer_id);
      clearModalDocumentForm();
    } catch (err) {
      showErrorAlert(formatApiErrors(err.fields) || err.message);
    }
  });

  document.getElementById("documents-modal-tbody")?.addEventListener("click", async (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;
    const t = getEditingTrainer();
    if (!t) return;
    const id = Number(btn.dataset.id);
    if (btn.dataset.action === "edit-doc") {
      try {
        const list = await apiRequest(`/trainers/${t.trainer_id}/documents`);
        const doc = list.find((d) => d.document_id === id);
        if (doc) setModalDocumentFormFromDto(doc);
      } catch (_) {}
      return;
    }
    if (btn.dataset.action === "delete-doc") {
      if (!confirm("Удалить этот документ?")) return;
      try {
        await apiRequest(`/trainers/${t.trainer_id}/documents/${id}`, { method: "DELETE" });
        await refreshDocumentsListModal(t.trainer_id);
        clearModalDocumentForm();
      } catch (err) {
        showErrorAlert(formatApiErrors(err.fields) || err.message);
      }
    }
  });

  document.getElementById("btn-open-add-from-client")?.addEventListener("click", () => {
    openAddFromClientModal();
  });

  document.getElementById("btn-create-trainer-from-client")?.addEventListener("click", async () => {
    const select = document.getElementById("select-client-for-trainer");
    const clientId = Number(select?.value || "");
    if (!clientId) {
      showErrorAlert("Выберите клиента.");
      return;
    }
    try {
      await apiRequest(`/trainers/from-client/${clientId}`, { method: "POST" });
      await loadTrainers();
      closeTrainerModalRoot();
    } catch (err) {
      showErrorAlert(formatApiErrors(err.fields) || err.message);
    }
  });

  document.getElementById("tdoc_type")?.addEventListener("change", () => applyTrainerDocumentTypeVisibility());

  applyTrainerDocumentTypeVisibility();
  closeTrainerModalRoot();
});
