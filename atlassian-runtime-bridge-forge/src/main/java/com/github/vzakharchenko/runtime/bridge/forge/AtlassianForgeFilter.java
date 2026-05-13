package com.github.vzakharchenko.runtime.bridge.forge;


import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that upgrades the current {@link ForgeAuthentication} so that
 * it carries a fully populated {@link com.atlassian.connect.spring.AtlassianHostUser}
 * built from the Forge invocation context.
 * <p>
 * The original {@link ForgeAuthentication} created by Connect only contains raw
 * Forge context details; this filter replaces it with a new instance whose
 * principal is the {@code AtlassianHostUser} resolved by
 * {@link AtlassianForgeMigrationService#getAtlassianHostUserFromContext()}.
 */
@Component
public class AtlassianForgeFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtlassianForgeFilter.class);
    private final AtlassianForgeMigrationService atlassianForgeMigrationService;

    public AtlassianForgeFilter(AtlassianForgeMigrationService atlassianForgeMigrationService) {
        this.atlassianForgeMigrationService = atlassianForgeMigrationService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof ForgeAuthentication forgeAuthentication) {
            try {
                atlassianForgeMigrationService.getAtlassianHostUserFromContext()
                        .ifPresentOrElse(user -> {
                            var ctx = forgeAuthentication.getDetails();
                            var auth = new ForgeAuthentication(ctx, user);
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }, () -> LOGGER.error("No AtlassianHostUser found for the provided Forge invocation token"));

            } catch (Exception e) {
                LOGGER.error("Failed to set up Forge authentication", e);
            }
        }
        filterChain.doFilter(request, response);
    }
}

