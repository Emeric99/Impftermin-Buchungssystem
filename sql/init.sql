-- =============================================================
-- init.sql – Impftermin-Buchungssystem
-- Datenbankschema für MariaDB
-- =============================================================

CREATE DATABASE IF NOT EXISTS impftermin;
USE impftermin;

-- --- Benutzer ---
CREATE TABLE IF NOT EXISTS users (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100)  NOT NULL,
    vorname         VARCHAR(100)  NOT NULL,
    `alter`         INT           NOT NULL,
    email           VARCHAR(150)  NOT NULL UNIQUE,
    passwort        VARCHAR(512)  NOT NULL,  -- Format: salt:hash (PBKDF2)
    benutzername    VARCHAR(100)  NOT NULL UNIQUE,
    adresse         VARCHAR(200)  NOT NULL,
    postleitzahl    VARCHAR(10)   NOT NULL,
    stadt           VARCHAR(100)  NOT NULL
);

-- --- Impfstoffe ---
CREATE TABLE IF NOT EXISTS vaccine (
    vaccine_id      INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100)  NOT NULL
);

-- --- Impfzentren ---
CREATE TABLE IF NOT EXISTS vaccination_center (
    center_id       INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(150)  NOT NULL,
    adresse         VARCHAR(200),
    cabins          INT           NOT NULL DEFAULT 5
);

-- --- Zeitslots ---
CREATE TABLE IF NOT EXISTS time_slot (
    time_slot_id    INT AUTO_INCREMENT PRIMARY KEY,
    slot_value      TIME          NOT NULL UNIQUE
);

-- --- Impfstoffvorrat pro Zentrum ---
CREATE TABLE IF NOT EXISTS center_vaccine (
    center_id       INT NOT NULL,
    vaccine_id      INT NOT NULL,
    fixed_quantity  INT NOT NULL DEFAULT 100,
    PRIMARY KEY (center_id, vaccine_id),
    FOREIGN KEY (center_id)  REFERENCES vaccination_center(center_id),
    FOREIGN KEY (vaccine_id) REFERENCES vaccine(vaccine_id)
);

-- --- Belegungsstatus pro Zentrum/Zeitslot/Datum ---
CREATE TABLE IF NOT EXISTS center_time_status (
    center_id       INT  NOT NULL,
    time_slot_id    INT  NOT NULL,
    date1           DATE NOT NULL,
    booked_count    INT  NOT NULL DEFAULT 0,
    PRIMARY KEY (center_id, time_slot_id, date1),
    FOREIGN KEY (center_id)     REFERENCES vaccination_center(center_id),
    FOREIGN KEY (time_slot_id)  REFERENCES time_slot(time_slot_id)
);

-- --- Gebuchte Termine ---
CREATE TABLE IF NOT EXISTS appointmentsApp (
    appointment_id      INT AUTO_INCREMENT PRIMARY KEY,
    center_id           INT          NOT NULL,
    user_id             INT          NOT NULL,
    date1               DATE         NOT NULL,
    time_slot_id        INT          NOT NULL,
    heure               TIME         NOT NULL,
    vaccine_id          INT          NOT NULL,
    qr_code_path        VARCHAR(300) DEFAULT '',
    familien_mitglied   VARCHAR(200) DEFAULT NULL,
    relation            VARCHAR(100) DEFAULT NULL,
    booking_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (center_id)     REFERENCES vaccination_center(center_id),
    FOREIGN KEY (user_id)       REFERENCES users(id),
    FOREIGN KEY (time_slot_id)  REFERENCES time_slot(time_slot_id),
    FOREIGN KEY (vaccine_id)    REFERENCES vaccine(vaccine_id)
);

-- =============================================================
-- Beispieldaten
-- =============================================================

-- Impfstoffe
INSERT INTO vaccine (name) VALUES
    ('BioNTech/Pfizer'),
    ('Moderna'),
    ('Johnson & Johnson'),
    ('AstraZeneca');

-- Zeitslots
INSERT INTO time_slot (slot_value) VALUES
    ('08:00:00'), ('08:30:00'), ('09:00:00'), ('09:30:00'),
    ('10:00:00'), ('10:30:00'), ('11:00:00'), ('11:30:00'),
    ('12:00:00'), ('12:30:00'), ('13:00:00'), ('13:30:00'),
    ('14:00:00'), ('14:30:00'), ('15:00:00'), ('15:30:00'),
    ('16:00:00'), ('16:30:00'), ('17:00:00'), ('17:30:00');

-- Impfzentren
INSERT INTO vaccination_center (name, adresse, cabins) VALUES
    ('Impfzentrum Bremerhaven', 'Hafenstraße 1, 27568 Bremerhaven', 5),
    ('Impfzentrum Bremen Mitte', 'Domshof 10, 28195 Bremen', 8),
    ('Impfzentrum Bremen Nord', 'Vegesacker Heerstraße 12, 28757 Bremen', 4);

-- Impfstoffvorrat pro Zentrum
INSERT INTO center_vaccine (center_id, vaccine_id, fixed_quantity) VALUES
    (1, 1, 200), (1, 2, 150), (1, 3, 100), (1, 4, 50),
    (2, 1, 300), (2, 2, 250), (2, 3, 200), (2, 4, 100),
    (3, 1, 150), (3, 2, 100), (3, 3, 80),  (3, 4, 40);
