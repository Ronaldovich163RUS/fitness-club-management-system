const trainerProfileState = { profile: null };

function setProfileMessage(msg, isError) {
  if (msg) alert(msg);
}

function fillForm(pd) {
  document.getElementById("trainer_id").value = pd.trainer_id;
  document.getElementById("personal_data_id").value = pd.personal_data_id;
  document.getElementById("last_name").value = pd.last_name || "";
  document.getElementById("first_name").value = pd.first_name || "";
  document.getElementById("middle_name").value = pd.middle_name || "";
  document.getElementById("birth_date").value = pd.birth_date || "";
  document.getElementById("phone").value = pd.phone || "";
  document.getElementById("email").value = pd.email || "";
  document.getElementById("gender").value = pd.gender || "Мужской";
  document.getElementById("registration_address").value = pd.registration_address || "";
  document.getElementById("actual_address").value = pd.actual_address || "";
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
  if (err.message) return String(err.message);
  return fallback;
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
  const contentType = res.headers.get("content-type") || "";
  if (contentType.includes("application/json")) return res.json();
  return null;
}

async function loadProfile() {
  const profile = await api("/api/trainer/profile");
  trainerProfileState.profile = profile;
  fillForm(profile);
}

async function saveProfile() {
  await api("/api/trainer/profile", {
    method: "PUT",
    body: JSON.stringify({
      last_name: document.getElementById("last_name").value.trim(),
      first_name: document.getElementById("first_name").value.trim(),
      middle_name: document.getElementById("middle_name").value.trim() || null,
      birth_date: document.getElementById("birth_date").value,
      phone: document.getElementById("phone").value.trim(),
      email: document.getElementById("email").value.trim(),
      gender: document.getElementById("gender").value,
      registration_address: document.getElementById("registration_address").value.trim() || null,
      actual_address: document.getElementById("actual_address").value.trim() || null
    })
  });
  setProfileMessage("Данные тренера сохранены.", false);
}

document.addEventListener("DOMContentLoaded", async () => {
  try {
    await loadProfile();
  } catch (err) {
    setProfileMessage(readError(err, "Не удалось загрузить профиль тренера."), true);
  }
  document.getElementById("trainer-profile-form")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.target;
    setProfileMessage("", false);
    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }
    try {
      await saveProfile();
    } catch (err) {
      setProfileMessage(readError(err, "Не удалось сохранить профиль."), true);
    }
  });
});
