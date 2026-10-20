const CLIENT_DATA_KEY = "fc_client_data_v1";
const SCHEDULE_KEY = "fc_schedule";
const REGISTRATION_KEY = "fc_registration";
const CURRENT_CLIENT_KEY = "fc_current_client_id";
const CURRENT_TRAINER_KEY = "fc_current_trainer_id";
const NOTIFICATIONS_KEY = "fc_notifications";
const CLUB_STATS_KEY = "fc_club_stats";
const TRAINER_ZONE_KEY = "fc_trainer_zone";
const TRAINING_PLAN_KEY = "fc_training_plan";
const MEMBERSHIP_PURCHASES_KEY = "fc_membership_purchases";
const SERVICE_PURCHASES_KEY = "fc_service_purchases";
const MEMBERSHIP_USAGE_KEY = "fc_membership_usage";

function seedClientData() {
  return {
    personal_data: [
      {
        personal_data_id: 1001,
        last_name: "Иванов",
        first_name: "Иван",
        middle_name: "Иванович",
        birth_date: "1999-05-10",
        phone: "+7 (999) 123-45-67",
        email: "ivanov.ivan@example.com",
        gender: "Мужской",
        registration_address: "г. Москва, ул. Примерная, д. 1",
        actual_address: "г. Москва, ул. Примерная, д. 1"
      },
      {
        personal_data_id: 1002,
        last_name: "Петрова",
        first_name: "Анна",
        middle_name: "Сергеевна",
        birth_date: "1995-11-22",
        phone: "+7 (901) 555-22-11",
        email: "petrova.anna@example.com",
        gender: "Женский",
        registration_address: "г. Москва, ул. Лесная, д. 5",
        actual_address: "г. Москва, ул. Лесная, д. 5"
      },
      {
        personal_data_id: 2001,
        last_name: "Морозова",
        first_name: "Анна",
        middle_name: "Павловна",
        birth_date: "1992-08-14",
        phone: "+7 (999) 222-33-44",
        email: "morozova.anna@example.com",
        gender: "Женский",
        registration_address: "г. Москва, ул. Клубная, д. 12",
        actual_address: "г. Москва, ул. Клубная, д. 12"
      },
      {
        personal_data_id: 2002,
        last_name: "Ильина",
        first_name: "Мария",
        middle_name: "Олеговна",
        birth_date: "1989-03-19",
        phone: "+7 (901) 777-88-99",
        email: "ilina.maria@example.com",
        gender: "Женский",
        registration_address: "г. Москва, ул. Фитнес, д. 7",
        actual_address: "г. Москва, ул. Фитнес, д. 7"
      }
    ],
    client: [
      { client_id: 101, personal_data_id: 1001, is_active: true },
      { client_id: 102, personal_data_id: 1002, is_active: true }
    ],
    service: [
      { service_id: 1, service_name: "Функциональный тренинг", service_type: "групповая тренировка", zone_id: 2, duration_minutes: 55, description: "Интенсивная работа на все группы мышц", base_price: 500, is_active: true },
      { service_id: 2, service_name: "Йога (Hatha)", service_type: "групповая тренировка", zone_id: 2, duration_minutes: 60, description: "Гибкость, дыхание и баланс", base_price: 450, is_active: true },
      { service_id: 5, service_name: "Персональная тренировка", service_type: "персональная тренировка", zone_id: 1, duration_minutes: 60, description: "Индивидуальная тренировка с тренером", base_price: 2500, is_active: true },
      { service_id: 6, service_name: "Доступ в тренажёрный зал", service_type: "доступ в зону", zone_id: 1, duration_minutes: 120, description: "Разовый доступ к тренажёрному залу", base_price: 900, is_active: true },
      { service_id: 7, service_name: "Дополнительная услуга", service_type: "дополнительная услуга", zone_id: 1, duration_minutes: 45, description: "Разовая доп. услуга по согласованию", base_price: 1200, is_active: true }
    ],
    trainer: [
      { trainer_id: 201, personal_data_id: 2001, last_name: "Морозова", first_name: "Анна", middle_name: "Павловна", specialization: "Силовые тренировки", experience_years: 6, rating: 4.8, biography: "Силовые и функциональные тренировки", is_active: true },
      { trainer_id: 202, personal_data_id: 2002, last_name: "Ильина", first_name: "Мария", middle_name: "Олеговна", specialization: "Йога", experience_years: 8, rating: 4.9, biography: "Йога и mobility", is_active: true }
    ],
    hall: [
      { hall_id: 1, hall_name: "Main Hall", zone_id: 1, is_active: true },
      { hall_id: 2, hall_name: "Studio Hall", zone_id: 2, is_active: true }
    ],
    zone: [
      { zone_id: 1, zone_name: "Тренажёрный зал", description: "Силовые и кардио" },
      { zone_id: 2, zone_name: "Групповые залы", description: "Йога, пилатес, функционал" }
    ],
    documents: {},
    membership: [
      { membership_id: 201, membership_name: "Start 30", description: "30 дней, безлимит групповых", price: 3500, duration_days: 30, visits_limit: null, freeze_days: 3, guest_visits: 1, is_active: true },
      { membership_id: 202, membership_name: "Active 90", description: "90 дней, расширенный доступ", price: 8500, duration_days: 90, visits_limit: null, freeze_days: 7, guest_visits: 2, is_active: true }
    ],
    membership_service: [
      { membership_service_id: 1, membership_id: 201, service_id: 1, included_visits: null, max_per_week: null },
      { membership_service_id: 2, membership_id: 201, service_id: 2, included_visits: null, max_per_week: null },
      { membership_service_id: 3, membership_id: 201, service_id: 5, included_visits: null, max_per_week: null },
      { membership_service_id: 4, membership_id: 202, service_id: 1, included_visits: null, max_per_week: null },
      { membership_service_id: 5, membership_id: 202, service_id: 2, included_visits: null, max_per_week: null },
      { membership_service_id: 6, membership_id: 202, service_id: 5, included_visits: null, max_per_week: null }
    ],
    training_plan: [
      { plan_id: 1, client_id: 101, trainer_id: 201, plan_name: "Базовый силовой", description: "План для развития общей силы", exercises: "1. Приседания 3×12\n2. Жим лёжа 3×10\n3. Тяга к поясу 3×12\n4. Планка 3×60 сек", created_at: "2025-03-01T10:00", updated_at: "2025-03-10T14:30" },
      { plan_id: 2, client_id: 101, trainer_id: 202, plan_name: "Растяжка и гибкость", description: "Улучшение подвижности", exercises: "1. Динамическая растяжка 10 мин\n2. Наклоны к ногам 3×15\n3. Шпагат у стены 5 мин\n4. Скручивания лёжа 3×10", created_at: "2025-03-05T09:00", updated_at: "2025-03-05T09:00" },
      { plan_id: 3, client_id: 102, trainer_id: 201, plan_name: "Функциональный блок", description: "Комплекс для функциональной подготовки", exercises: "1. Берпи 3×8\n2. Прыжки на тумбу 3×10\n3. Броски медбола 3×12\n4. Farmer walk 3×30 м", created_at: "2025-03-12T11:00", updated_at: "2025-03-12T11:00" }
    ]
  };
}

function getTrainingPlans() {
  const seeded = seedClientData();
  const fallback = Array.isArray(seeded.training_plan) ? seeded.training_plan : [];
  return readJson(TRAINING_PLAN_KEY, fallback);
}

function saveTrainingPlans(list) {
  writeJson(TRAINING_PLAN_KEY, list);
}

function esc(value) {
  return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;").replaceAll("'", "&#039;");
}

function readJson(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch (_) {
    return fallback;
  }
}

function writeJson(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch (_) {}
}

function getClientData() {
  const seeded = seedClientData();
  const data = readJson(CLIENT_DATA_KEY, seeded);
  if (!data || !Array.isArray(data.client)) {
    writeJson(CLIENT_DATA_KEY, seeded);
    return seeded;
  }
  if (!data.documents || typeof data.documents !== "object") data.documents = {};
  if (!Array.isArray(data.membership)) data.membership = seeded.membership || [];
  if (!Array.isArray(data.membership_service)) data.membership_service = seeded.membership_service || [];
  if (Array.isArray(data.trainer)) {
    data.trainer = data.trainer.map((t) => {
      const st = seeded.trainer.find((x) => x.trainer_id === t.trainer_id);
      return {
        ...st,
        ...t,
        personal_data_id: t.personal_data_id ?? st?.personal_data_id ?? null
      };
    });
  }
  if (Array.isArray(data.personal_data)) {
    seeded.personal_data.forEach((sp) => {
      if (!data.personal_data.some((p) => p.personal_data_id === sp.personal_data_id)) {
        data.personal_data.push(sp);
      }
    });
  }
  const seedHalls = seeded.hall || [];
  if (Array.isArray(data.hall)) {
    data.hall = data.hall.map((h) => {
      if (h.zone_id != null) return h;
      const sh = seedHalls.find((s) => s.hall_id === h.hall_id);
      return { ...h, zone_id: sh?.zone_id ?? null };
    });
  }
  return data;
}

function saveClientData(data) {
  writeJson(CLIENT_DATA_KEY, data);
}

function getDefaultSchedule() {
  const now = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  const d = (addDays) => {
    const x = new Date(now);
    x.setDate(x.getDate() + addDays);
    return x.getFullYear() + "-" + pad(x.getMonth() + 1) + "-" + pad(x.getDate());
  };
  return [
    { schedule_id: 1, service_id: 1, trainer_id: 201, hall_id: 1, start_datetime: d(2) + "T10:00", end_datetime: d(2) + "T10:55", max_participants: 20, current_participants: 12, is_cancelled: false },
    { schedule_id: 2, service_id: 2, trainer_id: 202, hall_id: 2, start_datetime: d(2) + "T11:00", end_datetime: d(2) + "T12:00", max_participants: 15, current_participants: 8, is_cancelled: false },
    { schedule_id: 4, service_id: 5, trainer_id: 202, hall_id: 2, start_datetime: d(2) + "T14:00", end_datetime: d(2) + "T15:00", max_participants: 1, current_participants: 1, is_cancelled: false },
    { schedule_id: 5, service_id: 5, trainer_id: 201, hall_id: 1, start_datetime: d(3) + "T12:00", end_datetime: d(3) + "T13:00", max_participants: 1, current_participants: 0, is_cancelled: false }
  ];
}

function getDefaultRegistrations(scheduleList) {
  const slot4 = scheduleList.find((s) => s.schedule_id === 4);
  if (!slot4) return [];
  return [
    { registration_id: 1, client_id: 101, schedule_id: 4, status: "активна", registration_date: new Date().toISOString() }
  ];
}

function getScheduleList() {
  const raw = localStorage.getItem(SCHEDULE_KEY);
  if (raw === null || raw === "") {
    const list = getDefaultSchedule();
    writeJson(SCHEDULE_KEY, list);
    return list;
  }
  try {
    const list = JSON.parse(raw);
    return Array.isArray(list) ? list : getDefaultSchedule();
  } catch (_) {
    return getDefaultSchedule();
  }
}

function saveScheduleList(list) {
  writeJson(SCHEDULE_KEY, list);
}

function getRegistrations() {
  const raw = localStorage.getItem(REGISTRATION_KEY);
  if (raw === null || raw === "") {
    const scheduleList = getScheduleList();
    const list = getDefaultRegistrations(scheduleList);
    if (list.length > 0) writeJson(REGISTRATION_KEY, list);
    return list;
  }
  try {
    const list = JSON.parse(raw);
    return Array.isArray(list) ? list : [];
  } catch (_) {
    return [];
  }
}

function saveRegistrations(list) {
  writeJson(REGISTRATION_KEY, list);
}

function getTrainerZone() {
  const raw = localStorage.getItem(TRAINER_ZONE_KEY);
  if (raw == null || raw === "") {
    const seed = [
      { trainer_id: 201, zone_id: 1 },
      { trainer_id: 202, zone_id: 2 }
    ];
    writeJson(TRAINER_ZONE_KEY, seed);
    return seed;
  }
  return readJson(TRAINER_ZONE_KEY, []);
}

function getMembershipPurchases() {
  return readJson(MEMBERSHIP_PURCHASES_KEY, []);
}

function saveMembershipPurchases(list) {
  writeJson(MEMBERSHIP_PURCHASES_KEY, list);
}

function getServicePurchases() {
  return readJson(SERVICE_PURCHASES_KEY, []);
}

function saveServicePurchases(list) {
  writeJson(SERVICE_PURCHASES_KEY, list);
}

function getMembershipUsage() {
  return readJson(MEMBERSHIP_USAGE_KEY, []);
}

function saveMembershipUsage(list) {
  writeJson(MEMBERSHIP_USAGE_KEY, list);
}

function getNotifications() {
  return readJson(NOTIFICATIONS_KEY, []);
}

function saveNotifications(list) {
  writeJson(NOTIFICATIONS_KEY, list);
}

function getClubStats() {
  const def = { id: 1, current_visitors: 0 };
  return readJson(CLUB_STATS_KEY, def);
}

function saveClubStats(stats) {
  writeJson(CLUB_STATS_KEY, stats);
}

function fullName(personal) {
  return [personal.last_name, personal.first_name, personal.middle_name].filter(Boolean).join(" ");
}

function trainerNameById(data, trainerId) {
  const t = data.trainer.find((x) => x.trainer_id === trainerId);
  return t ? [t.last_name, t.first_name, t.middle_name].filter(Boolean).join(" ") : String(trainerId);
}

function serviceById(data, serviceId) {
  return data.service.find((s) => s.service_id === serviceId);
}

function hallNameById(data, hallId) {
  return data.hall.find((h) => h.hall_id === hallId)?.hall_name ?? String(hallId);
}

function hallById(data, hallId) {
  return data.hall.find((h) => h.hall_id === hallId);
}

function zoneNameById(data, zoneId) {
  return data.zone?.find((z) => z.zone_id === zoneId)?.zone_name ?? String(zoneId);
}

function getDocumentsFor(data, personalDataId) {
  const id = Number(personalDataId);
  if (!data.documents[id]) data.documents[id] = [];
  return data.documents[id];
}

function formatDateTime(iso) {
  if (typeof fcFormatDateTime === "function") return fcFormatDateTime(iso);
  if (!iso) return "—";
  const d = new Date(iso);
  return Number.isNaN(d.getTime())
    ? String(iso)
    : d.toLocaleString("ru-RU", { dateStyle: "short", timeStyle: "short", timeZone: "Europe/Samara" });
}

function nextId(list, field) {
  return Math.max(0, ...list.map((x) => Number(x[field] ?? 0))) + 1;
}

function getCurrentClientId(data) {
  const stored = Number(localStorage.getItem(CURRENT_CLIENT_KEY) || 0);
  const exists = data.client.some((c) => c.client_id === stored && c.is_active);
  if (exists) return stored;
  const first = data.client.find((c) => c.is_active);
  return first ? first.client_id : null;
}

function getCurrentTrainerId(data) {
  const stored = Number(localStorage.getItem(CURRENT_TRAINER_KEY) || 0);
  const exists = data.trainer.some((t) => t.trainer_id === stored && t.is_active);
  if (exists) return stored;
  const first = data.trainer.find((t) => t.is_active);
  return first ? first.trainer_id : null;
}

function getTrainerById(data, trainerId) {
  return data.trainer.find((t) => t.trainer_id === trainerId) || null;
}

function getPersonalDataById(data, personalDataId) {
  return data.personal_data.find((p) => p.personal_data_id === personalDataId) || null;
}
