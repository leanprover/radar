package org.leanlang.radar.server.api.auth;

import java.security.Principal;

public final class Admin implements Principal {
    @Override
    public String getName() {
        return "admin";
    }
}
