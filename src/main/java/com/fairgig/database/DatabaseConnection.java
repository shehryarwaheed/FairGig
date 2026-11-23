package com.fairgig.database;

import com.fairgig.util.Env;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * The only place that opens JDBC connections to PostgreSQL (NFR-14).
 * Settings come from the git-ignored ".env" file: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD.
 */
public final class DatabaseConnection {

    private DatabaseConnection() {}

    /** JDBC URL, e.g. jdbc:postgresql://localhost:5432/fairgig_db (never contains the password). */
    public static String url() {
        String host = Env.get("DB_HOST", "localhost");
        String port = Env.get("DB_PORT", "5432");
        String name = Env.get("DB_NAME", "fairgig_db");
        return "jdbc:postgresql://" + host + ":" + port + "/" + name;
    }

    /** Opens a new connection. The caller must close it (use try-with-resources). */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            url(),
            Env.get("DB_USER", "postgres"),
            Env.get("DB_PASSWORD", ""));
    }

    /** True when the database can be reached with the current settings. */
    public static boolean isAvailable() {
        try (Connection c = getConnection()) {
            return c.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }
}
