package com.fairgig.database;

import java.sql.SQLException;

/**
 * "Runs this work as one transaction." Services take it as a constructor argument so unit tests
 * can hand in a fake that needs no database (the real one is {@link #standard()}).
 */
@FunctionalInterface
public interface Transactions {

    <T> T run(TransactionManager.Work<T> work) throws SQLException;

    /** The real thing: a PostgreSQL transaction that commits, or rolls back on any failure (NFR-06). */
    static Transactions standard() {
        return TransactionManager::inTransaction;
    }
}
