const API_BASE = (document.body.dataset.apiBase || "/api/client").replace(/\/$/, "");
const DOC_LABELS = { PASSPORT_RF: "Паспорт РФ", STUDENT_ID: "Студенческий билет", PENSION_CERT: "Пенсионное удостоверение" };
let profile = null;

function esc(v) { return String(v ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;").replaceAll("'", "&#039;"); }

function setMessage(text, isError) {
  if (text) alert(text);
}

function openDocumentModal() {
  const root = document.getElementById("client-doc-modal-root");
  if (!root) return;
  root.hidden = false;
  root.setAttribute("aria-hidden", "false");
}

function closeDocumentModal() {
  const root = document.getElementById("client-doc-modal-root");
  if (!root) return;
  root.hidden = true;
  root.setAttribute("aria-hidden", "true");
}

function toggleFieldWrap(id, shown) {
  const el = document.getElementById(id);
  if (!el) return;
  el.style.display = shown ? "" : "none";
}

function applyDocumentTypeVisibility() {
  const type = document.getElementById("doc_type")?.value || "PASSPORT_RF";
  const isStudent = type === "STUDENT_ID";
  const isPension = type === "PENSION_CERT";
  const showSeries = !isStudent && !isPension;
  const showIssuedBy = !isPension;
  const showIssueDate = !isPension;
  const showExpiryDate = !isPension;

  toggleFieldWrap("doc_field_series", showSeries);
  toggleFieldWrap("doc_field_issued_by", showIssuedBy);
  toggleFieldWrap("doc_field_issue_date", showIssueDate);
  toggleFieldWrap("doc_field_expiry_date", showExpiryDate);
  toggleFieldWrap("doc_field_notes", true);

  if (!showSeries) document.getElementById("doc_series").value = "";
  if (!showIssuedBy) document.getElementById("doc_issued_by").value = "";
  if (!showIssueDate) document.getElementById("doc_issue_date").value = "";
  if (!showExpiryDate) document.getElementById("doc_expiry_date").value = "";
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
  document.getElementById("btn-delete-document").style.display = d?.document_id ? "" : "none";
  applyDocumentTypeVisibility();
}

function clearDocumentForm() {
  setDocumentFormData(null);
}

function render() {
  if (!profile) return;
  ["personal_data_id","last_name","first_name","middle_name","birth_date","phone","email","gender","registration_address","actual_address"]
    .forEach((k) => { const el = document.getElementById(k); if (el) el.value = profile[k] ?? ""; });
  const tb = document.getElementById("documents-tbody");
  const docs = Array.isArray(profile.documents) ? profile.documents : [];
  tb.innerHTML = docs.length ? docs.map((d) => `<tr>
    <td>${esc(DOC_LABELS[d.document_type] || d.document_type)}</td><td>${esc(d.series || "")}</td><td>${esc(d.number || "")}</td><td>${esc(d.issued_by || "")}</td>
    <td>${esc(d.issue_date || "—")}</td><td>${esc(d.expiry_date || "—")}</td><td>${d.is_verified ? "Да" : "Нет"}</td>
    <td>
      <div class="admin-actions" style="justify-content:flex-start;">
        <button class="admin-btn secondary" type="button" data-edit-doc="${d.document_id}">Изменить</button>
        <button class="admin-btn danger" type="button" data-del-doc="${d.document_id}">Удалить</button>
      </div>
    </td></tr>`).join("") : '<tr><td colspan="8" class="muted">Нет документов.</td></tr>';
}

function load() { fetch(`${API_BASE}/profile`).then((r) => r.ok ? r.json() : null).then((p) => { profile = p; render(); }); }

document.addEventListener("DOMContentLoaded", () => {
  load();
  document.getElementById("profile-form")?.addEventListener("submit", (e) => {
    e.preventDefault();
    const body = {};
    ["last_name","first_name","middle_name","birth_date","phone","gender","registration_address","actual_address"].forEach((k) => body[k] = document.getElementById(k)?.value ?? null);
    fetch(`${API_BASE}/profile`, { method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) })
      .then((r) => { if (!r.ok) throw new Error("Не удалось сохранить данные"); setMessage("Персональные данные сохранены.", false); load(); })
      .catch((e) => setMessage(e.message, true));
  });
  document.getElementById("documentForm")?.addEventListener("submit", (e) => {
    e.preventDefault();
    const body = {
      document_type: document.getElementById("doc_type").value,
      series: document.getElementById("doc_series").value,
      number: document.getElementById("doc_number").value,
      issued_by: document.getElementById("doc_issued_by").value,
      issue_date: document.getElementById("doc_issue_date").value || null,
      expiry_date: document.getElementById("doc_expiry_date").value || null,
      notes: document.getElementById("doc_notes").value.trim() || null
    };
    const docId = document.getElementById("doc_id").value;
    const path = docId ? `${API_BASE}/profile/documents/${docId}` : `${API_BASE}/profile/documents`;
    const method = docId ? "PUT" : "POST";
    fetch(path, { method, headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) })
      .then((r) => {
        if (!r.ok) throw new Error("Не удалось сохранить документ.");
        clearDocumentForm();
        closeDocumentModal();
        load();
      })
      .catch((err) => alert(err.message));
  });
  document.getElementById("documents-tbody")?.addEventListener("click", (e) => {
    const editId = e.target.closest("button[data-edit-doc]")?.dataset.editDoc;
    if (editId) {
      const doc = (Array.isArray(profile?.documents) ? profile.documents : []).find((d) => String(d.document_id) === String(editId));
      if (doc) {
        setDocumentFormData(doc);
        openDocumentModal();
      }
      return;
    }
    const id = e.target.closest("button[data-del-doc]")?.dataset.delDoc;
    if (!id) return;
    if (!confirm("Удалить этот документ?")) return;
    fetch(`${API_BASE}/profile/documents/${id}`, { method: "DELETE" })
      .then((r) => {
        if (!r.ok) throw new Error("Не удалось удалить документ.");
        load();
      })
      .catch((err) => alert(err.message));
  });

  document.getElementById("btn-open-document-modal")?.addEventListener("click", () => {
    clearDocumentForm();
    openDocumentModal();
  });

  document.getElementById("doc_type")?.addEventListener("change", () => applyDocumentTypeVisibility());

  document.getElementById("btn-delete-document")?.addEventListener("click", () => {
    const id = document.getElementById("doc_id").value;
    if (!id) return;
    if (!confirm("Удалить этот документ?")) return;
    fetch(`${API_BASE}/profile/documents/${id}`, { method: "DELETE" })
      .then((r) => {
        if (!r.ok) throw new Error("Не удалось удалить документ.");
        clearDocumentForm();
        closeDocumentModal();
        load();
      })
      .catch((err) => alert(err.message));
  });

  document.querySelectorAll("[data-client-doc-dismiss]").forEach((el) => {
    el.addEventListener("click", () => closeDocumentModal());
  });
  applyDocumentTypeVisibility();
});
