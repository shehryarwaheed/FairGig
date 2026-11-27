
package com.fairgig.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

public final class DataSourceFactory {

    public static DataSource createFromEnv() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(System.getenv("DB_URL"));
        cfg.setUsername(System.getenv("DB_USER"));
        cfg.setPassword(System.getenv("DB_PASSWORD"));
        cfg.setMaximumPoolSize(10);
        return new HikariDataSource(cfg);
    }
}