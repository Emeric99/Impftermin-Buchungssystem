package hbv.web;

import jakarta.servlet.*;

public class MyContextListener implements ServletContextListener {

    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();

        String redisServer   = ctx.getInitParameter("redisserver");
        String redisPassword = ctx.getInitParameter("redispassword");

        if (redisServer == null || redisServer.isEmpty()) {
            redisServer = "redis"; // Docker-Service-Name als Fallback
        }

        JedisAdapter.init(redisServer, 6379, redisPassword);
        ctx.log("Redis-Verbindung initialisiert: " + redisServer + ":6379");
    }

    public void contextDestroyed(ServletContextEvent sce) {
        JedisAdapter.destroy();
        sce.getServletContext().log("Redis-Verbindung geschlossen.");
    }
}
