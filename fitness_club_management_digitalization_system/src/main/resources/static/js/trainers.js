const API_BASE = (document.body.dataset.apiBase || "/api/public").replace(/\/$/, "");
let trainers = [];

function esc(s) {
  return String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#039;");
}

function uniq(values) {
  return [...new Set(values)].sort((a, b) => a.localeCompare(b, "ru"));
}

function yearWord(n) {
  const x = Math.abs(Number(n) || 0) % 100;
  const y = x % 10;
  if (x >= 11 && x <= 14) return "лет";
  if (y === 1) return "год";
  if (y >= 2 && y <= 4) return "года";
  return "лет";
}

function fillSelect(select, values, anyLabel) {
  const current = select.value;
  const options = [`<option value="">${esc(anyLabel)}</option>`, ...values.map((v) => `<option value="${esc(String(v))}">${esc(String(v))}</option>`)];
  select.innerHTML = options.join("");
  if ([...select.options].some((o) => o.value === current)) select.value = current;
}

function starSvg() {
  return `
    <svg class="star" viewBox="0 0 24 24" aria-hidden="true">
      <path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/>
    </svg>
  `;
}

function initials(name) {
  const parts = String(name).trim().split(/\s+/).slice(0, 2);
  return parts.map((p) => p[0]?.toUpperCase()).join("");
}

function renderCard(t) {
  const el = document.createElement("article");
  el.className = "card trainer-card";

  el.dataset.exp = String(Number(t.experience_years ?? 0));
  el.dataset.spec = String(t.specialization ?? "");
  el.dataset.rating = String(t.rating ?? 0);

  const photoHtml = t.photo_url
    ? `<img class="trainer-photo" src="${esc(t.photo_url)}" alt="">`
    : `<div class="avatar" aria-hidden="true">${esc(initials(t.full_name))}</div>`;

  el.innerHTML = `
    <div class="trainer-top">
      <div class="trainer-photo-wrap">${photoHtml}</div>
      <div>
        <div class="trainer-name">${esc(t.full_name)}</div>
        <div class="trainer-sub">${esc(t.specialization)}</div>
      </div>
    </div>

    <div class="bio">${esc(t.biography || "")}</div>

    <div class="meta-row">
      <div class="meta">
        <div class="k">Опыт</div>
        <div class="v">${esc(String(t.experience_years))} ${esc(yearWord(t.experience_years))}</div>
      </div>
      <div class="meta">
        <div class="k">Рейтинг</div>
        <div class="v rating">${starSvg()} ${esc(String(t.rating))}</div>
      </div>
    </div>

    <div class="trainer-actions"></div>
  `;

  return el;
}

function apply() {
  const expValueRaw = document.getElementById("f-experience").value;
  const expValue = expValueRaw === "" ? null : Number(expValueRaw);
  const spec = document.getElementById("f-specialization").value;
  const ratingValueRaw = document.getElementById("f-rating").value;
  const ratingValue = ratingValueRaw === "" ? null : Number(ratingValueRaw);

  const filtered = trainers.filter((t) => {
    const exp = Number(t.experience_years ?? 0);
    const rating = Number(t.rating ?? 0);
    const trainerSpec = String(t.specialization ?? "");
    return (expValue === null || exp === expValue) && (!spec || trainerSpec === spec) && (ratingValue === null || rating === ratingValue);
  });

  const grid = document.getElementById("grid");
  grid.innerHTML = "";
  for (const t of filtered) grid.appendChild(renderCard(t));

  const isEmpty = filtered.length === 0;
  document.getElementById("empty").hidden = !isEmpty;
  grid.hidden = isEmpty;
}

function clearAll() {
  document.getElementById("f-experience").value = "";
  document.getElementById("f-specialization").value = "";
  document.getElementById("f-rating").value = "";
  apply();
}

function init() {
  fetch(`${API_BASE}/trainers`).then((r) => r.ok ? r.json() : []).then((list) => {
    trainers = Array.isArray(list) ? list : [];
    const expValues = [...new Set(trainers.map((t) => Number(t.experience_years ?? 0)).filter((x) => Number.isFinite(x)))].sort((a, b) => a - b);
    const ratingValues = [...new Set(trainers.map((t) => Number(t.rating ?? 0)).filter((x) => Number.isFinite(x)))].sort((a, b) => a - b);
    const specValues = uniq(trainers.map((t) => String(t.specialization ?? "")).filter((x) => x));
    fillSelect(document.getElementById("f-experience"), expValues, "Любой");
    fillSelect(document.getElementById("f-specialization"), specValues, "Все");
    fillSelect(document.getElementById("f-rating"), ratingValues, "Любой");
    const grid = document.getElementById("grid");
    grid.innerHTML = "";
    for (const t of trainers) grid.appendChild(renderCard(t));
    apply();
  });
  document.getElementById("btn-find").addEventListener("click", apply);
  document.getElementById("btn-clear").addEventListener("click", clearAll);
}

document.addEventListener("DOMContentLoaded", init);

