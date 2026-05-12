package hbv.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class BuchungServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        // Session aus Redis lesen
        String sessionId = JedisAdapter.getSessionIdFromCookies(request.getCookies());
        Map<String, String> session = JedisAdapter.getSession(sessionId);

        if (session == null) {
            response.sendRedirect("login.html");
            return;
        }

        int userId;
        try {
            userId = Integer.parseInt(session.get("user_id"));
        } catch (NumberFormatException e) {
            response.sendRedirect("login.html");
            return;
        }

        String emailAddress = session.get("email");

        // Maximale Terminanzahl prüfen (4 Termine pro Benutzer)
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement psCount = con.prepareStatement(
                     "SELECT COUNT(*) FROM appointmentsApp WHERE user_id = ?")) {
            psCount.setInt(1, userId);
            try (ResultSet rsCount = psCount.executeQuery()) {
                if (rsCount.next() && rsCount.getInt(1) >= 4) {
                    PrintWriter out = response.getWriter();
                    out.println("<html><head><script>");
                    out.println("alert('Sie können nicht mehr als 4 Termine buchen.');");
                    out.println("setTimeout(function(){ window.location.href='zentren.html'; }, 3000);");
                    out.println("</script></head><body></body></html>");
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.getWriter().println("<h2>Datenbankfehler: " + e.getMessage() + "</h2>");
            return;
        }

        // Formulardaten lesen
        String centerName      = request.getParameter("center");
        String date1           = request.getParameter("date1");
        String heureStr        = request.getParameter("heure1");
        String vaccineValue    = request.getParameter("vaccine");
        String familienMitglied = request.getParameter("familien_mitglied");
        String relation        = request.getParameter("relation");

        if (centerName == null || centerName.isEmpty() ||
                date1 == null || date1.isEmpty() ||
                heureStr == null || heureStr.isEmpty() ||
                vaccineValue == null || vaccineValue.isEmpty()) {
            response.getWriter().println("<h2>Bitte alle Pflichtfelder ausfüllen.</h2>");
            return;
        }

        // Datum validieren
        LocalDate chosenDate;
        try {
            chosenDate = LocalDate.parse(date1);
            if (chosenDate.isBefore(LocalDate.now())) {
                response.getWriter().println("<h2>Das ausgewählte Datum liegt in der Vergangenheit.</h2>");
                return;
            }
        } catch (DateTimeParseException e) {
            response.getWriter().println("<h2>Ungültiges Datumsformat.</h2>");
            return;
        }

        try (Connection con = DatabaseConnection.getConnection()) {

            // Zentrum abrufen
            int centerId = 0, cabins = 0;
            try (PreparedStatement psCenter = con.prepareStatement(
                    "SELECT center_id, cabins FROM vaccination_center WHERE name = ?")) {
                psCenter.setString(1, centerName);
                try (ResultSet rsCenter = psCenter.executeQuery()) {
                    if (rsCenter.next()) {
                        centerId = rsCenter.getInt("center_id");
                        cabins   = rsCenter.getInt("cabins");
                    } else {
                        response.getWriter().println("<h2>Zentrum nicht gefunden.</h2>");
                        return;
                    }
                }
            }

            // Zeitslot abrufen
            String timeValue = heureStr;
            if (timeValue.split(":").length == 2) timeValue += ":00";

            int timeSlotId = 0;
            try (PreparedStatement psTime = con.prepareStatement(
                    "SELECT time_slot_id FROM time_slot WHERE slot_value = ?")) {
                psTime.setString(1, timeValue);
                try (ResultSet rsTime = psTime.executeQuery()) {
                    if (rsTime.next()) {
                        timeSlotId = rsTime.getInt("time_slot_id");
                    } else {
                        response.getWriter().println("<h2>Uhrzeit nicht gefunden.</h2>");
                        return;
                    }
                }
            }

            int vaccineId = Integer.parseInt(vaccineValue);

            // Verfügbarkeit Zeitslot prüfen
            int bookedCount = 0;
            try (PreparedStatement psStatus = con.prepareStatement(
                    "SELECT booked_count FROM center_time_status WHERE center_id = ? AND time_slot_id = ? AND date1 = ?")) {
                psStatus.setInt(1, centerId);
                psStatus.setInt(2, timeSlotId);
                psStatus.setString(3, date1);
                try (ResultSet rsStatus = psStatus.executeQuery()) {
                    if (rsStatus.next()) bookedCount = rsStatus.getInt("booked_count");
                }
            }
            if (bookedCount >= cabins) {
                PrintWriter out = response.getWriter();
                out.println("<html><head><script>");
                out.println("alert('Die gewählte Uhrzeit ist nicht verfügbar.');");
                out.println("setTimeout(function(){ window.location.href='zentren.html'; }, 3000);");
                out.println("</script></head><body></body></html>");
                return;
            }

            // Impfstoffverfügbarkeit prüfen
            try (PreparedStatement psVaccine = con.prepareStatement(
                    "SELECT fixed_quantity FROM center_vaccine WHERE center_id = ? AND vaccine_id = ?")) {
                psVaccine.setInt(1, centerId);
                psVaccine.setInt(2, vaccineId);
                try (ResultSet rsVaccine = psVaccine.executeQuery()) {
                    if (!rsVaccine.next() || rsVaccine.getInt("fixed_quantity") <= 0) {
                        response.getWriter().println("<h2>Der gewählte Impfstoff ist nicht verfügbar.</h2>");
                        return;
                    }
                }
            }

            // Termin speichern
            try (PreparedStatement psInsert = con.prepareStatement(
                    "INSERT INTO appointmentsApp (center_id, user_id, date1, time_slot_id, heure, vaccine_id, qr_code_path, familien_mitglied, relation) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                psInsert.setInt(1, centerId);
                psInsert.setInt(2, userId);
                psInsert.setString(3, date1);
                psInsert.setInt(4, timeSlotId);
                psInsert.setString(5, timeValue);
                psInsert.setInt(6, vaccineId);
                psInsert.setString(7, "");
                psInsert.setString(8, familienMitglied);
                psInsert.setString(9, relation);

                if (psInsert.executeUpdate() > 0) {
                    // center_time_status aktualisieren
                    try (PreparedStatement psStatusSelect = con.prepareStatement(
                            "SELECT booked_count FROM center_time_status WHERE center_id = ? AND time_slot_id = ? AND date1 = ?")) {
                        psStatusSelect.setInt(1, centerId);
                        psStatusSelect.setInt(2, timeSlotId);
                        psStatusSelect.setString(3, date1);
                        try (ResultSet rsStatusSelect = psStatusSelect.executeQuery()) {
                            if (rsStatusSelect.next()) {
                                try (PreparedStatement psUpdate = con.prepareStatement(
                                        "UPDATE center_time_status SET booked_count = booked_count + 1 WHERE center_id = ? AND time_slot_id = ? AND date1 = ?")) {
                                    psUpdate.setInt(1, centerId);
                                    psUpdate.setInt(2, timeSlotId);
                                    psUpdate.setString(3, date1);
                                    psUpdate.executeUpdate();
                                }
                            } else {
                                try (PreparedStatement psInsertStatus = con.prepareStatement(
                                        "INSERT INTO center_time_status (center_id, time_slot_id, date1, booked_count) VALUES (?, ?, ?, ?)")) {
                                    psInsertStatus.setInt(1, centerId);
                                    psInsertStatus.setInt(2, timeSlotId);
                                    psInsertStatus.setString(3, date1);
                                    psInsertStatus.setInt(4, 1);
                                    psInsertStatus.executeUpdate();
                                }
                            }
                        }
                    }

                    // Impfstoffvorrat reduzieren
                    try (PreparedStatement psUpdateVaccine = con.prepareStatement(
                            "UPDATE center_vaccine SET fixed_quantity = fixed_quantity - 1 WHERE center_id = ? AND vaccine_id = ?")) {
                        psUpdateVaccine.setInt(1, centerId);
                        psUpdateVaccine.setInt(2, vaccineId);
                        psUpdateVaccine.executeUpdate();
                    }

                    // E-Mail senden
                    try (PreparedStatement stmtGetId = con.prepareStatement(
                            "SELECT appointment_id FROM appointmentsApp WHERE user_id = ? ORDER BY booking_time DESC LIMIT 1")) {
                        stmtGetId.setInt(1, userId);
                        try (ResultSet rs = stmtGetId.executeQuery()) {
                            if (rs.next()) {
                                SendMail mailSender = new SendMail();
                                mailSender.sendConfirmation(
                                        String.valueOf(rs.getInt("appointment_id")),
                                        centerName, date1, timeValue, vaccineValue,
                                        String.valueOf(userId));
                            }
                        }
                    }

                    response.sendRedirect("termine.html");
                } else {
                    response.getWriter().println("<h2>Termin konnte nicht gespeichert werden.</h2>");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.getWriter().println("<h2>Datenbankfehler: " + e.getMessage() + "</h2>");
        }
    }
}package hbv.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Map;

public class TerminA extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        // Session aus Redis lesen
        String sessionId = JedisAdapter.getSessionIdFromCookies(request.getCookies());
        Map<String, String> session = JedisAdapter.getSession(sessionId);

        if (session == null) {
            response.sendRedirect("login.html");
            return;
        }

        int userId;
        try {
            userId = Integer.parseInt(session.get("user_id"));
        } catch (NumberFormatException e) {
            response.sendRedirect("login.html");
            return;
        }

        String sql = "SELECT a.appointment_id, a.center_id, vc.name AS center_name, a.date1, a.heure, " +
                "a.vaccine_id, v.name AS vaccine_name, a.time_slot_id " +
                "FROM appointmentsApp a " +
                "JOIN vaccination_center vc ON a.center_id = vc.center_id " +
                "JOIN vaccine v ON a.vaccine_id = v.vaccine_id " +
                "WHERE a.user_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                PrintWriter out = response.getWriter();
                out.println("<html><head><title>Meine Termine</title>");
                out.println("<link rel='stylesheet' type='text/css' href='script.css'>");
                out.println("<style>");
                out.println("table { border-collapse: collapse; width: 100%; font-family: Arial, sans-serif; font-size: 14px; }");
                out.println("th, td { text-align: left; padding: 8px; border-bottom: 1px solid #ddd; }");
                out.println("th { background-color: #008CBA; color: white; }");
                out.println("tr:nth-child(even) { background-color: #f2f2f2; }");
                out.println("</style></head><body>");
                out.println("<h1>Meine Termine</h1>");
                out.println("<table><tr><th>Zentrum</th><th>Datum</th><th>Uhrzeit</th><th>Impfstoff</th><th>Termin stornieren</th></tr>");

                while (rs.next()) {
                    int appointmentId = rs.getInt("appointment_id");
                    int centerId      = rs.getInt("center_id");
                    String centerName = rs.getString("center_name");
                    String dateStr    = rs.getString("date1");
                    String heure      = rs.getString("heure");
                    int vaccineId     = rs.getInt("vaccine_id");
                    String vaccineName = rs.getString("vaccine_name");
                    int timeSlotId    = rs.getInt("time_slot_id");

                    out.println("<tr id='appointment_" + appointmentId + "' " +
                            "data-center_id='" + centerId + "' " +
                            "data-date1='" + dateStr + "' " +
                            "data-time_slot_id='" + timeSlotId + "' " +
                            "data-vaccine_id='" + vaccineId + "'>");
                    out.println("<td>" + centerName + "</td>");
                    out.println("<td>" + dateStr + "</td>");
                    out.println("<td>" + heure + "</td>");
                    out.println("<td>" + vaccineName + "</td>");
                    out.println("<td><button onclick=\"deleteAppointment(" + appointmentId +
                            ", document.getElementById('appointment_" + appointmentId + "'))\">Stornieren</button></td>");
                    out.println("</tr>");
                }

                out.println("</table></body></html>");
            }
            DatabaseConnection.releaseConnection(con);
        } catch (SQLException e) {
            e.printStackTrace();
            response.getWriter().println("<p>Datenbankfehler: " + e.getMessage() + "</p>");
        }
    }
}
