const scheduleState = { data: getClientData(), schedule: getScheduleList() };

function fillScheduleFilters() {
  const qService = document.getElementById("q-service");
  const qTrainer = document.getElementById("q-trainer");
  qService.innerHTML = "<option value=\"\">Все</option>" + scheduleState.data.service
    .filter((s) => s.is_active)
    .map((s) => `<option value="${s.service_id}">${esc(s.service_name)}</option>`).join("");
  qTrainer.innerHTML = "<option value=\"\">Все</option>" + scheduleState.data.trainer
    .filter((t) => t.is_active)
    .map((t) => `<option value="${t.trainer_id}">${esc([t.last_name, t.first_name, t.middle_name].filter(Boolean).join(" "))}</option>`).join("");
}

function renderScheduleTable() {
  const qDate = document.getElementById("q-date").value;
  const qType = document.getElementById("q-type").value;
  const qService = document.getElementById("q-service").value;
  const qTrainer = document.getElementById("q-trainer").value;
  const tbody = document.getElementById("client-schedule-tbody");
  const filtered = scheduleState.schedule.filter((row) => {
    const svc = serviceById(scheduleState.data, row.service_id);
    if (!svc) return false;
    const okDate = !qDate || String(row.start_datetime || "").startsWith(qDate);
    const okType = !qType || svc.service_type === qType;
    const okService = !qService || String(row.service_id) === qService;
    const okTrainer = !qTrainer || String(row.trainer_id) === qTrainer;
    return okDate && okType && okService && okTrainer;
  });
  tbody.innerHTML = "";
  if (!filtered.length) {
    tbody.innerHTML = "<tr><td colspan=\"7\" class=\"muted\">По фильтрам нет занятий.</td></tr>";
    return;
  }
  filtered.sort((a, b) => String(a.start_datetime || "").localeCompare(String(b.start_datetime || "")));
  filtered.forEach((row) => {
    const svc = serviceById(scheduleState.data, row.service_id);
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc(formatDateTime(row.start_datetime))}</td>
      <td>${esc(svc?.service_name || row.service_id)}</td>
      <td>${esc(svc?.service_type || "—")}</td>
      <td>${esc(trainerNameById(scheduleState.data, row.trainer_id))}</td>
      <td>${esc(hallNameById(scheduleState.data, row.hall_id))}</td>
      <td>${esc(row.current_participants)} / ${esc(row.max_participants)}</td>
      <td>${row.is_cancelled ? "Отменена" : "Активна"}</td>
    `;
    tbody.appendChild(tr);
  });
}

document.addEventListener("DOMContentLoaded", () => {
  fillScheduleFilters();
  renderScheduleTable();
  document.getElementById("btn-search")?.addEventListener("click", renderScheduleTable);
  document.getElementById("btn-reset")?.addEventListener("click", () => {
    document.getElementById("q-date").value = "";
    document.getElementById("q-type").value = "";
    document.getElementById("q-service").value = "";
    document.getElementById("q-trainer").value = "";
    renderScheduleTable();
  });
});
