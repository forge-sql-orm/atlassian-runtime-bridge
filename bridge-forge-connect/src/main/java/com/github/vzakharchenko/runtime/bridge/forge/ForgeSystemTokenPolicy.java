package com.github.vzakharchenko.runtime.bridge.forge;

import com.atlassian.connect.spring.ForgeApiContext;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import com.github.vzakharchenko.runtime.bridge.common.SystemTokenPolicy;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.MalformedURLException;
import java.net.URI;
import java.text.ParseException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Hybrid runtime: the system token arrives in {@code x-forge-oauth-system} and is the app token. It
 * is an Atlassian OAuth access token (RS256 JWT, issuer {@code https://auth.atlassian.com})
 * obtained for the app's installation.
 *
 * <p><strong>Atlassian does not document how to validate this token</strong>, so these checks are
 * not an official contract and can be disabled. Atlassian treats OAuth tokens and their claims as
 * opaque and does not publish their shape. The checks below rely on the shape observed on real
 * Forge system tokens (issuer, JWKS location, and an {@code aud} that lists the installation ARI);
 * they may need adjusting, or disabling, if the token contents change. Every check except {@code
 * exp} has a property that turns it off. The Connect library itself only <em>reads</em> {@code exp}
 * of this token, so these checks are the only signature, issuer and installation-binding checks it
 * gets.
 *
 * <p>Checks:
 *
 * <ul>
 *   <li>always: the token is a JWT with an {@code exp} that has not passed (clock skew from {@code
 *       app.clockTolerance}, default 60s);
 *   <li>signature: RS256 against the JWKS, {@code bridge.forge.system-token.jwks-url} (default
 *       {@value #DEFAULT_JWKS_URL}; empty disables);
 *   <li>issuer: {@code bridge.forge.system-token.issuer} (default {@value #DEFAULT_ISSUER}; empty
 *       disables);
 *   <li>installation binding: {@code aud} contains the installation id of the invocation token —
 *       which the Connect library has already verified — so the token belongs to this installation;
 *       {@code bridge.forge.system-token.bind-installation} (default {@code true}; {@code false}
 *       disables).
 * </ul>
 */
@Component
public class ForgeSystemTokenPolicy implements SystemTokenPolicy {

  static final String DEFAULT_ISSUER = "https://auth.atlassian.com";
  static final String DEFAULT_JWKS_URL = "https://auth.atlassian.com/.well-known/jwks.json";
  private static final String INSTALLATION_ARI_PREFIX = "ari:cloud:ecosystem::installation/";

  private static final Logger LOGGER = LoggerFactory.getLogger(ForgeSystemTokenPolicy.class);

  private final DefaultJWTProcessor<SecurityContext> signedProcessor;
  private final DefaultJWTClaimsVerifier<SecurityContext> claimsVerifier;
  private final boolean bindInstallation;

  @Autowired
  public ForgeSystemTokenPolicy(
      @Value("${bridge.forge.system-token.jwks-url:" + DEFAULT_JWKS_URL + "}") String jwksUrl,
      @Value("${bridge.forge.system-token.issuer:" + DEFAULT_ISSUER + "}") String issuer,
      @Value("${bridge.forge.system-token.bind-installation:true}") boolean bindInstallation,
      @Value("${app.clockTolerance:60}") int clockToleranceSeconds)
      throws MalformedURLException {
    this(
        StringUtils.hasText(jwksUrl)
            ? JWKSourceBuilder.create(URI.create(jwksUrl).toURL()).build()
            : null,
        issuer,
        bindInstallation,
        clockToleranceSeconds);
  }

  ForgeSystemTokenPolicy(JWKSource<SecurityContext> jwkSource, String issuer, int clockSkew) {
    this(jwkSource, issuer, true, clockSkew);
  }

  ForgeSystemTokenPolicy(
      JWKSource<SecurityContext> jwkSource,
      String issuer,
      boolean bindInstallation,
      int clockSkew) {
    this.bindInstallation = bindInstallation;
    var exact = new JWTClaimsSet.Builder();
    if (StringUtils.hasText(issuer)) {
      exact.issuer(issuer);
    }
    this.claimsVerifier = new DefaultJWTClaimsVerifier<>(null, exact.build(), Set.of("exp"), null);
    this.claimsVerifier.setMaxClockSkew(clockSkew);
    if (jwkSource == null) {
      this.signedProcessor = null;
    } else {
      this.signedProcessor = new DefaultJWTProcessor<>();
      this.signedProcessor.setJWSKeySelector(
          new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));
      this.signedProcessor.setJWTClaimsSetVerifier(claimsVerifier);
    }
  }

  @Override
  public boolean hasSystemToken(ForgeAuthentication authentication) {
    var context = authentication.getDetails();
    var token = context == null ? null : context.getAppToken().orElse(null);
    if (!StringUtils.hasText(token)) {
      return false;
    }
    try {
      var claims = verify(token);
      if (bindInstallation && !belongsToInstallation(claims, installationId(context))) {
        throw new BadJWTException("token audience does not include the invocation installation");
      }
      return true;
    } catch (ParseException | JOSEException | BadJOSEException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("Forge system token rejected: {}", e.getMessage());
      }
      return false;
    }
  }

  private JWTClaimsSet verify(String token) throws ParseException, JOSEException, BadJOSEException {
    if (signedProcessor != null) {
      return signedProcessor.process(token, null);
    }
    var claims = JWTParser.parse(token).getJWTClaimsSet();
    claimsVerifier.verify(claims, null);
    return claims;
  }

  private static String installationId(ForgeApiContext context) {
    var invocationToken = context.getForgeInvocationToken();
    var app = invocationToken == null ? null : invocationToken.getApp();
    return app == null ? null : app.getInstallationId();
  }

  private static boolean belongsToInstallation(JWTClaimsSet claims, String installationId) {
    if (!StringUtils.hasText(installationId)) {
      return false;
    }
    var expected =
        installationId.startsWith("ari:")
            ? installationId
            : INSTALLATION_ARI_PREFIX + installationId;
    return claims.getAudience().contains(expected);
  }
}
