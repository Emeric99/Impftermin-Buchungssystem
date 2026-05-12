package hbv.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Map;

public class DeleteT extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        // Session aus Redis lesen
        String sessionId = JedisAdapter.getSessionIdFromCookies(request.getCookies());
        Map<String, String> session = JedisAdapter.getSession(sessionId);

        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("{\"status\":\"error\",\"message\":\"Nicht angemeldet.\"}");
            return;
        }

        int userId;
        try {
            userId = Integer.parseInt(session.get("user_id"));
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("{\"status\":\"error\",\"message\":\"Benutzer-ID nicht gefunden.\"}");
            return;
        }

        // Termin-ID lesen
        String idStr = request.getParameter("id");
        if (idStr == null || idStr.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("{\"status\":\"error\",\"message\":\"Keine Termin-ID angegeben.\"}");
            return;
        }

        int appointmentId;
        try {
            appointmentId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("{\"status\":\"error\",\"message\":\"Ungültige Termin-ID.\"}");
            return;
        }

        String centerIdStr   = request.getParameter("center_id");
        String date1         = request.getParameter("date1");
        String timeSlotIdStr = request.getParameter("time_slot_id");
        String vaccineIdStr  = request.getParameter("vaccine_id");

        if (centerIdStr == null || date1 == null || timeSlotIdStr == null || vaccineIdStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("{\"status\":\"error\",\"message\":\"Erforderliche Parameter fehlen.\"}");
            return;
        }

        int centerId, timeSlotId, vaccineId;
        try {
            centerId    = Integer.parseInt(centerIdStr);
            timeSlotId  = Integer.parseInt(timeSlotIdStr);
            vaccineId   = Integer.parseInt(vaccineIdStr);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("{\"status\":\"error\",\"message\":\"Ungültige Parameterwerte.\"}");
            return;
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            // Termin löschen
            try (PreparedStatement stmt = con.prepareStatement(
                    "DELETE FROM appointmentsApp WHERE appointment_id = ? AND user_id = ?")) {
                stmt.setInt(1, appointmentId);
                stmt.setInt(2, userId);
                int rowsAffected = stmt.executeUpdate();

                if (rowsAffected > 0) {
                    // booked_count dekrementieren
                    try (PreparedStatement psStatus = con.prepareStatement(
                            "UPDATE center_time_status SET booked_count = booked_count - 1 " +
                                    "WHERE center_id = ? AND time_slot_id = ? AND date1 = ?")) {
                        psStatus.setInt(1, centerId);
                        psStatus.setInt(2, timeSlotId);
                        psStatus.setString(3, date1);
                        psStatus.executeUpdate();
                    }

                    // Impfstoffvorrat erhöhen
                    try (PreparedStatement psVaccine = con.prepareStatement(
                            "UPDATE center_vaccine SET fixed_quantity = fixed_quantity + 1 " +
                                    "WHERE center_id = ? AND vaccine_id = ?")) {
                        psVaccine.setInt(1, centerId);
                        psVaccine.setInt(2, vaccineId);
                        psVaccine.executeUpdate();
                    }

                    response.getWriter().println("{\"status\":\"success\",\"message\":\"Termin erfolgreich gelöscht.\"}");
                } else {
                    response.getWriter().println("{\"status\":\"error\",\"message\":\"Termin konnte nicht gelöscht werden.\"}");
                }
            }
            DatabaseConnection.releaseConnection(con);
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("{\"status\":\"error\",\"message\":\"Datenbankfehler: " + e.getMessage() + "\"}");
        }
    }
}
