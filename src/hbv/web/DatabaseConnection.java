package hbv.web;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {

    private static HikariDataSource dataSource;

    static {
        String host     = System.getenv().getOrDefault("DB_HOST",     "mariadb");
        String port     = System.getenv().getOrDefault("DB_PORT",     "3306");
        String dbName   = System.getenv().getOrDefault("DB_NAME",     "impftermin");
        String user     = System.getenv().getOrDefault("DB_USER",     "impfuser");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "impfpassword");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + dbName);
        config.setUsername(user);
        config.setPassword(password);

        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(10_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        config.setValidationTimeout(5000);
        config.setLeakDetectionThreshold(2000);

        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void releaseConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException("Fehler beim Freigeben der Verbindung", e);
            }
        }
    }
}
