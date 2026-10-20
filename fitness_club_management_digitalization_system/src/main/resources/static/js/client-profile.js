const DOCUMENT_TYPE_LABELS = {
  PASSPORT_RF: "Паспорт РФ",
  STUDENT_ID: "Студенческий билет",
  PENSION_CERT: "Пенсионное удостоверение"
};

const profileState = { data: getClientData() };

function getCurrentClient() {
  const clientId = getCurrentClientId(profileState.data);
  return profileState.data.client.find((c) => c.client_id === clientId) || null;
}

function selectedPersonalData() {
  const c = getCurrentClient();
  return c ? profileState.data.personal_data.find((p) => p.personal_data_id === c.personal_data_id) || null : null;
}

function setMessage(text, isError) {
  const box = document.getElementById("profile-message");
  if (!box) return;
  if (!text) {
    box.style.display = "none";
    box.innerHTML = "";
    box.className = "admin-alert";
    return;
  }
  box.style.display = "block";
  box.innerHTML = "";
  if (isError) {
    box.className = "admin-alert admin-alert--error";
    box.setAttribute("role", "alert");
    const t = document.createElement("span");
    t.className = "admin-alert-title";
    t.textContent = "Ошибка";
    const b = document.createElement("div");
    b.className = "admin-alert-body";
    b.textContent = text;
    box.appendChild(t);
    box.appendChild(b);
  } else {
    box.className = "admin-alert admin-alert--success";
    box.setAttribute("role", "status");
    box.textContent = text;
  }
}

function setDocumentFormError(msg) {
  const box = document.getElementById("documentFormError");
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
  t.textContent = "Ошибка";
  const b = document.createElement("div");
  b.className = "admin-alert-body";
  b.textContent = msg;
  box.appendChild(t);
  box.appendChild(b);
}

function renderForm() {
  const p = selectedPersonalData();
  if (!p) return;
  document.getElementById("personal_data_id").value = p.personal_data_id;
  document.getElementById("last_name").value = p.last_name || "";
  document.getElementById("first_name").value = p.first_name || "";
  document.getElementById("middle_name").value = p.middle_name || "";
  document.getElementById("birth_date").value = p.birth_date || "";
  document.getElementById("phone").value = p.phone || "";
  document.getElementById("email").value = p.email || "";
  document.getElementById("gender").value = p.gender || "Мужской";
  document.getElementById("registration_address").value = p.registration_address || "";
  document.getElementById("actual_address").value = p.actual_address || "";
}

function saveProfile(e) {
  e.preventDefault();
  setMessage("", false);
  const form = document.getElementById("profile-form");
  if (!form.checkValidity()) {
    form.reportValidity();
    return;
  }
  const p = selectedPersonalData();
  if (!p) {
    setMessage("Нет данных авторизованного клиента.", true);
    return;
  }
  p.last_name = document.getElementById("last_name").value.trim();
  p.first_name = document.getElementById("first_name").value.trim();
  p.middle_name = document.getElementById("middle_name").value.trim();
  p.birth_date = document.getElementById("birth_date").value;
  p.phone = document.getElementById("phone").value.trim();
  p.email = document.getElementById("email").value.trim();
  p.gender = document.getElementById("gender").value;
  p.registration_address = document.getElementById("registration_address").value.trim();
  p.actual_address = document.getElementById("actual_address").value.trim();
  saveClientData(profileState.data);
  setMessage("Персональные данные сохранены.", false);
}

function getDocumentsList() {
  const p = selectedPersonalData();
  return p ? getDocumentsFor(profileState.data, p.personal_data_id) : [];
}

function nextDocumentId() {
  const list = getDocumentsList();
  return Math.max(0, ...list.map((d) => Number(d.document_id ?? 0))) + 1;
}

function refreshDocumentsList() {
  const tbody = document.getElementById("documents-tbody");
  if (!tbody) return;
  const list = getDocumentsList();
  tbody.innerHTML = "";
  if (!list.length) {
    tbody.innerHTML = "<tr><td colspan=\"7\" class=\"muted\">Нет документов. Добавьте документ ниже.</td></tr>";
    return;
  }
  list.forEach((d) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc(DOCUMENT_TYPE_LABELS[d.document_type] ?? d.document_type)}</td>
      <td>${esc(d.series)}</td>
      <td>${esc(d.number)}</td>
      <td>${esc(d.issued_by)}</td>
      <td>${esc(d.issue_date ?? "—")}</td>
      <td>${esc(d.expiry_date ?? "—")}</td>
      <td>
        <div class="admin-actions" style="justify-content:flex-start;">
          <button class="admin-btn secondary" type="button" data-action="edit-doc" data-id="${d.document_id}">Изменить</button>
          <button class="admin-btn danger" type="button" data-action="delete-doc" data-id="${d.document_id}">Удалить</button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function getDocumentFormData() {
  const personalDataId = document.getElementById("personal_data_id").value;
  return {
    document_id: document.getElementById("doc_id").value ? Number(document.getElementById("doc_id").value) : null,
    personal_data_id: personalDataId ? Number(personalDataId) : null,
    document_type: document.getElementById("doc_type").value,
    series: document.getElementById("doc_series").value.trim(),
    number: document.getElementById("doc_number").value.trim(),
    issued_by: document.getElementById("doc_issued_by").value.trim(),
    issue_date: document.getElementById("doc_issue_date").value || null,
    expiry_date: document.getElementById("doc_expiry_date").value || null,
    notes: document.getElementById("doc_notes").value.trim() || null
  };
}

function setDocumentFormData(d) {
  document.getElementById("doc_id").value = d?.document_id ?? "";
  document.getElementById("doc_type").value = d?.document_type ?? "PASSPORT_RF";
  document.getElementById("doc_series").value = d?.series ?? "";
  document.getElementById("doc_number").value = d?.number ?? "";
  document.getElementById("doc_issued_by").value = d?.issued_by ?? "";
  document.getElementById("doc_issue_date").value = d?.issue_date ?? "";
  document.getElementById("doc_expiry_date").value = d?.expiry_date ?? "";
  document.getElementById("doc_notes").value = d?.notes ?? "";
}

function clearDocumentForm() {
  setDocumentFormData(null);
  setDocumentFormError("");
}

function doLogout() {
  try {
    localStorage.removeItem(CURRENT_CLIENT_KEY);
  } catch (_) {}
  window.location.href = "/login";
}

function doDeleteAccount() {
  if (!confirm("Удалить аккаунт? Данные будут удалены без возможности восстановления.")) return;
  const c = getCurrentClient();
  if (!c) return;
  const personalDataId = c.personal_data_id;
  const clientId = c.client_id;
  profileState.data.client = profileState.data.client.filter((x) => x.client_id !== clientId);
  profileState.data.personal_data = profileState.data.personal_data.filter((x) => x.personal_data_id !== personalDataId);
  if (profileState.data.documents[personalDataId]) delete profileState.data.documents[personalDataId];
  saveClientData(profileState.data);
  try {
    localStorage.removeItem(CURRENT_CLIENT_KEY);
  } catch (_) {}
  window.location.href = "/login";
}

document.addEventListener("DOMContentLoaded", () => {
  const p = selectedPersonalData();
  if (!p) {
    setMessage("Нет авторизованного клиента. Войдите в личный кабинет.", true);
    document.getElementById("profile-form")?.querySelectorAll("input, select").forEach((el) => { el.disabled = true; });
    document.getElementById("documents-card").style.display = "none";
    document.getElementById("btn-logout").style.display = "none";
    document.getElementById("btn-delete-account").style.display = "none";
    return;
  }
  renderForm();
  refreshDocumentsList();

  document.getElementById("profile-form")?.addEventListener("submit", saveProfile);

  document.getElementById("documentForm")?.addEventListener("submit", (e) => {
    e.preventDefault();
    setDocumentFormError("");
    const personalDataId = document.getElementById("personal_data_id").value;
    if (!personalDataId) return;
    const form = e.target;
    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }
    const data = getDocumentFormData();
    const list = getDocumentsFor(profileState.data, data.personal_data_id);
    if (!data.document_id) {
      const dup = list.find((d) => d.document_type === data.document_type && d.number === data.number);
      if (dup) {
        setDocumentFormError("Документ с таким типом и номером уже добавлен.");
        return;
      }
    }
    if (data.document_id) {
      const idx = list.findIndex((d) => d.document_id === data.document_id);
      if (idx >= 0) list[idx] = { ...list[idx], ...data };
    } else {
      list.push({ ...data, document_id: nextDocumentId() });
    }
    saveClientData(profileState.data);
    refreshDocumentsList();
    clearDocumentForm();
  });

  document.getElementById("btn-reset-document")?.addEventListener("click", () => {
    clearDocumentForm();
  });

  document.getElementById("documents-tbody")?.addEventListener("click", (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;
    const list = getDocumentsList();
    const id = Number(btn.dataset.id);
    if (btn.dataset.action === "edit-doc") {
      const doc = list.find((d) => d.document_id === id);
      if (doc) setDocumentFormData(doc);
      return;
    }
    if (btn.dataset.action === "delete-doc") {
      if (!confirm("Удалить этот документ?")) return;
      const idx = list.findIndex((d) => d.document_id === id);
      if (idx >= 0) list.splice(idx, 1);
      saveClientData(profileState.data);
      refreshDocumentsList();
      clearDocumentForm();
    }
  });

  document.getElementById("btn-logout")?.addEventListener("click", (e) => {
    e.preventDefault();
    doLogout();
  });

  document.getElementById("btn-delete-account")?.addEventListener("click", doDeleteAccount);
});
