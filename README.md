# Impftermin-Buchungssystem

Eine webbasierte Anwendung zur Verwaltung und Buchung von Impfterminen, entwickelt im Rahmen des Moduls **Softwareentwicklung 3** an der Hochschule Bremerhaven.

---

## Funktionen

- **Registrierung & Login** – Benutzerregistrierung mit sicherer Passwortverschlüsselung (PBKDF2 + Salt)
- **Terminbuchung** – Auswahl von Impfzentrum, Datum, Uhrzeit und Impfstoff
- **Verfügbarkeitsprüfung** – Echtzeit-Prüfung freier Zeitslots und Impfstoffvorräte
- **Familienbuchung** – Termine für Familienmitglieder mitbuchen
- **E-Mail-Bestätigung** – Automatischer Versand einer Bestätigungsmail nach erfolgreicher Buchung
- **Terminübersicht** – Eigene gebuchte Termine einsehen und verwalten
- **Sitzungsverwaltung** – Sichere Sessions mit automatischem Logout

---

## Tech-Stack

| Bereich | Technologie |
|--------|-------------|
| Backend | Java (Jakarta EE), HTTP-Servlets |
| Datenbank | MariaDB + HikariCP (Connection Pooling) |
| Session-Management | Redis (Jedis) |
| Passwort-Hashing | PBKDF2WithHmacSHA512 + SecureRandom Salt |
| E-Mail | Jakarta Mail (SMTP) |
| Frontend | HTML5, CSS3, JavaScript |
| Server | Apache Tomcat |
| Deployment | Docker |

---

## Projektstruktur

```
Impftermin-Buchungssystem/
├── src/hbv/web/
│   ├── RegisterServlet.java      # Registrierung mit PBKDF2-Hashing
│   ├── LoginServlet.java         # Login mit Redis Session-Management
│   ├── LogoutServlet.java        # Session-Invalidierung
│   ├── BuchungServlet.java       # Terminbuchung & Verfügbarkeitsprüfung
│   ├── TerminA.java              # Terminanzeige
│   ├── DeleteT.java              # Terminlöschung
│   ├── DatabaseConnection.java   # MariaDB-Verbindung via HikariCP
│   ├── SendMail.java             # E-Mail-Bestätigung via Jakarta Mail
│   ├── JedisAdapter.java         # Redis Session-Management
│   └── MyContextListener.java    # Redis-Initialisierung beim Start
├── webapp/
│   ├── WEB-INF/web.xml           # Servlet-Konfiguration
│   ├── META-INF/context.xml      # Datenbankverbindung (Tomcat)
│   ├── login.html
│   ├── register.html
│   ├── termine.html
│   ├── zentren.html
│   ├── willkommen.html
│   ├── admin.html
│   ├── script.css
│   └── script.js
├── sql/
│   └── init.sql                  # Datenbankschema + Beispieldaten
├── docker/
│   └── context.xml               # DB-Konfiguration für Docker
├── docker-compose.yml            # Startet MariaDB, Redis, Mailhog & Tomcat
└── pom.xml                       # Maven Build-Konfiguration
```
---

## Sicherheitskonzept

- Passwörter werden **niemals im Klartext** gespeichert
- **PBKDF2WithHmacSHA512** mit 210.000 Iterationen und zufälligem Salt
- **PreparedStatements** für alle SQL-Abfragen (SQL-Injection-Schutz)
- **Session-Validierung** bei jedem geschützten Endpunkt
- **Redis** für skalierbare Session-Verwaltung

---

## Lokale Ausführung

### Voraussetzungen

### Voraussetzungen

- Docker & Docker Compose
  
### Mit Docker starten

```bash
# 1. Repository klonen
git clone https://github.com/Emeric99/Impftermin-Buchungssystem.git
cd Impftermin-Buchungssystem

# 2. Projekt kompilieren
docker compose --profile build run build

# 3. Anwendung starten
docker compose up
```

Anschließend im Browser öffnen:  
 **http://localhost:8080/impftermin/login.html**
E-Mail-Vorschau (Mailhog): **http://localhost:8025**



---

## Was ich dabei gelernt habe

- Entwicklung einer mehrschichtigen Java-Webanwendung mit Jakarta EE
- Sichere Passwortspeicherung mit PBKDF2 und kryptografischem Salt
- Datenbankanbindung mit MariaDB und HikariCP Connection Pool
- Session-Management mit Redis (Jedis)
- Schutz gegen SQL-Injection mit PreparedStatements
- Deployment mit Apache Tomcat in einer Docker-Umgebung
- Validierung von Benutzereingaben auf Server-Seite

---

*Modul: Softwareentwicklung 3 · Hochschule Bremerhaven · 2025*
