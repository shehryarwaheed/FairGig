package com.fairgig.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Runs several database steps as ONE transaction: either all of them succeed,
 * or none of them is saved (NFR-06). Used by the CSV import and any multi-step update.
 *
 * <pre>
 * int saved = TransactionManager.inTransaction(connection -> {
 *     // use "connection" for every statement of this unit of work
 *     return 42;
 * });
 * </pre>
 */
public final class TransactionManager {

    /** One unit of work that runs inside a transaction. */
    @FunctionalInterface
    public interface Work<T> {
        T execute(Connection connection) throws SQLException;
    }

    private TransactionManager() {}

    public static <T> T inTransaction(Work<T> work) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return inTransaction(connection, work);
        }
    }

    /** Same, but on a connection the caller already opened (it is not closed here). */
    public static <T> T inTransaction(Connection connection, Work<T> work) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T result = work.execute(connection);
            connection.commit();
            return result;
        } catch (SQLException | RuntimeException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackFailure) {
                e.addSuppressed(rollbackFailure);
            }
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }
}
