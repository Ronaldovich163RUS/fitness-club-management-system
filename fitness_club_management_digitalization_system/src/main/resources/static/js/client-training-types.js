document.addEventListener("DOMContentLoaded", () => {
  const data = getClientData();
  const tbody = document.getElementById("training-types-tbody");
  const list = data.service.filter((s) => s.is_active && (s.service_type === "групповая тренировка" || s.service_type === "персональная тренировка"));
  tbody.innerHTML = "";
  if (!list.length) {
    tbody.innerHTML = "<tr><td colspan=\"4\" class=\"muted\">Нет доступных видов тренировок.</td></tr>";
    return;
  }
  list.forEach((s) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc(s.service_name)}</td>
      <td>${esc(s.service_type)}</td>
      <td>${esc(s.duration_minutes)}</td>
      <td>${esc(s.description || "—")}</td>
    `;
    tbody.appendChild(tr);
  });
});
