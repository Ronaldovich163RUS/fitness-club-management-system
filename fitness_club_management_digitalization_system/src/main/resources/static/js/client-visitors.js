function refreshCount() {
  const api = (document.body.dataset.apiBase || "/api/client").replace(/\/$/, "");
  fetch(`${api}/visitors`).then((r) => r.ok ? r.json() : null).then((stats) => {
    const el = document.getElementById("visitors-count");
    if (el) el.textContent = stats?.current_visitors ?? 0;
  });
}

document.addEventListener("DOMContentLoaded", () => {
  refreshCount();
  document.getElementById("btn-refresh-visitors")?.addEventListener("click", refreshCount);
  setInterval(refreshCount, 10000);
});
