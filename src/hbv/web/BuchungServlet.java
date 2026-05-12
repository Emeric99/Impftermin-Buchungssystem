package hbv.web;

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
