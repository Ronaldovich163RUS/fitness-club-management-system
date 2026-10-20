-- Создание базы данных (если необходимо)
CREATE DATABASE IF NOT EXISTS fitness_club
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE fitness_club;

-- 1. Таблица персональных данных (общая для всех физических лиц)
CREATE TABLE personal_data (
    personal_data_id INT AUTO_INCREMENT PRIMARY KEY,
    last_name VARCHAR(50) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    middle_name VARCHAR(50) NULL,
    birth_date DATE NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    gender ENUM('Мужской', 'Женский') NULL,
    password_hash VARCHAR(255) NOT NULL,
    registration_address TEXT NULL,
    actual_address TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. Таблица администраторов (роль)
CREATE TABLE admin (
    admin_id INT AUTO_INCREMENT PRIMARY KEY,
    personal_data_id INT NOT NULL UNIQUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (personal_data_id) REFERENCES personal_data(personal_data_id) ON DELETE CASCADE
);

-- 3. Таблица клиентов (роль)
CREATE TABLE client (
    client_id INT AUTO_INCREMENT PRIMARY KEY,
    personal_data_id INT NOT NULL UNIQUE,
    is_active BOOLEAN DEFAULT TRUE,
    has_student_id BOOLEAN DEFAULT FALSE,
    has_pension_cert BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (personal_data_id) REFERENCES personal_data(personal_data_id) ON DELETE CASCADE
);

-- 4. Таблица тренеров (роль)
CREATE TABLE trainer (
    trainer_id INT AUTO_INCREMENT PRIMARY KEY,
    personal_data_id INT NOT NULL UNIQUE,
    specialization VARCHAR(100) NOT NULL,
    experience_years INT NOT NULL,
    rating DECIMAL(3,2) NULL,
    is_active BOOLEAN DEFAULT TRUE,
    biography TEXT NULL,
    photo_url VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (personal_data_id) REFERENCES personal_data(personal_data_id) ON DELETE CASCADE
);

-- 5. Таблица документов (один документ – одна запись)
CREATE TABLE documents (
    document_id INT AUTO_INCREMENT PRIMARY KEY,
    personal_data_id INT NOT NULL,
    document_type ENUM(
        'PASSPORT_RF',
        'INN',
        'SNILS',
        'MEDICAL_BOOK_PAPER',
        'MEDICAL_BOOK_ELECTRONIC',
        'DIPLOMA_HIGHER',
        'DIPLOMA_SECONDARY',
        'CERTIFICATE',
        'WORK_RECORD_PAPER',
        'STUDENT_ID',
        'PENSION_CERT'
    ) NOT NULL,
    series VARCHAR(20) NULL,
    number VARCHAR(30) NOT NULL,
    issued_by VARCHAR(255) NULL,
    issue_date DATE NULL,
    expiry_date DATE NULL,
    notes TEXT NULL,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (personal_data_id) REFERENCES personal_data(personal_data_id) ON DELETE CASCADE,
    UNIQUE KEY unique_document (personal_data_id, document_type, number)
);

-- 6. Таблица зон (для группировки услуг)
CREATE TABLE zone (
    zone_id INT AUTO_INCREMENT PRIMARY KEY,
    zone_name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT NULL
);

-- 7. Таблица услуг (тренировок и прочих активностей, исключая аренду)
CREATE TABLE service (
    service_id INT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    service_type ENUM(
        'групповая тренировка',
        'персональная тренировка',
        'доступ в зону',
        'дополнительная услуга'
    ) NOT NULL,
    duration_minutes INT NOT NULL,
    description TEXT NULL,
    base_price DECIMAL(10,2) NOT NULL,
    zone_id INT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_by_admin_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (zone_id) REFERENCES zone(zone_id) ON DELETE SET NULL,
    FOREIGN KEY (created_by_admin_id) REFERENCES admin(admin_id)
);

-- 8. Таблица абонементов (без аренды шкафчика)
CREATE TABLE membership (
    membership_id INT AUTO_INCREMENT PRIMARY KEY,
    membership_name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    price DECIMAL(10,2) NOT NULL,
    duration_days INT NOT NULL,
    visits_limit INT NULL,
    freeze_days INT NULL,
    guest_visits INT NULL,
    parking BOOLEAN DEFAULT FALSE,
    towel BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_by_admin_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by_admin_id) REFERENCES admin(admin_id)
);

-- 9. Таблица связи абонемента с услугами (правила посещения)
CREATE TABLE membership_service (
    membership_service_id INT AUTO_INCREMENT PRIMARY KEY,
    membership_id INT NOT NULL,
    service_id INT NOT NULL,
    included_visits INT NULL,
    max_per_week INT NULL,
    days_of_week SET('пн','вт','ср','чт','пт','сб','вс') NULL,
    time_start TIME NULL,
    time_end TIME NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (membership_id) REFERENCES membership(membership_id) ON DELETE CASCADE,
    FOREIGN KEY (service_id) REFERENCES service(service_id) ON DELETE CASCADE,
    UNIQUE KEY unique_membership_service (membership_id, service_id)
);

-- 10. Таблица покупок абонементов клиентами (sold_by_admin_id может быть NULL для онлайн-покупок)
CREATE TABLE membership_purchase (
    purchase_id INT AUTO_INCREMENT PRIMARY KEY,
    client_id INT NOT NULL,
    membership_id INT NOT NULL,
    purchase_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    remaining_visits INT NULL,
    price DECIMAL(10,2) NOT NULL,
    status ENUM('активен', 'истек', 'отменен') NOT NULL,
    sold_by_admin_id INT NULL,
    remaining_guest_visits INT DEFAULT 0,
    remaining_freeze_days INT DEFAULT 0,
    freeze_started_on DATE NULL,
    freeze_requested_days INT NULL,
    FOREIGN KEY (client_id) REFERENCES client(client_id),
    FOREIGN KEY (membership_id) REFERENCES membership(membership_id),
    FOREIGN KEY (sold_by_admin_id) REFERENCES admin(admin_id)
);

-- 11. Таблица учёта использования услуг по абонементу
CREATE TABLE membership_usage (
    usage_id INT AUTO_INCREMENT PRIMARY KEY,
    membership_purchase_id INT NOT NULL,
    service_id INT NOT NULL,
    used_at DATETIME NOT NULL,
    FOREIGN KEY (membership_purchase_id) REFERENCES membership_purchase(purchase_id) ON DELETE CASCADE,
    FOREIGN KEY (service_id) REFERENCES service(service_id) ON DELETE CASCADE
);

-- 12. Таблица залов (связь с зоной)
CREATE TABLE hall (
    hall_id INT AUTO_INCREMENT PRIMARY KEY,
    hall_name VARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    description TEXT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    zone_id INT NULL,
    created_by_admin_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (zone_id) REFERENCES zone(zone_id) ON DELETE SET NULL,
    FOREIGN KEY (created_by_admin_id) REFERENCES admin(admin_id)
);

-- 13. Таблица расписания (с возможностью создания тренером)
CREATE TABLE schedule (
    schedule_id INT AUTO_INCREMENT PRIMARY KEY,
    service_id INT NOT NULL,
    trainer_id INT NOT NULL,
    hall_id INT NOT NULL,
    start_datetime DATETIME NOT NULL,
    end_datetime DATETIME NOT NULL,
    max_participants INT NOT NULL,
    current_participants INT DEFAULT 0,
    is_cancelled BOOLEAN DEFAULT FALSE,
    created_by_admin_id INT NULL,
    created_by_trainer_id INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (service_id) REFERENCES service(service_id),
    FOREIGN KEY (trainer_id) REFERENCES trainer(trainer_id),
    FOREIGN KEY (hall_id) REFERENCES hall(hall_id),
    FOREIGN KEY (created_by_admin_id) REFERENCES admin(admin_id) ON DELETE SET NULL,
    FOREIGN KEY (created_by_trainer_id) REFERENCES trainer(trainer_id) ON DELETE SET NULL
);

-- 14. Таблица покупок услуг (разовые, не по абонементу) – schedule_id может быть NULL
CREATE TABLE service_purchase (
    purchase_id INT AUTO_INCREMENT PRIMARY KEY,
    client_id INT NOT NULL,
    service_id INT NOT NULL,
    schedule_id INT NULL,
    purchase_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    price DECIMAL(10,2) NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    sold_by_admin_id INT NULL,
    created_at DATETIME(6) NULL DEFAULT NULL,
	used_at DATETIME(6) NULL DEFAULT NULL,
    FOREIGN KEY (client_id) REFERENCES client(client_id),
    FOREIGN KEY (service_id) REFERENCES service(service_id),
    FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id),
    FOREIGN KEY (sold_by_admin_id) REFERENCES admin(admin_id)
);

-- 15. Таблица уведомлений
CREATE TABLE notification (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    recipient_type ENUM('client', 'trainer') NOT NULL,
    recipient_client_id INT NULL,
    recipient_trainer_id INT NULL,
    sender_admin_id INT NULL,
    sender_trainer_id INT NULL,
    notification_type ENUM('email', 'system') NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    is_sent BOOLEAN DEFAULT FALSE,
    sent_datetime TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recipient_client_id) REFERENCES client(client_id) ON DELETE CASCADE,
    FOREIGN KEY (recipient_trainer_id) REFERENCES trainer(trainer_id) ON DELETE CASCADE,
    FOREIGN KEY (sender_admin_id) REFERENCES admin(admin_id) ON DELETE SET NULL,
    FOREIGN KEY (sender_trainer_id) REFERENCES trainer(trainer_id) ON DELETE SET NULL
);

-- 16. Таблица планов тренировок (для клиентов)
CREATE TABLE training_plan (
    plan_id INT AUTO_INCREMENT PRIMARY KEY,
    client_id INT NOT NULL,
    trainer_id INT NOT NULL,
    plan_name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    exercises TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES client(client_id) ON DELETE CASCADE,
    FOREIGN KEY (trainer_id) REFERENCES trainer(trainer_id) ON DELETE CASCADE
);

-- 17. Таблица посещений (фиксация входа/выхода)
CREATE TABLE visit (
    visit_id INT AUTO_INCREMENT PRIMARY KEY,
    client_id INT NOT NULL,
    schedule_id INT NULL,
    entry_datetime DATETIME NOT NULL,
    exit_datetime DATETIME NULL,
    registered_by_admin_id INT NOT NULL,
    visit_type ENUM('обычный', 'гостевой') NOT NULL DEFAULT 'обычный',
    FOREIGN KEY (client_id) REFERENCES client(client_id) ON DELETE CASCADE,
    FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id) ON DELETE SET NULL,
    FOREIGN KEY (registered_by_admin_id) REFERENCES admin(admin_id)
);

-- 18. Таблица записей на тренировки
CREATE TABLE registration (
    registration_id INT AUTO_INCREMENT PRIMARY KEY,
    client_id INT NOT NULL,
    schedule_id INT NOT NULL,
    status ENUM('активна', 'отменена', 'завершена') NOT NULL,
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES client(client_id) ON DELETE CASCADE,
    FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id) ON DELETE CASCADE,
    UNIQUE KEY unique_registration (client_id, schedule_id)
);

-- 19. Таблица для хранения текущего количества посетителей в клубе (одна строка)
CREATE TABLE club_stats (
    id INT PRIMARY KEY DEFAULT 1,
    current_visitors INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT single_row CHECK (id = 1)
);

-- 20. Таблица связи тренера с зонами (в каких зонах может работать тренер)
CREATE TABLE trainer_zone (
    trainer_zone_id INT AUTO_INCREMENT PRIMARY KEY,
    trainer_id INT NOT NULL,
    zone_id INT NOT NULL,
    FOREIGN KEY (trainer_id) REFERENCES trainer(trainer_id) ON DELETE CASCADE,
    FOREIGN KEY (zone_id) REFERENCES zone(zone_id) ON DELETE CASCADE,
    UNIQUE KEY unique_trainer_zone (trainer_id, zone_id)
);

-- 21. Таблица периодов заморозки абонемента
CREATE TABLE membership_freeze (
    freeze_id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_id INT NOT NULL,
    freeze_start DATE NOT NULL,
    freeze_end DATE NOT NULL,
    freeze_days_used INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (purchase_id) REFERENCES membership_purchase(purchase_id) ON DELETE CASCADE
);

-- Инициализация счётчика посетителей
INSERT INTO club_stats (id, current_visitors) VALUES (1, 0) ON DUPLICATE KEY UPDATE id=1;




