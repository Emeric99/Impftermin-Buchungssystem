package hbv.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class LogoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Session aus Redis löschen
        String sessionId = JedisAdapter.getSessionIdFromCookies(request.getCookies());
        if (sessionId != null) {
            JedisAdapter.deleteSession(sessionId);
        }

        // Cookie löschen
        Cookie cookie = new Cookie("REDIS_SESSION", "");
        cookie.setMaxAge(0);
        cookie.setPath(request.getContextPath());
        response.addCookie(cookie);

        response.sendRedirect("login.html");
    }
}
