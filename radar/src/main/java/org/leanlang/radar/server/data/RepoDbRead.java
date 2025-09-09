package org.leanlang.radar.server.data;

import org.jooq.DSLContext;

public class RepoDbRead {
    private final DSLContext dsl;

    public RepoDbRead(final DSLContext dsl) {
        this.dsl = dsl;
    }

    public DSLContext dsl() {
        return dsl;
    }
}
