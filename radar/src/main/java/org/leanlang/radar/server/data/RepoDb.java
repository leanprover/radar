package org.leanlang.radar.server.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.TransactionalCallable;
import org.jooq.TransactionalRunnable;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

public final class RepoDb implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(RepoDb.class);

    private final String name;
    private final HikariDataSource hikariDataSource;
    private final DSLContext dslContext;
    private final Lock writeLock = new ReentrantLock();

    public RepoDb(String name, Path path) throws IOException {
        log.info("Opening DB for {}", name);
        this.name = name;

        // Configure DB connection
        String jdbcUrl = "jdbc:sqlite:file:" + path.toAbsolutePath();
        SQLiteDataSource sqLiteDataSource = new SQLiteDataSource(buildSqliteConfig());
        sqLiteDataSource.setUrl(jdbcUrl);

        // Configure DB connection pool
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSource(sqLiteDataSource);
        hikariConfig.setPoolName("db-pool-" + name);

        // Create and migrate DB file
        Files.createDirectories(path.getParent());
        Flyway.configure().dataSource(sqLiteDataSource).load().migrate();

        // Connect to DB
        this.hikariDataSource = new HikariDataSource(hikariConfig);
        this.dslContext = DSL.using(hikariDataSource, SQLDialect.SQLITE);
    }

    static SQLiteConfig buildSqliteConfig() {
        // See also https://briandouglas.ie/sqlite-defaults/
        SQLiteConfig sqliteConfig = new SQLiteConfig();

        // TODO Open PR for these?
        // No auto_vacuum, see https://github.com/xerial/sqlite-jdbc/issues/580
        // No trusted_schema = false, see https://github.com/xerial/sqlite-jdbc/issues/891

        // https://www.sqlite.org/pragma.html#pragma_foreign_keys
        sqliteConfig.enforceForeignKeys(true);

        // https://www.sqlite.org/pragma.html#pragma_journal_mode
        sqliteConfig.setJournalMode(SQLiteConfig.JournalMode.WAL);

        // https://www.sqlite.org/pragma.html#pragma_synchronous
        // "WAL mode is safe from corruption with synchronous=NORMAL"
        // "WAL mode is always consistent with synchronous=NORMAL
        sqliteConfig.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);

        // https://www.sqlite.org/pragma.html#pragma_cache_size
        // When negative, specified in kibibytes
        sqliteConfig.setCacheSize(-32 * 1024); // 32 MiB

        // https://www.sqlite.org/pragma.html#pragma_journal_size_limit
        // Otherwise the WAL file can grow pretty large
        sqliteConfig.setJournalSizeLimit(32 * 1024 * 1024); // 32 MiB

        return sqliteConfig;
    }

    @Override
    public void close() {
        log.info("Closing DB for {}", name);
        hikariDataSource.close();
    }

    public RepoDbRead read() {
        return new RepoDbRead(dslContext);
    }

    public void readTransaction(TransactionalRunnable transaction) {
        dslContext.transaction(transaction);
    }

    public <T> T readTransactionResult(TransactionalCallable<T> transaction) {
        return dslContext.transactionResult(transaction);
    }

    public RepoDbWrite write() {
        return new RepoDbWrite(dslContext, writeLock);
    }

    public void writeTransaction(TransactionalRunnable transaction) {
        try (var write = write()) {
            write.dsl().transaction(transaction);
        }
    }

    public <T> T writeTransactionResult(TransactionalCallable<T> transaction) {
        try (var write = write()) {
            return write.dsl().transactionResult(transaction);
        }
    }
}
