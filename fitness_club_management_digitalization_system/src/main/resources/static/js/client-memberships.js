const API_BASE = (document.body.dataset.apiBase || "/api/client").replace(/\/$/, "");

function esc(v) {
  return String(v ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;").replaceAll("'", "&#039;");
}

function renderCurrentMembership(payload) {
  const purchase = payload?.membership || null;
  const rules = Array.isArray(payload?.rules) ? payload.rules : [];
  const infoTbody = document.getElementById("membership-info-tbody");
  const svcTbody = document.getElementById("membership-services-tbody");

  if (!infoTbody || !svcTbody) return;
  const detailsBtn = document.getElementById("btn-membership-details");

  if (!purchase) {
    infoTbody.innerHTML =
      '<tr><td colspan="6" class="muted">Нет активного абонемента (дата не в периоде действия или статус не «активен»).</td></tr>';
    svcTbody.innerHTML = "";
    if (detailsBtn) detailsBtn.disabled = true;
    return;
  }
  if (detailsBtn) detailsBtn.disabled = false;

  infoTbody.innerHTML = `
    <tr>
      <td>${esc(purchase.membership_name)}</td>
      <td>${esc(String(purchase.remaining_freeze_days ?? 0))}</td>
      <td>${esc(String(purchase.remaining_guest_visits ?? 0))}</td>
      <td>${esc(purchase.start_date)}</td>
      <td>${esc(purchase.end_date)}</td>
      <td>${esc(purchase.status)}</td>
    </tr>
  `;

  svcTbody.innerHTML = "";
  if (!rules.length) {
    svcTbody.innerHTML = '<tr><td colspan="7" class="muted">Нет привязанных услуг по активному абонементу.</td></tr>';
    return;
  }

  rules.forEach((ms) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc(ms.service_name)}</td>
      <td>${esc(ms.included_visits)}</td>
      <td>${esc(ms.remaining)}</td>
      <td>${esc(ms.max_per_week != null ? String(ms.max_per_week) : "—")}</td>
      <td>${esc(ms.days_of_week || "—")}</td>
      <td>${esc(ms.time_start || "—")}</td>
      <td>${esc(ms.time_end || "—")}</td>
    `;
    svcTbody.appendChild(tr);
  });
}

function renderActiveOneTimeServices(payload) {
  const tbody = document.getElementById("service-purchases-tbody");
  if (!tbody) return;
  const active = Array.isArray(payload?.service_purchases) ? payload.service_purchases : [];
  tbody.innerHTML = "";
  if (!active.length) {
    tbody.innerHTML = '<tr><td colspan="7" class="muted">Активных разовых услуг нет.</td></tr>';
    return;
  }

  active
    .slice()
    .sort((a, b) => Number(b.purchase_id ?? 0) - Number(a.purchase_id ?? 0))
    .forEach((p) => {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>${esc(p.service_name || "—")}</td>
        <td>${esc(String(p.included_visits ?? "—"))}</td>
        <td>${esc(String(p.remaining ?? "—"))}</td>
        <td>${esc(p.max_per_week ?? "—")}</td>
        <td>${esc(p.days_of_week ?? "—")}</td>
        <td>${esc(p.time_start ?? "—")}</td>
        <td>${esc(p.time_end ?? "—")}</td>
      `;
      tbody.appendChild(tr);
    });
}

document.addEventListener("DOMContentLoaded", () => {
  fetch(`${API_BASE}/memberships/overview`)
    .then((r) => r.ok ? r.json() : null)
    .then((payload) => {
      renderCurrentMembership(payload || {});
      renderActiveOneTimeServices(payload || {});
    });

  function openRulesModal() {
    const root = document.getElementById("membership-rules-modal-root");
    if (!root) return;
    root.hidden = false;
    root.setAttribute("aria-hidden", "false");
  }

  function closeRulesModal() {
    const root = document.getElementById("membership-rules-modal-root");
    if (!root) return;
    root.hidden = true;
    root.setAttribute("aria-hidden", "true");
  }

  document.getElementById("btn-membership-details")?.addEventListener("click", () => openRulesModal());
  document.querySelectorAll("[data-membership-rules-dismiss]").forEach((el) => {
    el.addEventListener("click", () => closeRulesModal());
  });
});
