/**
 * Отображение дат в часовом поясе клуба (как com.fitnessclub.ClubZone на сервере).
 * Без этого toLocaleString использует пояс браузера, а не Самару.
 * Переопределение: атрибут data-club-timezone на body.
 */
(function () {
  const tz =
    (typeof document !== "undefined" &&
      document.body &&
      document.body.dataset &&
      document.body.dataset.clubTimezone) ||
    "Europe/Samara";

  window.fcClubDisplayTimezone = tz;

  window.fcFormatDateTime = function (iso) {
    if (iso == null || iso === "") return "—";
    const d = typeof iso === "number" ? new Date(iso) : new Date(iso);
    if (Number.isNaN(d.getTime())) return String(iso);
    return d.toLocaleString("ru-RU", {
      dateStyle: "short",
      timeStyle: "short",
      timeZone: tz,
    });
  };

  window.fcFormatDateRu = function (iso) {
    if (iso == null || iso === "") return "—";
    const d = typeof iso === "number" ? new Date(iso) : new Date(iso);
    if (Number.isNaN(d.getTime())) return String(iso);
    return d.toLocaleDateString("ru-RU", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      timeZone: tz,
    });
  };

  window.fcFormatTimeRu = function (iso) {
    if (iso == null || iso === "") return "—";
    const d = typeof iso === "number" ? new Date(iso) : new Date(iso);
    if (Number.isNaN(d.getTime())) return String(iso);
    return d.toLocaleTimeString("ru-RU", { hour: "2-digit", minute: "2-digit", timeZone: tz });
  };

  window.fcWeekdayLongRu = function (iso) {
    if (iso == null || iso === "") return "—";
    const d = typeof iso === "number" ? new Date(iso) : new Date(iso);
    if (Number.isNaN(d.getTime())) return String(iso);
    return new Intl.DateTimeFormat("ru-RU", { weekday: "long", timeZone: tz }).format(d);
  };

  /** LocalDateTime с API (без смещения) → epoch ms в поясе клуба. */
  window.fcClubDateTimeToMs = function (iso) {
    if (iso == null || iso === "") return null;
    const raw = String(iso).trim().replace(" ", "T");
    if (!raw) return null;
    if (/[Zz]$/.test(raw) || /[+-]\d{2}:\d{2}$/.test(raw)) {
      const ms = new Date(raw).getTime();
      return Number.isNaN(ms) ? null : ms;
    }
    const bare = raw.slice(0, 19);
    if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(bare)) {
      const ms = new Date(raw).getTime();
      return Number.isNaN(ms) ? null : ms;
    }
    const probe = new Date();
    const parts = new Intl.DateTimeFormat("en-US", {
      timeZone: tz,
      timeZoneName: "shortOffset",
    }).formatToParts(probe);
    const tzPart = parts.find((p) => p.type === "timeZoneName");
    let offset = "+00:00";
    if (tzPart && tzPart.value) {
      const m = tzPart.value.match(/GMT([+-])(\d{1,2})(?::?(\d{2}))?/);
      if (m) {
        const sign = m[1];
        const hh = String(m[2]).padStart(2, "0");
        const mm = String(m[3] ?? "0").padStart(2, "0");
        offset = `${sign}${hh}:${mm}`;
      }
    }
    const ms = new Date(`${bare}${offset}`).getTime();
    return Number.isNaN(ms) ? null : ms;
  };

  window.fcClubNowMs = function () {
    const probe = new Date();
    const parts = new Intl.DateTimeFormat("sv-SE", {
      timeZone: tz,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
    }).format(probe);
    return window.fcClubDateTimeToMs(parts.replace(" ", "T"));
  };
})();
