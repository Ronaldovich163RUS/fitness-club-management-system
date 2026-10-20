const API_BASE = (document.body.dataset.apiBase || "/api/client").replace(/\/$/, "");

function esc(value) {
  return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;").replaceAll("'", "&#039;");
}

function formatDateTime(iso) {
  if (typeof fcFormatDateTime === "function") return fcFormatDateTime(iso);
  if (iso == null || iso === "") return "—";
  const d = typeof iso === "number" ? new Date(iso) : new Date(iso);
  return Number.isNaN(d.getTime())
    ? String(iso)
    : d.toLocaleString("ru-RU", { dateStyle: "short", timeStyle: "short", timeZone: "Europe/Samara" });
}

function purchaseHistoryWhen(item) {
  return item?.date ?? item?.purchase_date ?? item?.created_at;
}

/** Дата/время покупки: сервер отдаёт уже строку в самарском поясе (не ISO). */
function purchaseHistoryDateLabel(item) {
  const v = purchaseHistoryWhen(item);
  if (v == null || v === "") return "—";
  return String(v);
}

function money(v) {
  const n = Number(v ?? 0);
  return `${n.toLocaleString("ru-RU")} ₽`;
}

async function apiRequest(path, options = {}) {
  const headers = { ...options.headers };
  if (options.body && !headers["Content-Type"]) headers["Content-Type"] = "application/json";
  const r = await fetch(`${API_BASE}${path}`, { ...options, headers });
  const text = await r.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); } catch (_) { data = { error: text.slice(0, 200) }; }
  }
  if (!r.ok) {
    const fieldMsg = data?.fields && typeof data.fields === "object" ? Object.values(data.fields)[0] : null;
    throw new Error(fieldMsg || data?.error || r.statusText || "Ошибка запроса");
  }
  return data;
}

function membershipCard(m) {
  return `
    <article class="card offer-card">
      <div>
        <div class="offer-title">${esc(m.membership_name)}</div>
      </div>

      <div class="offer-desc">${esc(m.description || "")}</div>

      <div class="chips" aria-label="Опции">
        <span class="chip">${esc(String(m.duration_days))} дней</span>
        <span class="chip muted">Заморозка: ${esc(String(m.freeze_days ?? 0))} дн.</span>
        <span class="chip muted">Гостевые: ${esc(String(m.guest_visits ?? 0))}</span>
        <span class="chip muted">Полотенце: ${m.towel ? "Да" : "Нет"}</span>
        <span class="chip muted">Парковка: ${m.parking ? "Да" : "Нет"}</span>
      </div>

      <div class="offer-bottom">
        <div class="offer-price">${esc(money(m.price))}</div>
        <div style="display:flex;gap:8px;">
          <button class="btn" type="button" data-buy="membership" data-id="${m.membership_id}">Купить</button>
          <button class="btn btn-secondary" type="button" data-more-membership="${m.membership_id}">Подробнее</button>
        </div>
      </div>
    </article>
  `;
}

function serviceCard(s) {
  return `
    <article class="card offer-card">
      <div>
        <div class="offer-title">${esc(s.service_name)}</div>
      </div>

      <div class="chips" aria-label="Параметры">
        <span class="chip">${esc(String(s.duration_minutes))} мин</span>
        <span class="chip muted">${esc(s.service_type)}</span>
      </div>

      <div class="offer-bottom">
        <div class="offer-price">${esc(money(s.base_price ?? s.price ?? 0))}</div>
        <div style="display:flex;gap:8px;">
          <button class="btn" type="button" data-buy="service" data-id="${s.service_id}">Купить</button>
          <button class="btn btn-secondary" type="button" data-more-service="${s.service_id}">Подробнее</button>
        </div>
      </div>
    </article>
  `;
}

function openModal(id) {
  const root = document.getElementById("client-modal-root");
  const m = document.getElementById(id);
  if (!root || !m) return;
  root.hidden = false;
  root.setAttribute("aria-hidden", "false");
  root.querySelectorAll(".admin-modal").forEach((x) => { x.hidden = true; });
  m.hidden = false;
}

function closeModal(id) {
  const root = document.getElementById("client-modal-root");
  const m = document.getElementById(id);
  if (m) m.hidden = true;
  if (root) {
    root.hidden = true;
    root.setAttribute("aria-hidden", "true");
  }
}

document.addEventListener("DOMContentLoaded", () => {
  const membershipWrap = document.getElementById("membership-cards");
  const serviceWrap = document.getElementById("service-cards");

  if (membershipWrap) {
    apiRequest("/memberships/catalog").then((list) => {
      membershipWrap.innerHTML = (Array.isArray(list) ? list : []).map((m) => membershipCard(m)).join("");
    });
  }

  if (serviceWrap) {
    apiRequest("/services/catalog").then((list) => {
      serviceWrap.innerHTML = (Array.isArray(list) ? list : []).map((s) => serviceCard(s)).join("");
    });
  }

  document.addEventListener("click", (e) => {
    const btn = e.target.closest("button[data-buy]");
    if (!btn) return;
    const buy = btn.dataset.buy;
    const id = btn.dataset.id;
    if (buy === "membership") {
      apiRequest("/purchases/memberships", { method: "POST", body: JSON.stringify({ membership_id: Number(id) }) })
        .then(() => { window.location.href = "/client/memberships"; })
        .catch((e) => alert(e.message));
    }
    if (buy === "service") {
      apiRequest("/purchases/services", { method: "POST", body: JSON.stringify({ service_id: Number(id) }) })
        .then(() => { window.location.href = "/client/memberships"; })
        .catch((e) => alert(e.message));
    }
  });

  document.addEventListener("click", (e) => {
    const mbtn = e.target.closest("button[data-more-membership]");
    if (mbtn) {
      const id = Number(mbtn.dataset.moreMembership);
      const tbody = document.getElementById("membership-rules-tbody");
      apiRequest(`/memberships/${id}/rules`).then((rows) => {
        const list = Array.isArray(rows) ? rows : [];
        tbody.innerHTML = list.length ? list.map((r) => `
          <tr>
            <td>${esc(r.service_name ?? "—")}</td>
            <td>${esc(r.included_visits ?? "безлимит")}</td>
            <td>${esc(r.remaining ?? "—")}</td>
            <td>${esc(r.max_per_week ?? "—")}</td>
            <td>${esc(r.days_of_week ?? "—")}</td>
            <td>${esc(r.time_start ?? "—")}</td>
            <td>${esc(r.time_end ?? "—")}</td>
          </tr>`).join("") : '<tr><td colspan="7" class="muted">Нет правил</td></tr>';
        openModal("membership-rules-modal");
      }).catch((e) => alert(e.message));
    }

    const sbtn = e.target.closest("button[data-more-service]");
    if (sbtn) {
      const id = Number(sbtn.dataset.moreService);
      const tbody = document.getElementById("service-rules-tbody");
      apiRequest(`/services/${id}/rules`).then((r) => {
        tbody.innerHTML = `<tr>
          <td>${esc(r.included_visits ?? "—")}</td>
          <td>${esc(r.remaining ?? "—")}</td>
          <td>${esc(r.max_per_week ?? "—")}</td>
          <td>${esc(r.days_of_week ?? "—")}</td>
          <td>${esc(r.time_start ?? "—")}</td>
          <td>${esc(r.time_end ?? "—")}</td>
        </tr>`;
        openModal("service-rules-modal");
      }).catch((e) => alert(e.message));
    }

    if (e.target.id === "close-membership-rules-modal") closeModal("membership-rules-modal");
    if (e.target.id === "close-service-rules-modal") closeModal("service-rules-modal");
    if (e.target.matches("[data-modal-dismiss='root']")) closeModal();
  });

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") closeModal();
  });
});

