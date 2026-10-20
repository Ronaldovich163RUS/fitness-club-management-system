const API_BASE = (document.body.dataset.apiBase || "/api/admin").replace(/\/$/, "");

const GROUP_SERVICE_TYPE = "групповая тренировка";
const MAX_DURATION_MIN = 240;
/** Минимальный перерыв между занятиями одного тренера или в одном зале (как на сервере). */
const MIN_GAP_BETWEEN_SLOTS_MS = 10 * 60 * 1000;

let scheduleListCache = [];
let servicesOptions = [];
let trainersOptions = [];
let hallsOptions = [];

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

function formatApiErrors(fields) {
  if (!fields || typeof fields !== "object") return "";
  return Object.entries(fields)
    .map(([k, v]) => `${k}: ${v}`)
    .join("\n");
}

/** Как на страницах клиентов и тренеров — браузерный alert. */
function notify(msg) {
  if (msg) alert(msg);
}

function setScheduleFormError(msg) {
  notify(msg);
}

function startIsoToParts(iso) {
  if (!iso || typeof iso !== "string") return { date: "", time: "" };
  const s = iso.length >= 16 ? iso.slice(0, 19) : iso;
  return { date: s.slice(0, 10), time: s.slice(11, 16) };
}

function endFromStartDuration(date, timeHm, durationMin) {
  if (!date || !timeHm || !durationMin) return null;
  const d = new Date(`${date}T${timeHm}:00`);
  if (Number.isNaN(d.getTime())) return null;
  d.setMinutes(d.getMinutes() + durationMin);
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:00`;
}

function readEntityActive(o) {
  if (o == null || typeof o !== "object") return true;
  if (typeof o.is_active === "boolean") return o.is_active;
  if (typeof o.isActive === "boolean") return o.isActive;
  if (typeof o.active === "boolean") return o.active;
  const v = o.is_active ?? o.isActive ?? o.active;
  if (v === undefined || v === null) return true;
  return v === true || v === 1 || v === "1" || v === "true";
}

function entityIsActive(o) {
  if (!o || typeof o !== "object") return false;
  return readEntityActive(o);
}

function normalizeApiList(data) {
  if (Array.isArray(data)) return data;
  if (data && Array.isArray(data.content)) return data.content;
  if (data && Array.isArray(data.items)) return data.items;
  return [];
}

function isGroupService(s) {
  return String(s.service_type ?? s.serviceType ?? "")
    .trim()
    .toLowerCase() === GROUP_SERVICE_TYPE;
}

function isGroupSlot(s) {
  const st = String(s?.service_type ?? s?.serviceType ?? "")
    .trim()
    .toLowerCase();
  if (st === GROUP_SERVICE_TYPE) return true;
  const svcId = s?.service_id ?? s?.serviceId;
  if (svcId == null || svcId === "") return false;
  const svc = servicesOptions.find((x) => Number(serviceIdOf(x)) === Number(svcId));
  return svc ? isGroupService(svc) : false;
}

function slotStartMs(s) {
  const st = s?.start_datetime ?? s?.startDatetime;
  if (typeof fcClubDateTimeToMs === "function") return fcClubDateTimeToMs(st);
  if (!st) return null;
  const ms = new Date(st).getTime();
  return Number.isNaN(ms) ? null : ms;
}

function slotEndMs(s) {
  const en = s?.end_datetime ?? s?.endDatetime;
  if (typeof fcClubDateTimeToMs === "function") return fcClubDateTimeToMs(en);
  if (!en) return null;
  const ms = new Date(en).getTime();
  return Number.isNaN(ms) ? null : ms;
}

function clubNowMs() {
  if (typeof fcClubNowMs === "function") return fcClubNowMs();
  return Date.now();
}

/** Не завершённые занятия (окончание в поясе клуба ещё не наступило). */
function isFutureSlot(s) {
  const endMs = slotEndMs(s);
  const nowMs = clubNowMs();
  if (endMs == null || nowMs == null) return false;
  return endMs >= nowMs;
}

function filterSchedulesForAdminPage(list) {
  return (Array.isArray(list) ? list : []).filter((s) => isGroupSlot(s) && isFutureSlot(s));
}

function groupServicesForSelect() {
  return servicesOptions.filter((s) => entityIsActive(s) && isGroupService(s));
}

function serviceIdOf(s) {
  return s.service_id ?? s.serviceId;
}

function trainerIdOf(t) {
  return t.trainer_id ?? t.trainerId;
}

function hallIdOf(h) {
  return h.hall_id ?? h.hallId;
}

function hallCapacityById(id) {
  const n = Number(id);
  const h = hallsOptions.find((x) => Number(hallIdOf(x)) === n);
  const c = h != null ? h.capacity : null;
  return c != null && Number.isFinite(Number(c)) ? Number(c) : null;
}

function trainerLabel(t) {
  const mn = t.middle_name ?? t.middleName ?? "";
  const m = mn ? ` ${mn}` : "";
  return `${t.last_name ?? t.lastName ?? ""} ${t.first_name ?? t.firstName ?? ""}${m}`.trim();
}

function serviceName(id) {
  const s = servicesOptions.find((x) => serviceIdOf(x) === id);
  return s ? (s.service_name ?? s.serviceName) : id;
}

function hallName(id) {
  const h = hallsOptions.find((x) => hallIdOf(x) === id);
  return h ? (h.hall_name ?? h.hallName) : id;
}

function trainerNameById(id) {
  const t = trainersOptions.find((x) => trainerIdOf(x) === id);
  return t ? trainerLabel(t) : id;
}

/** В форме занятия — только активные; при редактировании оставляем в списке текущего тренера, если он уже неактивен. */
function trainerOptionsForScheduleSlotSelect() {
  const active = trainersOptions.filter((t) => entityIsActive(t));
  const sid = document.getElementById("schedule_id")?.value;
  if (!sid) {
    return active;
  }
  const row = scheduleListCache.find((x) => Number(x.schedule_id ?? x.scheduleId) === Number(sid));
  if (!row) {
    return active;
  }
  const curId = Number(row.trainer_id ?? row.trainerId);
  const cur = trainersOptions.find((t) => Number(trainerIdOf(t)) === curId);
  if (!cur || entityIsActive(cur)) {
    return active;
  }
  return [cur, ...active.filter((t) => Number(trainerIdOf(t)) !== curId)];
}

/** Все залы для формы (без фильтра is_active — иначе при NULL/false в БД список пустой). */
function hallOptionsForScheduleSlotSelect() {
  if (!hallsOptions.length) return [];
  const sid = document.getElementById("schedule_id")?.value;
  if (!sid) return hallsOptions;
  const row = scheduleListCache.find((x) => Number(x.schedule_id ?? x.scheduleId) === Number(sid));
  if (!row) return hallsOptions;
  const curId = Number(row.hall_id ?? row.hallId);
  if (!Number.isFinite(curId)) return hallsOptions;
  const has = hallsOptions.some((h) => Number(hallIdOf(h)) === curId);
  if (has) return hallsOptions;
  const fromRow = {
    hall_id: curId,
    hall_name: row.hall_name ?? row.hallName ?? `Зал #${curId}`,
    capacity: row.hall_capacity ?? row.hallCapacity,
  };
  return [fromRow, ...hallsOptions];
}

function formatDateTime(iso) {
  if (typeof fcFormatDateTime === "function") return fcFormatDateTime(iso);
  if (!iso) return "—";
  const s = typeof iso === "string" ? iso : "";
  const d = new Date(s);
  if (Number.isNaN(d.getTime())) return s;
  return d.toLocaleString("ru-RU", { dateStyle: "short", timeStyle: "short", timeZone: "Europe/Samara" });
}

function formatDate(iso) {
  if (!iso) return "—";
  const s = typeof iso === "string" ? iso : "";
  return s.length >= 10 ? s.slice(0, 10) : s;
}

function formatTime(iso) {
  if (!iso) return "—";
  const s = typeof iso === "string" ? iso : "";
  return s.length >= 16 ? s.slice(11, 16) : "—";
}

function refreshScheduleDateMin() {
  const today = new Date().toISOString().slice(0, 10);
  const el = document.getElementById("schedule_date");
  if (el) el.min = today;
  const qDate = document.getElementById("q-date");
  if (qDate) qDate.min = today;
}

function applyHallCapacityToMaxParticipants() {
  const hid = document.getElementById("schedule_hall_id")?.value;
  const inp = document.getElementById("schedule_max_participants");
  if (!inp) return;
  if (!hid) {
    inp.removeAttribute("max");
    return;
  }
  const cap = hallCapacityById(Number(hid));
  if (cap != null && cap > 0) {
    inp.setAttribute("max", String(cap));
    const cur = Number(inp.value);
    if (cur > cap) inp.value = String(cap);
  } else {
    inp.removeAttribute("max");
  }
}

function syncScheduleCancelledRowVisibility() {
  const row = document.getElementById("schedule-cancelled-row");
  if (!row) return;
  const isNew = !document.getElementById("schedule_id").value;
  row.hidden = isNew;
  if (isNew) {
    document.getElementById("schedule_is_cancelled").value = "false";
  }
}

function ensureServiceOptionForEdit(slot) {
  const sel = document.getElementById("schedule_service_id");
  const id = slot?.service_id ?? slot?.serviceId;
  if (!sel || id == null || id === "") return;
  const exists = [...sel.options].some((o) => Number(o.value) === Number(id));
  if (!exists) {
    const name = (slot.service_name ?? slot.serviceName) || serviceName(id);
    const opt = document.createElement("option");
    opt.value = String(id);
    opt.textContent = name;
    sel.insertBefore(opt, sel.options[1] || null);
  }
  sel.value = String(id);
}

function parseSlotBoundsMs(s) {
  const st = s.start_datetime ?? s.startDatetime;
  const en = s.end_datetime ?? s.endDatetime;
  if (!st || !en) return null;
  const a = new Date(st).getTime();
  const b = new Date(en).getTime();
  if (Number.isNaN(a) || Number.isNaN(b)) return null;
  return { start: a, end: b };
}

function isSlotCancelled(s) {
  const v = s.is_cancelled ?? s.isCancelled;
  return v === true || v === 1 || v === "1" || v === "true";
}

/** Нет интервала ≥10 мин между концом одного слота и началом другого (зал или тренер). */
function findScheduleGapViolation(startMs, endMs, hallId, trainerId, excludeScheduleId) {
  for (const s of scheduleListCache) {
    if (isSlotCancelled(s)) continue;
    const id = s.schedule_id ?? s.scheduleId;
    if (excludeScheduleId != null && excludeScheduleId !== "" && Number(id) === Number(excludeScheduleId)) {
      continue;
    }
    const ob = parseSlotBoundsMs(s);
    if (!ob) continue;
    const rangeStart = startMs - MIN_GAP_BETWEEN_SLOTS_MS;
    const rangeEnd = endMs + MIN_GAP_BETWEEN_SLOTS_MS;
    if (ob.start < rangeEnd && ob.end > rangeStart) {
      if (Number(s.hall_id ?? s.hallId) === Number(hallId)) return "hall";
      if (Number(s.trainer_id ?? s.trainerId) === Number(trainerId)) return "trainer";
    }
  }
  return null;
}

function getScheduleFormPayload() {
  const date = document.getElementById("schedule_date").value;
  const timeStart = document.getElementById("schedule_time_start").value;
  const duration = Number(document.getElementById("schedule_duration").value);
  const start_datetime = date && timeStart ? `${date}T${timeStart}:00` : null;
  const end_datetime = start_datetime ? endFromStartDuration(date, timeStart, duration) : null;
  const curVal = document.getElementById("schedule_current_participants").value;
  const sid = document.getElementById("schedule_id").value;
  const isCancelled =
    sid && document.getElementById("schedule-cancelled-row") && !document.getElementById("schedule-cancelled-row").hidden
      ? document.getElementById("schedule_is_cancelled").value === "true"
      : false;
  return {
    service_id: Number(document.getElementById("schedule_service_id").value),
    trainer_id: Number(document.getElementById("schedule_trainer_id").value),
    hall_id: Number(document.getElementById("schedule_hall_id").value),
    start_datetime,
    end_datetime,
    max_participants: Number(document.getElementById("schedule_max_participants").value),
    current_participants: curVal === "" ? null : Number(curVal),
    is_cancelled: isCancelled,
  };
}

function setScheduleFormData(s) {
  document.getElementById("schedule_id").value = s?.schedule_id ?? s?.scheduleId ?? "";
  document.getElementById("schedule_service_id").value = s?.service_id ?? s?.serviceId ?? "";
  document.getElementById("schedule_trainer_id").value = s?.trainer_id ?? s?.trainerId ?? "";
  document.getElementById("schedule_hall_id").value = s?.hall_id ?? s?.hallId ?? "";
  const start = s?.start_datetime ?? s?.startDatetime;
  const startStr = typeof start === "string" ? start : "";
  if (startStr) {
    const { date, time } = startIsoToParts(startStr);
    document.getElementById("schedule_date").value = date;
    document.getElementById("schedule_time_start").value = time;
  } else {
    document.getElementById("schedule_date").value = "";
    document.getElementById("schedule_time_start").value = "";
  }
  const endRaw = s?.end_datetime ?? s?.endDatetime;
  const dur =
    startStr && endRaw && typeof endRaw === "string"
      ? (new Date(endRaw) - new Date(startStr)) / 60000
      : 60;
  document.getElementById("schedule_duration").value = Math.min(
    Number.isFinite(dur) ? dur : 60,
    MAX_DURATION_MIN,
  );
  document.getElementById("schedule_max_participants").value = s?.max_participants ?? s?.maxParticipants ?? 20;
  document.getElementById("schedule_current_participants").value =
    s?.current_participants != null || s?.currentParticipants != null
      ? String(s.current_participants ?? s.currentParticipants)
      : "0";
  document.getElementById("schedule_is_cancelled").value =
    (s?.is_cancelled ?? s?.isCancelled) ? "true" : "false";
  syncScheduleCancelledRowVisibility();
  ensureServiceOptionForEdit(s);
  applyHallCapacityToMaxParticipants();
  refreshScheduleDateMin();
}

function clearScheduleForm() {
  document.getElementById("schedule_id").value = "";
  document.getElementById("schedule_service_id").value = "";
  document.getElementById("schedule_trainer_id").value = "";
  document.getElementById("schedule_hall_id").value = "";
  document.getElementById("schedule_date").value = "";
  document.getElementById("schedule_time_start").value = "";
  document.getElementById("schedule_duration").value = 60;
  document.getElementById("schedule_max_participants").value = 20;
  document.getElementById("schedule_current_participants").value = "0";
  document.getElementById("schedule_is_cancelled").value = "false";
  setScheduleFormError("");
  syncScheduleCancelledRowVisibility();
  refreshScheduleDateMin();
  const inp = document.getElementById("schedule_max_participants");
  if (inp) inp.removeAttribute("max");
  fillSelects();
}

function fillSelects() {
  const opt = (v, text) => `<option value="${esc(v)}">${esc(text)}</option>`;
  const selService = document.getElementById("schedule_service_id");
  const selTrainer = document.getElementById("schedule_trainer_id");
  const selHall = document.getElementById("schedule_hall_id");
  const qService = document.getElementById("q-service");
  const qTrainer = document.getElementById("q-trainer");
  const qHall = document.getElementById("q-hall");
  const workloadTrainer = document.getElementById("workload-trainer");

  const groups = groupServicesForSelect();
  const hallsForForm = hallOptionsForScheduleSlotSelect();

  if (selService) {
    selService.innerHTML =
      '<option value="">— Выберите —</option>' +
      groups.map((s) => opt(serviceIdOf(s), s.service_name ?? s.serviceName ?? "")).join("");
  }
  const trainersForSlotForm = trainerOptionsForScheduleSlotSelect();
  if (selTrainer) {
    selTrainer.innerHTML =
      '<option value="">— Выберите —</option>' +
      trainersForSlotForm.map((t) => opt(trainerIdOf(t), trainerLabel(t))).join("");
  }
  if (selHall) {
    selHall.innerHTML =
      '<option value="">— Выберите —</option>' +
      hallsForForm
        .map((h) => {
          const id = hallIdOf(h);
          const label = (h.hall_name ?? h.hallName ?? "").trim() || `Зал #${id}`;
          return opt(id, label);
        })
        .join("");
  }

  if (qService) {
    qService.innerHTML =
      "<option value=\"\">Все</option>" +
      groups.map((s) => opt(serviceIdOf(s), s.service_name ?? s.serviceName ?? "")).join("");
  }
  if (qTrainer) {
    qTrainer.innerHTML =
      "<option value=\"\">Все</option>" +
      trainersOptions.map((t) => opt(trainerIdOf(t), trainerLabel(t))).join("");
  }
  if (qHall) {
    qHall.innerHTML =
      "<option value=\"\">Все</option>" +
      hallsOptions
        .map((h) => {
          const id = hallIdOf(h);
          const label = (h.hall_name ?? h.hallName ?? "").trim() || `Зал #${id}`;
          return opt(id, label);
        })
        .join("");
  }
  if (workloadTrainer) {
    workloadTrainer.innerHTML =
      '<option value="">Выберите тренера</option>' +
      trainersOptions.map((t) => opt(trainerIdOf(t), trainerLabel(t))).join("");
  }

  syncScheduleCancelledRowVisibility();
  applyHallCapacityToMaxParticipants();
}

function applySearch() {
  const qDate = document.getElementById("q-date").value;
  const qService = document.getElementById("q-service").value;
  const qTrainer = document.getElementById("q-trainer").value;
  const qHall = document.getElementById("q-hall").value;
  const filtered = scheduleListCache.filter((s) => {
    const startRaw = s.start_datetime ?? s.startDatetime;
    const start = typeof startRaw === "string" ? startRaw : "";
    const okDate = !qDate || start.startsWith(qDate);
    const okService = !qService || String(s.service_id ?? s.serviceId) === qService;
    const okTrainer = !qTrainer || String(s.trainer_id ?? s.trainerId) === qTrainer;
    const okHall = !qHall || String(s.hall_id ?? s.hallId) === qHall;
    return okDate && okService && okTrainer && okHall;
  });
  renderScheduleTable(filtered);
}

function renderScheduleTable(list) {
  const tbody = document.getElementById("schedule-tbody");
  tbody.innerHTML = "";
  if (!list.length) {
    tbody.innerHTML =
      "<tr><td colspan=\"8\" class=\"muted\">Нет предстоящих групповых занятий по выбранным фильтрам</td></tr>";
    return;
  }
  for (const s of list) {
    const schId = s.schedule_id ?? s.scheduleId;
    const svcId = s.service_id ?? s.serviceId;
    const trnId = s.trainer_id ?? s.trainerId;
    const hId = s.hall_id ?? s.hallId;
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc((s.service_name ?? s.serviceName) || serviceName(svcId))}</td>
      <td>${esc((s.trainer_full_name ?? s.trainerFullName) || trainerNameById(trnId))}</td>
      <td>${esc((s.hall_name ?? s.hallName) || hallName(hId))}</td>
      <td>${esc(formatDateTime(s.start_datetime ?? s.startDatetime))}</td>
      <td>${esc(formatDateTime(s.end_datetime ?? s.endDatetime))}</td>
      <td>${esc(s.max_participants ?? s.maxParticipants)}</td>
      <td>${(s.is_cancelled ?? s.isCancelled) ? "Да" : "Нет"}</td>
      <td>
        <div class="admin-actions" style="justify-content:flex-start;">
          <button class="admin-btn secondary" type="button" data-action="edit" data-id="${esc(schId)}">Редактировать</button>
          <button class="admin-btn danger" type="button" data-action="delete" data-id="${esc(schId)}">Удалить</button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  }
}

function renderWorkload() {
  const trainerId = document.getElementById("workload-trainer").value;
  const tbody = document.getElementById("workload-tbody");
  tbody.innerHTML = "";
  if (!trainerId) {
    tbody.innerHTML = "<tr><td colspan=\"7\" class=\"muted\">Выберите тренера</td></tr>";
    return;
  }
  const list = scheduleListCache
    .filter((s) => String(s.trainer_id ?? s.trainerId) === trainerId)
    .sort((a, b) => (slotStartMs(a) ?? 0) - (slotStartMs(b) ?? 0));
  if (!list.length) {
    tbody.innerHTML = "<tr><td colspan=\"7\" class=\"muted\">Нет предстоящих групповых занятий</td></tr>";
    return;
  }
  for (const s of list) {
    const startD = s.start_datetime ?? s.startDatetime;
    const endD = s.end_datetime ?? s.endDatetime;
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc(formatDate(startD))}</td>
      <td>${esc((s.service_name ?? s.serviceName) || serviceName(s.service_id ?? s.serviceId))}</td>
      <td>Групповая</td>
      <td>${esc((s.hall_name ?? s.hallName) || hallName(s.hall_id ?? s.hallId))}</td>
      <td>${esc(formatTime(startD))} – ${esc(formatTime(endD))}</td>
      <td>${esc(s.current_participants ?? s.currentParticipants)} / ${esc(s.max_participants ?? s.maxParticipants)}</td>
      <td>${(s.is_cancelled ?? s.isCancelled) ? "Да" : "Нет"}</td>
    `;
    tbody.appendChild(tr);
  }
}

async function loadAll() {
  const warnings = [];
  const [schedules, services, trainers, halls] = await Promise.all([
    apiRequest("/schedules").catch((e) => {
      warnings.push("расписание");
      throw e;
    }),
    apiRequest("/services").catch((e) => {
      warnings.push("услуги");
      throw e;
    }),
    apiRequest("/trainers").catch((e) => {
      warnings.push("тренеры");
      throw e;
    }),
    apiRequest("/halls").catch((e) => {
      warnings.push(`залы (${e.message || "ошибка API"})`);
      return [];
    }),
  ]);
  servicesOptions = normalizeApiList(services);
  trainersOptions = normalizeApiList(trainers);
  hallsOptions = normalizeApiList(halls);
  scheduleListCache = filterSchedulesForAdminPage(normalizeApiList(schedules));
  if (warnings.length) {
    notify(warnings.join("; "));
  } else if (!hallsOptions.length) {
    notify(
      "Список залов пуст. Добавьте залы в разделе «Зоны и залы» → «Залы», затем обновите страницу.",
    );
  }
  fillSelects();
  applySearch();
  renderWorkload();
}

document.addEventListener("DOMContentLoaded", () => {
  refreshScheduleDateMin();
  syncScheduleCancelledRowVisibility();

  document.getElementById("schedule_hall_id")?.addEventListener("change", () => {
    applyHallCapacityToMaxParticipants();
  });

  (async () => {
    try {
      await loadAll();
    } catch (e) {
      notify(formatApiErrors(e.fields) || e.message || "Не удалось загрузить данные.");
      document.getElementById("schedule-tbody").innerHTML =
        `<tr><td colspan="8" class="muted">Данные не загружены</td></tr>`;
    }
  })();

  document.getElementById("btn-search")?.addEventListener("click", applySearch);
  document.getElementById("btn-clear-search")?.addEventListener("click", () => {
    document.getElementById("q-date").value = "";
    document.getElementById("q-service").value = "";
    document.getElementById("q-trainer").value = "";
    document.getElementById("q-hall").value = "";
    applySearch();
  });

  document.getElementById("workload-trainer")?.addEventListener("change", renderWorkload);

  document.getElementById("schedule-tbody")?.addEventListener("click", async (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;
    const id = Number(btn.dataset.id);
    const item = scheduleListCache.find((x) => (x.schedule_id ?? x.scheduleId) === id);
    if (!item) return;
    if (btn.dataset.action === "edit") {
      setScheduleFormData(item);
      document.getElementById("scheduleForm")?.scrollIntoView({ behavior: "smooth", block: "start" });
      return;
    }
    if (btn.dataset.action === "delete") {
      if (!confirm("Удалить занятие из расписания?")) return;
      setScheduleFormError("");
      try {
        await apiRequest(`/schedules/${id}`, { method: "DELETE" });
        await loadAll();
        if (Number(document.getElementById("schedule_id").value) === id) clearScheduleForm();
      } catch (err) {
        notify(formatApiErrors(err.fields) || err.message);
      }
    }
  });

  document.getElementById("btn-reset-schedule")?.addEventListener("click", () => {
    clearScheduleForm();
  });

  document.getElementById("scheduleForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    setScheduleFormError("");
    const form = e.target;
    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }
    const sid = document.getElementById("schedule_id").value;
    const payload = getScheduleFormPayload();
    if (!payload.start_datetime || !payload.end_datetime) {
      setScheduleFormError("Укажите дату и время начала.");
      return;
    }
    const d = new Date();
    const startMs = new Date(payload.start_datetime).getTime();
    if (!sid && startMs < d.getTime() - 60000) {
      setScheduleFormError("Нельзя добавлять занятие в прошлом.");
      return;
    }
    const t = document.getElementById("schedule_time_start").value;
    if (t < "06:00" || t > "22:00") {
      setScheduleFormError("Время начала: с 06:00 до 22:00.");
      return;
    }
    const dur = Number(document.getElementById("schedule_duration").value);
    if (dur > MAX_DURATION_MIN) {
      setScheduleFormError(`Длительность не более ${MAX_DURATION_MIN} мин.`);
      return;
    }
    if (!sid) {
      payload.current_participants = 0;
      payload.is_cancelled = false;
    }
    if (!payload.is_cancelled) {
      const startMs = new Date(payload.start_datetime).getTime();
      const endMs = new Date(payload.end_datetime).getTime();
      const viol = findScheduleGapViolation(startMs, endMs, payload.hall_id, payload.trainer_id, sid || null);
      if (viol === "hall") {
        setScheduleFormError(
          "В зале следующее занятие может начаться не ранее чем через 10 мин после окончания предыдущего.",
        );
        return;
      }
      if (viol === "trainer") {
        setScheduleFormError("Между двумя занятиями одного тренера должно пройти не менее 10 мин.");
        return;
      }
    }
    try {
      if (!sid) {
        await apiRequest("/schedules", { method: "POST", body: JSON.stringify(payload) });
        clearScheduleForm();
        await loadAll();
        alert("Групповое занятие добавлено в расписание.");
      } else {
        await apiRequest(`/schedules/${Number(sid)}`, { method: "PUT", body: JSON.stringify(payload) });
        clearScheduleForm();
        await loadAll();
        alert("Изменения сохранены.");
      }
    } catch (err) {
      setScheduleFormError(formatApiErrors(err.fields) || err.message);
    }
  });
});
