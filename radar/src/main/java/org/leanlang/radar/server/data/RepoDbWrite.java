package org.leanlang.radar.server.data;

import java.util.concurrent.locks.Lock;
import org.jooq.DSLContext;

public class RepoDbWrite implements AutoCloseable {
    private final DSLContext dsl;
    private final Lock lock;

    public RepoDbWrite(DSLContext dsl, Lock lock) {
        this.dsl = dsl;
        this.lock = lock;
        this.lock.lock();
    }

    @Override
    public void close() {
        lock.unlock();
    }

    public DSLContext dsl() {
        return dsl;
    }
}
