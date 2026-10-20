/** Просмотр тренировочных планов клиента (training_plan) */
const API_BASE = "/api/client";
const state = { plans: [] };

function formatDateTime(iso) {
  if (typeof fcFormatDateTime === "function") return fcFormatDateTime(iso);
  if (!iso) return "—";
  const d = new Date(iso);
  return Number.isNaN(d.getTime())
    ? String(iso)
    : d.toLocaleString("ru-RU", { dateStyle: "short", timeStyle: "short", timeZone: "Europe/Samara" });
}

function esc(s) {
  return String(s ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function formatExercises(exercises) {
  if (!exercises) return "—";
  return esc(String(exercises))
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .join("<br>");
}

function renderPlans() {
  const container = document.getElementById("plans-container");
  if (!container) return;

  const myPlans = state.plans;

  if (!myPlans.length) {
    container.innerHTML = '<p class="muted">У вас пока нет тренировочных планов.</p>';
    return;
  }

  container.innerHTML = myPlans
    .map(
      (p) => `
    <div class="card" style="padding: 18px; margin-bottom: 16px;">
      <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px;">
        <div>
          <h3 style="margin:0 0 8px; font-size:1.2rem;">${esc(p.plan_name)}</h3>
          <p class="muted" style="margin:0 0 4px; font-size:0.9rem;">
            Тренер: ${esc(p.trainer_name || p.trainer_id)}
          </p>
          <p class="muted" style="margin:0; font-size:0.85rem;">
            Создан: ${esc(formatDateTime(p.created_at))}
            ${p.updated_at && p.updated_at !== p.created_at ? ` · Обновлён: ${esc(formatDateTime(p.updated_at))}` : ""}
          </p>
        </div>
      </div>
      ${p.description ? `<p style="margin:12px 0 0; color:var(--muted);">${esc(p.description)}</p>` : ""}
      <div style="margin-top:14px; padding:12px; background:var(--bg); border-radius:10px;">
        <strong style="display:block; margin-bottom:8px;">Упражнения</strong>
        <div style="line-height:1.6;">${formatExercises(p.exercises)}</div>
      </div>
    </div>
  `
    )
    .join("");
}

async function loadPlans() {
  const r = await fetch(`${API_BASE}/training-plans`);
  if (!r.ok) throw new Error("Не удалось загрузить тренировочные планы.");
  state.plans = await r.json();
  renderPlans();
}

document.addEventListener("DOMContentLoaded", async () => {
  try {
    await loadPlans();
  } catch (e) {
    const container = document.getElementById("plans-container");
    if (container) container.innerHTML = `<p class="error-message">${esc(e.message)}</p>`;
  }
});
