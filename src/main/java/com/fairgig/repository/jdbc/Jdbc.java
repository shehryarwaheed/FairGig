package com.fairgig.repository.jdbc;

import com.fairgig.database.DatabaseConnection;
import com.fairgig.util.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tiny helper that removes the repeated JDBC boilerplate from the repositories.
 * EVERY statement goes through a PreparedStatement with ? placeholders (NFR-02),
 * values are never glued into the SQL text.
 * Methods without a Connection open (and close) their own; methods with a Connection
 * use the caller's, so they can be part of a transaction (NFR-06).
 */
final class Jdbc {

    /** Puts the values into the ? placeholders. */
    @FunctionalInterface
    interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    /** Turns the current row of a ResultSet into an object. */
    @FunctionalInterface
    interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    static final Binder NO_PARAMS = ps -> { };

    private Jdbc() {}

    // ---------------------------------------------------------------- queries

    static <T> List<T> queryList(String sql, Binder binder, RowMapper<T> mapper) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return queryList(connection, sql, binder, mapper);
        } catch (SQLException e) {
            throw new DataAccessException("Could not connect to the database.", e);
        }
    }

    static <T> List<T> queryList(Connection connection, String sql, Binder binder, RowMapper<T> mapper) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database error while reading data.", e);
        }
    }

    static <T> Optional<T> queryOne(String sql, Binder binder, RowMapper<T> mapper) {
        return queryList(sql, binder, mapper).stream().findFirst();
    }

    static <T> Optional<T> queryOne(Connection connection, String sql, Binder binder, RowMapper<T> mapper) {
        return queryList(connection, sql, binder, mapper).stream().findFirst();
    }

    /** Runs a SELECT count(*) style query that returns a single number. */
    static int queryInt(String sql, Binder binder) {
        return queryOne(sql, binder, rs -> rs.getInt(1)).orElse(0);
    }

    // ---------------------------------------------------------------- changes

    /** INSERT / UPDATE / DELETE. Returns the number of rows changed. */
    static int update(String sql, Binder binder) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return update(connection, sql, binder);
        } catch (SQLException e) {
            throw new DataAccessException("Could not connect to the database.", e);
        }
    }

    static int update(Connection connection, String sql, Binder binder) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Database error while saving data.", e);
        }
    }

    // ---------------------------------------------------------------- value helpers

    static void setInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    static Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    static LocalDate getDate(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDate.class);
    }

    static LocalDateTime getDateTime(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDateTime.class);
    }
}
