const API_BASE = (document.body.dataset.apiBase || "/api/public").replace(/\/$/, "");

function esc(s) {
  return String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#039;");
}

function money(v) {
  return `${Number(v).toLocaleString("ru-RU")} ₽`;
}

function yesNo(flag) {
  return flag ? "Да" : "Нет";
}

function membershipCard(m) {
  const el = document.createElement("article");
  el.className = "card offer-card";
  const visits = m.visits_limit == null ? "Безлимит" : String(m.visits_limit);

  el.innerHTML = `
    <div>
      <div class="offer-title">${esc(m.membership_name)}</div>
    </div>

    <div class="offer-desc">${esc(m.description || "")}</div>

    <div class="chips" aria-label="Опции">
      <span class="chip">${esc(String(m.duration_days))} дней</span>
      <span class="chip muted">${esc(visits)} посещений</span>
      <span class="chip muted">Заморозка: ${esc(String(m.freeze_days ?? 0))} дн.</span>
      <span class="chip muted">Гостевые: ${esc(String(m.guest_visits ?? 0))}</span>
      <span class="chip muted">Парковка: ${esc(yesNo(m.parking))}</span>
      <span class="chip muted">Полотенце: ${esc(yesNo(m.towel))}</span>
      <span class="chip muted">Активен: ${esc(yesNo(m.is_active))}</span>
    </div>

    <div class="offer-bottom">
      <div class="offer-price">${esc(money(m.price))}</div>
    </div>
  `;

  return el;
}

function serviceCard(s) {
  const el = document.createElement("article");
  el.className = "card offer-card";

  el.innerHTML = `
    <div>
      <div class="offer-title">${esc(s.service_name)}</div>
    </div>

    <div class="offer-desc">${esc(s.description || "")}</div>

    <div class="chips" aria-label="Параметры">
      <span class="chip">${esc(String(s.duration_minutes))} мин</span>
      <span class="chip muted">${esc(String(s.zone_name || "Без зоны"))}</span>
      <span class="chip muted">Разовая услуга</span>
      <span class="chip muted">Активна: ${esc(yesNo(s.is_active))}</span>
    </div>

    <div class="offer-bottom">
      <div class="offer-price">${esc(money(s.base_price))}</div>
    </div>
  `;

  return el;
}

function setTab(which) {
  const mTab = document.getElementById("tab-memberships");
  const sTab = document.getElementById("tab-services");
  const mSection = document.getElementById("memberships");
  const sSection = document.getElementById("services");

  const membershipsActive = which === "memberships";
  mTab.classList.toggle("active", membershipsActive);
  sTab.classList.toggle("active", !membershipsActive);

  mSection.hidden = !membershipsActive;
  sSection.hidden = membershipsActive;
}

function init() {
  const mg = document.getElementById("membership-grid");
  const sg = document.getElementById("service-grid");
  const toJsonSafe = (r) => (r.ok ? r.json().catch(() => []) : []);
  Promise.all([
    fetch(`${API_BASE}/memberships/catalog`).then(toJsonSafe),
    fetch(`${API_BASE}/services/catalog`).then(toJsonSafe),
  ]).then(([memberships, services]) => {
    const safeMemberships = Array.isArray(memberships) ? memberships : [];
    const safeServices = Array.isArray(services) ? services : [];
    mg.innerHTML = "";
    sg.innerHTML = "";
    for (const m of safeMemberships) mg.appendChild(membershipCard(m));
    for (const s of safeServices) sg.appendChild(serviceCard(s));
  });

  document.getElementById("tab-memberships").addEventListener("click", () => setTab("memberships"));
  document.getElementById("tab-services").addEventListener("click", () => setTab("services"));
}

document.addEventListener("DOMContentLoaded", init);

