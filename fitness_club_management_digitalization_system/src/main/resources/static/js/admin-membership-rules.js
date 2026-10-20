const API_BASE = (document.body.dataset.apiBase || "/api/admin").replace(/\/$/, "");

let rulesCache = [];
/** Полные планы абонементов (в т.ч. service_ids), GET /memberships */
let membershipsPlans = [];
let servicesBrief = [];

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

const RULE_FIELD_LABELS = {
  membership_id: "Абонемент",
  service_id: "Услуга",
  included_visits: "Включённые посещения",
  max_per_week: "Максимум в неделю",
  days_of_week: "Дни недели",
  time_start: "Время начала",
  time_end: "Время окончания",
  membership_service_id: "Правило",
};

function formatApiErrors(fields) {
  if (!fields || typeof fields !== "object") return "";
  return Object.entries(fields)
    .map(([k, v]) => `${RULE_FIELD_LABELS[k] || k}: ${v}`)
    .join("\n");
}

function parseTimeMinutes(s) {
  const m = /^(\d{1,2}):(\d{2})$/.exec(String(s ?? "").trim());
  if (!m) return null;
  const h = Number(m[1]);
  const min = Number(m[2]);
  if (Number.isNaN(h) || Number.isNaN(min) || h < 0 || h > 23 || min < 0 || min > 59) return null;
  return h * 60 + min;
}

/** Согласовано с AdminMembershipRuleService.validateVisitTimeWindow */
function validateVisitTimes(timeStart, timeEnd) {
  if (!timeStart && !timeEnd) return "";
  if (!timeStart || !timeEnd) {
    return "Укажите время начала и окончания вместе или оставьте оба пустыми.";
  }
  const ts = parseTimeMinutes(timeStart);
  const te = parseTimeMinutes(timeEnd);
  if (ts === null || te === null) return "Некорректный формат времени (ожидается ЧЧ:мм).";
  const START_MIN = 6 * 60;
  const START_MAX = 23 * 60;
  const END_MIN = 7 * 60;
  const END_MAX = 23 * 60 + 59;
  const START_23 = 23 * 60;
  if (ts < START_MIN || ts > START_MAX) return "Время начала: с 06:00 до 23:00.";
  if (te === 0) return "";
  if (ts === START_23) {
    return "При начале в 23:00 укажите окончание 00:00 (полночь).";
  }
  if (te < END_MIN || te > END_MAX) {
    return "Время окончания: с 07:00 до 23:59 или 00:00 (полночь).";
  }
  if (te <= ts) return "Время окончания должно быть позже начала (в тот же календарный день).";
  return "";
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
  fillAdminAlertError(document.getElementById("ruleFormError"), "Не удалось сохранить правило", msg);
}

function findRule(mid, sid) {
  return rulesCache.find(
    (x) => (x.membership_id ?? x.membershipId) === mid && (x.service_id ?? x.serviceId) === sid,
  );
}

function getMembershipPlan(mid) {
  return membershipsPlans.find((m) => (m.membership_id ?? m.membershipId) === mid);
}

/** Услуги, включённые в абонемент в каталоге (поле service_ids плана). */
function servicesLinkedToMembership(mid) {
  const m = getMembershipPlan(mid);
  const ids = new Set((m?.service_ids ?? m?.serviceIds ?? []).map(Number));
  return servicesBrief.filter((s) => ids.has(Number(s.service_id ?? s.serviceId)));
}

/**
 * Заполняет только поле «Услуга» в форме добавления/редактирования.
 * @param {number|undefined|null} ensureServiceId — при редактировании оставить в списке услугу правила,
 *   даже если её убрали из состава абонемента (иначе нельзя сохранить без смены услуги).
 */
function fillFormServiceOptions(ensureServiceId) {
  const sSel = document.getElementById("service_id");
  const mEl = document.getElementById("membership_id");
  const mid = Number(mEl?.value);
  let list = Number.isFinite(mid) && mid > 0 ? servicesLinkedToMembership(mid) : [];
  const extraId = ensureServiceId != null ? Number(ensureServiceId) : NaN;
  if (Number.isFinite(extraId) && extraId > 0 && !list.some((s) => Number(s.service_id ?? s.serviceId) === extraId)) {
    const extra = servicesBrief.find((s) => Number(s.service_id ?? s.serviceId) === extraId);
    if (extra) list = [...list, extra];
  }
  if (!list.length) {
    sSel.innerHTML = `<option value="">— Нет услуг в абонементе —</option>`;
    sSel.disabled = true;
    return;
  }
  sSel.disabled = false;
  sSel.innerHTML = list
    .map((s) => {
      const id = s.service_id ?? s.serviceId;
      const name = s.service_name ?? s.serviceName;
      return `<option value="${esc(id)}">${esc(name)}</option>`;
    })
    .join("");
}

function setRuleForm(rule) {
  document.getElementById("membership_service_id").value = rule
    ? String(rule.membership_service_id ?? rule.membershipServiceId ?? "")
    : "";

  if (rule) {
    document.getElementById("membership_id").value = String(rule.membership_id ?? rule.membershipId);
    fillFormServiceOptions(rule.service_id ?? rule.serviceId);
    document.getElementById("service_id").value = String(rule.service_id ?? rule.serviceId);
  }

  document.getElementById("included_visits").value =
    rule?.included_visits != null || rule?.includedVisits != null
      ? String(rule.included_visits ?? rule.includedVisits)
      : "";
  document.getElementById("max_per_week").value =
    rule?.max_per_week != null || rule?.maxPerWeek != null
      ? String(rule.max_per_week ?? rule.maxPerWeek)
      : "";

  const ts = rule?.time_start ?? rule?.timeStart;
  const te = rule?.time_end ?? rule?.timeEnd;
  document.getElementById("time_start").value = ts != null ? String(ts).slice(0, 5) : "";
  document.getElementById("time_end").value = te != null ? String(te).slice(0, 5) : "";

  const selectedDays = rule?.days_of_week ?? rule?.daysOfWeek ?? [];
  document.querySelectorAll("#days-checkboxes input[type='checkbox']").forEach((cb) => {
    cb.checked = Array.isArray(selectedDays) && selectedDays.includes(cb.value);
  });
}

function getRulePayload() {
  const selectedDays = [...document.querySelectorAll("#days-checkboxes input[type='checkbox']:checked")].map((x) => x.value);
  return {
    membership_id: Number(document.getElementById("membership_id").value),
    service_id: Number(document.getElementById("service_id").value),
    included_visits:
      document.getElementById("included_visits").value === "" ? null : Number(document.getElementById("included_visits").value),
    max_per_week:
      document.getElementById("max_per_week").value === "" ? null : Number(document.getElementById("max_per_week").value),
    days_of_week: selectedDays.length ? selectedDays : null,
    time_start: document.getElementById("time_start").value === "" ? null : document.getElementById("time_start").value,
    time_end: document.getElementById("time_end").value === "" ? null : document.getElementById("time_end").value,
  };
}

function clearRuleForm() {
  document.getElementById("membership_service_id").value = "";
  document.getElementById("included_visits").value = "";
  document.getElementById("max_per_week").value = "";
  document.getElementById("time_start").value = "";
  document.getElementById("time_end").value = "";
  document.querySelectorAll("#days-checkboxes input[type='checkbox']").forEach((cb) => (cb.checked = false));
}

function renderRules(list) {
  const tbody = document.getElementById("rules-tbody");
  tbody.innerHTML = "";

  if (!list.length) {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td colspan="8" class="muted">Ничего не найдено</td>`;
    tbody.appendChild(tr);
    return;
  }

  for (const r of list) {
    const days = r.days_of_week ?? r.daysOfWeek;
    const daysStr = Array.isArray(days) && days.length ? days.join(", ") : "—";
    const msid = r.membership_service_id ?? r.membershipServiceId;
    const mname = r.membership_name ?? r.membershipName ?? "";
    const sname = r.service_name ?? r.serviceName ?? "";
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc(mname || "—")}</td>
      <td>${esc(sname || "—")}</td>
      <td>${esc((r.included_visits ?? r.includedVisits) == null ? "Безлимит" : r.included_visits ?? r.includedVisits)}</td>
      <td>${esc((r.max_per_week ?? r.maxPerWeek) == null ? "—" : r.max_per_week ?? r.maxPerWeek)}</td>
      <td>${esc(daysStr)}</td>
      <td>${esc((r.time_start ?? r.timeStart) ?? "—")}</td>
      <td>${esc((r.time_end ?? r.timeEnd) ?? "—")}</td>
      <td>
        <div class="admin-actions" style="justify-content:flex-start;">
          <button class="admin-btn secondary" type="button" data-action="edit" data-id="${esc(msid)}">Редактировать</button>
          <button class="admin-btn danger" type="button" data-action="delete" data-id="${esc(msid)}">Удалить</button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  }
}

function applySearch() {
  const mid = document.getElementById("q-membership").value;
  const sid = document.getElementById("q-service").value;
  const filtered = rulesCache.filter((r) => {
    const okMid = !mid || String(r.membership_id ?? r.membershipId) === mid;
    const okSid = !sid || String(r.service_id ?? r.serviceId) === sid;
    return okMid && okSid;
  });
  renderRules(filtered);
}

function fillSelects() {
  const mSel = document.getElementById("membership_id");
  const qM = document.getElementById("q-membership");
  const qS = document.getElementById("q-service");

  mSel.innerHTML = membershipsPlans
    .map((m) => {
      const id = m.membership_id ?? m.membershipId;
      const name = m.membership_name ?? m.membershipName;
      return `<option value="${esc(id)}">${esc(name)}</option>`;
    })
    .join("");
  fillFormServiceOptions();

  qM.innerHTML =
    `<option value="">Все</option>` +
    membershipsPlans
      .map((m) => {
        const id = m.membership_id ?? m.membershipId;
        const name = m.membership_name ?? m.membershipName;
        return `<option value="${esc(id)}">${esc(name)}</option>`;
      })
      .join("");
  qS.innerHTML =
    `<option value="">Все</option>` +
    servicesBrief
      .map((s) => {
        const id = s.service_id ?? s.serviceId;
        const name = s.service_name ?? s.serviceName;
        return `<option value="${esc(id)}">${esc(name)}</option>`;
      })
      .join("");
}

async function loadAll() {
  fillAdminAlertError(document.getElementById("rule-load-error"), "", "");
  const [rules, memPlans, svcList] = await Promise.all([
    apiRequest("/membership-rules"),
    apiRequest("/memberships"),
    apiRequest("/services"),
  ]);
  rulesCache = Array.isArray(rules) ? rules : [];
  membershipsPlans = Array.isArray(memPlans) ? memPlans : [];
  servicesBrief = Array.isArray(svcList) ? svcList : [];
  fillSelects();
  applySearch();
}

document.addEventListener("DOMContentLoaded", () => {
  const membershipSelect = document.getElementById("membership_id");
  const serviceSelect = document.getElementById("service_id");

  const onPairChange = () => {
    const mid = Number(membershipSelect.value);
    const sid = Number(serviceSelect.value);
    const rule = findRule(mid, sid);
    if (rule) {
      setRuleForm(rule);
    } else {
      clearRuleForm();
      document.getElementById("membership_service_id").value = "";
    }
  };

  loadAll()
    .then(() => {
      if (membershipSelect?.value && serviceSelect?.value) onPairChange();
    })
    .catch((e) => {
      fillAdminAlertError(
        document.getElementById("rule-load-error"),
        "Ошибка загрузки",
        formatApiErrors(e.fields) || e.message,
      );
      document.getElementById("rules-tbody").innerHTML =
        `<tr><td colspan="8" class="muted">Не удалось загрузить данные</td></tr>`;
    });

  document.getElementById("btn-search")?.addEventListener("click", applySearch);
  document.getElementById("btn-clear-search")?.addEventListener("click", () => {
    document.getElementById("q-membership").value = "";
    document.getElementById("q-service").value = "";
    applySearch();
  });

  membershipSelect?.addEventListener("change", () => {
    fillFormServiceOptions();
    onPairChange();
  });
  serviceSelect?.addEventListener("change", onPairChange);

  document.getElementById("rules-tbody")?.addEventListener("click", async (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;
    const id = Number(btn.dataset.id);
    const rule = rulesCache.find((x) => (x.membership_service_id ?? x.membershipServiceId) === id);
    if (!rule) return;

    if (btn.dataset.action === "edit") {
      setRuleForm(rule);
      setFormError("");
      document.getElementById("ruleForm")?.scrollIntoView({ behavior: "smooth", block: "start" });
      return;
    }

    if (btn.dataset.action === "delete") {
      if (!confirm("Удалить связь абонемента с услугой (правило посещения)?")) return;
      try {
        await apiRequest(`/membership-rules/${id}`, { method: "DELETE" });
        await loadAll();
        onPairChange();
      } catch (err) {
        fillAdminAlertError(
          document.getElementById("ruleFormError"),
          "Не удалось удалить правило",
          formatApiErrors(err.fields) || err.message,
        );
      }
    }
  });

  document.getElementById("btn-reset-rule")?.addEventListener("click", () => {
    clearRuleForm();
    document.getElementById("membership_service_id").value = "";
    setFormError("");
    onPairChange();
  });

  document.getElementById("ruleForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    setFormError("");
    if (!e.target.checkValidity()) {
      e.target.reportValidity();
      return;
    }
    const payload = getRulePayload();
    const timeErr = validateVisitTimes(payload.time_start ?? "", payload.time_end ?? "");
    if (timeErr) {
      setFormError(timeErr);
      return;
    }
    const hiddenId = document.getElementById("membership_service_id").value;
    try {
      if (hiddenId) {
        await apiRequest(`/membership-rules/${Number(hiddenId)}`, { method: "PUT", body: JSON.stringify(payload) });
      } else {
        await apiRequest("/membership-rules", { method: "POST", body: JSON.stringify(payload) });
      }
      clearRuleForm();
      document.getElementById("membership_service_id").value = "";
      await loadAll();
      onPairChange();
    } catch (err) {
      setFormError(formatApiErrors(err.fields) || err.message);
    }
  });
});
