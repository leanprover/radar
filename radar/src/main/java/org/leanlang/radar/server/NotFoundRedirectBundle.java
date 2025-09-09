package org.leanlang.radar.server;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Environment;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;

public record NotFoundRedirectBundle(String servlet, String target, String name)
        implements ConfiguredBundle<Configuration> {

    @Override
    public void run(Configuration configuration, Environment environment) throws Exception {
        environment
                .servlets()
                .addFilter(name, new HttpFilter() {
                    @Override
                    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                            throws IOException, ServletException {

                        super.doFilter(req, res, chain);

                        if (req.getPathInfo().equals(target)) return; // No infinite loops if target does not exist, plz
                        if (res.getStatus() != HttpServletResponse.SC_NOT_FOUND) return;

                        // Try again, this time with our target instead of the original URL.
                        res.setStatus(HttpServletResponse.SC_OK); // Is not reset automatically
                        req.getRequestDispatcher(target).forward(req, res);
                    }
                })
                .addMappingForServletNames(EnumSet.allOf(DispatcherType.class), true, servlet);
    }
}
