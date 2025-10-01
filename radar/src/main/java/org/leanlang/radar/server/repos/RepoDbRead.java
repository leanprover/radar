package org.leanlang.radar.server.repos;

import org.jooq.DSLContext;

public final class RepoDbRead {
    private final DSLContext dsl;

    public RepoDbRead(DSLContext dsl) {
        this.dsl = dsl;
    }

    public DSLContext dsl() {
        return dsl;
    }
}
