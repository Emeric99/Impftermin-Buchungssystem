package hbv.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html; charset=UTF-8");

        String benutzername = request.getParameter("benutzername");
        String passwort     = request.getParameter("passwort");

        if (benutzername == null || benutzername.isEmpty() ||
                passwort == null || passwort.isEmpty()) {
            response.getWriter().println("<h3>Bitte füllen Sie alle Felder aus.</h3>");
            return;
        }

        // Admin-Login
        if ("Admin".equals(benutzername) && "Adminpwd123".equals(passwort)) {
            response.sendRedirect("admin.html");
            return;
        }

        String sql = "SELECT id, name, vorname, `alter`, email, passwort, benutzername, adresse, postleitzahl, stadt " +
                "FROM users WHERE benutzername = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, benutzername);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("passwort");
                    String[] parts = storedHash.split(":");
                    if (parts.length != 2) {
                        response.getWriter().println("<h3>Interner Fehler: Ungültiges Passwortformat.</h3>");
                        return;
                    }

                    byte[] salt = HexFormat.of().parseHex(parts[0]);
                    byte[] hash = HexFormat.of().parseHex(parts[1]);
                    byte[] providedHash = hashPassword(passwort, salt);

                    if (compareHashes(hash, providedHash)) {
                        // Session-Daten in Redis speichern
                        Map<String, String> sessionData = new HashMap<>();
                        sessionData.put("user_id",      String.valueOf(rs.getInt("id")));
                        sessionData.put("benutzername", rs.getString("benutzername"));
                        sessionData.put("name",         rs.getString("name"));
                        sessionData.put("vorname",      rs.getString("vorname"));
                        sessionData.put("email",        rs.getString("email"));
                        sessionData.put("adresse",      rs.getString("adresse"));
                        sessionData.put("postleitzahl", rs.getString("postleitzahl"));
                        sessionData.put("stadt",        rs.getString("stadt"));

                        String sessionId = JedisAdapter.createSession(sessionData);

                        // Session-ID als Cookie setzen
                        Cookie cookie = new Cookie("REDIS_SESSION", sessionId);
                        cookie.setHttpOnly(true);
                        cookie.setPath(request.getContextPath());
                        cookie.setMaxAge(1800); // 30 Minuten
                        response.addCookie(cookie);

                        response.sendRedirect("willkommen.html");
                    } else {
                        response.getWriter().println("<h3>Falscher Benutzername oder Passwort.</h3>");
                    }
                } else {
                    response.getWriter().println("<h3>Falscher Benutzername oder Passwort.</h3>");
                }
            }
        } catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            response.getWriter().println("<h3>Interner Fehler: " + e.getMessage() + "</h3>");
            e.printStackTrace();
        }
    }

    private boolean compareHashes(byte[] hash1, byte[] hash2) {
        if (hash1.length != hash2.length) return false;
        for (int i = 0; i < hash1.length; i++) {
            if (hash1[i] != hash2[i]) return false;
        }
        return true;
    }

    private static byte[] hashPassword(String passwort, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        PBEKeySpec spec = new PBEKeySpec(passwort.toCharArray(), salt, 210000, 256);
        SecretKey key = factory.generateSecret(spec);
        spec.clearPassword();
        return key.getEncoded();
    }
}
