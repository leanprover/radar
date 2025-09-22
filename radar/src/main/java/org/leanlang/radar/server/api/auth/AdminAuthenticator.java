package org.leanlang.radar.server.api.auth;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import java.util.Optional;

public record AdminAuthenticator(String token) implements Authenticator<BasicCredentials, Admin> {
    @Override
    public Optional<Admin> authenticate(BasicCredentials basicCredentials) throws AuthenticationException {
        String username = basicCredentials.getUsername();
        String password = basicCredentials.getPassword();

        if (username.equals("admin") && password.equals(token)) {
            return Optional.of(new Admin());
        }

        return Optional.empty();
    }
}
