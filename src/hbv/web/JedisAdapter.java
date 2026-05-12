package hbv.web;

import redis.clients.jedis.*;

import java.util.Map;
import java.util.UUID;

public class JedisAdapter {

    private static JedisPool jedisPool;
    private static final int SESSION_TTL = 1800; // 30 Minuten

    public static void init(String host, int port, String password) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(300);
        poolConfig.setMaxIdle(100);
        poolConfig.setMinIdle(10);
        if (password != null && !password.isEmpty()) {
            jedisPool = new JedisPool(poolConfig, host, port, 2000, password);
        } else {
            jedisPool = new JedisPool(poolConfig, host, port);
        }
    }

    public static void destroy() {
        if (jedisPool != null) jedisPool.close();
    }

    public static Jedis getJedis() {
        return jedisPool.getResource();
    }

    public static void releaseJedis(Jedis jedis) {
        if (jedis != null) jedis.close();
    }

    // --- Session Management ---

    // Neue Session erstellen und Session-ID zurückgeben
    public static String createSession(Map<String, String> data) {
        String sessionId = UUID.randomUUID().toString();
        try (Jedis jedis = getJedis()) {
            jedis.hset("session:" + sessionId, data);
            jedis.expire("session:" + sessionId, SESSION_TTL);
        }
        return sessionId;
    }

    // Session-Daten abrufen
    public static Map<String, String> getSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) return null;
        try (Jedis jedis = getJedis()) {
            Map<String, String> data = jedis.hgetAll("session:" + sessionId);
            if (data == null || data.isEmpty()) return null;
            // TTL erneuern bei Aktivität
            jedis.expire("session:" + sessionId, SESSION_TTL);
            return data;
        }
    }

    // Session löschen (Logout)
    public static void deleteSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) return;
        try (Jedis jedis = getJedis()) {
            jedis.del("session:" + sessionId);
        }
    }

    // Session-ID aus Cookie lesen
    public static String getSessionIdFromCookies(jakarta.servlet.http.Cookie[] cookies) {
        if (cookies == null) return null;
        for (jakarta.servlet.http.Cookie cookie : cookies) {
            if ("REDIS_SESSION".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
