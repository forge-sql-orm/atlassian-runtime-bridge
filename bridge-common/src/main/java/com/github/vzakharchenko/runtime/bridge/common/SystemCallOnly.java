package com.github.vzakharchenko.runtime.bridge.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Authorization opt-in: the bridge itself only authenticates callers; this annotation is a
 * ready-made authorization rule for endpoints that only Forge triggers may call.
 *
 * <p>Marks a controller (or a single handler method) as reachable only by <strong>system
 * calls</strong>: Forge invocations that carry no user, such as the {@code scheduledTrigger} /
 * {@code trigger} modules of the manifest.
 *
 * <pre>{@code
 * scheduledTrigger:
 *   - key: token-sync
 *     endpoint: system-token-sync
 * trigger:
 *   - key: event-install
 *     endpoint: system-token-sync
 * }</pre>
 *
 * <p>Works the same in {@code bridge-forge-connect} and {@code bridge-connect-container}. A request
 * is rejected with {@code AccessDeniedException} (HTTP 403) when it has no Forge authentication or
 * when it carries a user ({@code userId} / account id), see {@link SystemCallVerifier}. Where the
 * runtime exposes it (hybrid Forge Remote), a system token ({@code appSystemToken}) is required
 * too, see {@link SystemTokenPolicy}. This is an optional defense-in-depth check on top of the
 * Forge platform's own token validation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SystemCallOnly {}
