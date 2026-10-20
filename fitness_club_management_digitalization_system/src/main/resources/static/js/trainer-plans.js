const trainerPlansState = { clients: [], plans: [] };

function setMessage(id, msg, isError) {
  if (msg) alert(msg);
}

function readError(err, fallback) {
  if (!err) return fallback;
  if (typeof err === "string") return err;
  if (err.fields && typeof err.fields === "object") {
    const first = Object.values(err.fields)[0];
    if (first) return String(first);
  }
  if (err.errors && typeof err.errors === "object") {
    const first = Object.values(err.errors)[0];
    if (first) return String(first);
  }
  return err.message || fallback;
}

async function api(url, options = {}) {
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json" },
    ...options
  });
  if (!res.ok) {
    let payload = null;
    try { payload = await res.json(); } catch (_) {}
    throw payload || { message: `HTTP ${res.status}` };
  }
  const type = res.headers.get("content-type") || "";
  if (type.includes("application/json")) return res.json();
  return null;
}

function fillClients() {
  const sel = document.getElementById("trainer-client-id");
  sel.innerHTML = "<option value=\"\">— Выберите клиента —</option>" + trainerPlansState.clients
    .map((c) => `<option value="${c.client_id}">${esc(c.full_name)}</option>`).join("");
}

function renderPlanEditor() {
  const cid = Number(document.getElementById("trainer-client-id").value || 0);
  const delBtn = document.getElementById("btn-plan-delete");
  if (!cid) {
    clearPlanForm();
    if (delBtn) delBtn.disabled = true;
    return;
  }
  const list = trainerPlansState.plans
    .filter((p) => p.client_id === cid)
    .sort((a, b) => String(b.updated_at || b.created_at).localeCompare(String(a.updated_at || a.created_at)));
  const p = list.length ? list[0] : null;
  document.getElementById("plan_id").value = p?.plan_id || "";
  document.getElementById("plan_name").value = p?.plan_name || "";
  document.getElementById("plan_description").value = p?.description || "";
  document.getElementById("plan_exercises").value = p?.exercises || "";
  if (delBtn) delBtn.disabled = !p;
}

function firstPlanForSelectedClient() {
  const cid = Number(document.getElementById("trainer-client-id").value || 0);
  if (!cid) return null;
  const list = trainerPlansState.plans
    .filter((p) => p.client_id === cid)
    .sort((a, b) => String(b.updated_at || b.created_at).localeCompare(String(a.updated_at || a.created_at)));
  return list.length ? list[0] : null;
}

function clearPlanForm() {
  document.getElementById("plan_id").value = "";
  document.getElementById("plan_name").value = "";
  document.getElementById("plan_description").value = "";
  document.getElementById("plan_exercises").value = "";
}

async function loadData() {
  const [clients, plans] = await Promise.all([
    api("/api/trainer/clients"),
    api("/api/trainer/plans")
  ]);
  trainerPlansState.clients = clients || [];
  trainerPlansState.plans = plans || [];
  fillClients();
  const sel = document.getElementById("trainer-client-id");
  if (sel && !sel.value && trainerPlansState.clients.length) {
    sel.value = String(trainerPlansState.clients[0].client_id);
  }
  renderPlanEditor();
}

document.addEventListener("DOMContentLoaded", async () => {
  try {
    await loadData();
  } catch (err) {
    setMessage("plan-message", readError(err, "Не удалось загрузить планы тренировок."), true);
  }

  document.getElementById("trainer-client-id")?.addEventListener("change", () => {
    renderPlanEditor();
  });

  document.getElementById("plan-form")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    setMessage("plan-message", "", false);
    const cid = Number(document.getElementById("trainer-client-id").value || 0);
    if (!cid) {
      setMessage("plan-message", "Выберите клиента.", true);
      return;
    }
    const name = document.getElementById("plan_name").value.trim();
    const desc = document.getElementById("plan_description").value.trim();
    const ex = document.getElementById("plan_exercises").value.trim();
    if (!name || !ex) {
      setMessage("plan-message", "Заполните название и упражнения.", true);
      return;
    }
    const id = Number(document.getElementById("plan_id").value || 0);
    const existing = firstPlanForSelectedClient();
    try {
      if (id || existing) {
        const targetId = id || existing.plan_id;
        await api(`/api/trainer/plans/${targetId}`, {
          method: "PUT",
          body: JSON.stringify({ plan_name: name, description: desc || null, exercises: ex })
        });
      } else {
        await api("/api/trainer/plans", {
          method: "POST",
          body: JSON.stringify({ client_id: cid, plan_name: name, description: desc || null, exercises: ex })
        });
      }
      await loadData();
      setMessage("plan-message", "План сохранен.", false);
    } catch (err) {
      setMessage("plan-message", readError(err, "Не удалось сохранить план."), true);
    }
  });

  document.getElementById("btn-plan-delete")?.addEventListener("click", async () => {
    setMessage("plan-message", "", false);
    const id = Number(document.getElementById("plan_id").value || 0);
    if (!id) {
      setMessage("plan-message", "План для выбранного клиента отсутствует.", true);
      return;
    }
    try {
      await api(`/api/trainer/plans/${id}`, { method: "DELETE" });
      await loadData();
      setMessage("plan-message", "План удален.", false);
    } catch (err) {
      setMessage("plan-message", readError(err, "Не удалось удалить план."), true);
    }
  });
});
