package com.github.vzakharchenko.runtime.bridge.forge;

import static org.assertj.core.api.Assertions.assertThat;

import com.atlassian.connect.spring.ForgeApiContext;
import com.atlassian.connect.spring.ForgeApp;
import com.atlassian.connect.spring.ForgeInvocationToken;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ForgeSystemTokenPolicyTest {

  private static final String ISSUER = "https://auth.atlassian.com";
  private static final String INSTALLATION = "ari:cloud:ecosystem::installation/inst-1";

  private static RSAKey key;
  private static RSAKey otherKey;

  @BeforeAll
  static void keys() throws JOSEException {
    key = new RSAKeyGenerator(2048).keyID("k1").generate();
    otherKey = new RSAKeyGenerator(2048).keyID("k1").generate();
  }

  private static ForgeAuthentication auth(String token, String installationId) {
    var app = new ForgeApp();
    app.setInstallationId(installationId);
    var invocation = new ForgeInvocationToken();
    invocation.setApp(app);
    return new ForgeAuthentication(
        new ForgeApiContext(invocation, Optional.empty(), Optional.ofNullable(token)));
  }

  private static ForgeAuthentication auth(String token) {
    return auth(token, INSTALLATION);
  }

  @SuppressWarnings("PMD.ReplaceJavaUtilDate") // Nimbus API requires java.util.Date
  private static String jwt(RSAKey signer, String iss, String aud, long expOffsetMs)
      throws JOSEException {
    var claims =
        new JWTClaimsSet.Builder()
            .issuer(iss)
            .audience(java.util.List.of("api.atlassian.com", aud))
            .expirationTime(new Date(System.currentTimeMillis() + expOffsetMs))
            .build();
    var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k1").build(), claims);
    jwt.sign(new RSASSASigner(signer));
    return jwt.serialize();
  }

  private static ForgeSystemTokenPolicy strict() {
    return new ForgeSystemTokenPolicy(
        new ImmutableJWKSet<>(new JWKSet(key.toPublicJWK())), ISSUER, 0);
  }

  private static ForgeSystemTokenPolicy unsigned() {
    return new ForgeSystemTokenPolicy((JWKSource<SecurityContext>) null, "", 0);
  }

  @Test
  void acceptsValidSignedToken() throws Exception {
    assertThat(strict().hasSystemToken(auth(jwt(key, ISSUER, INSTALLATION, 60_000)))).isTrue();
  }

  @Test
  void acceptsRawInstallationIdFromHostContext() throws Exception {
    assertThat(strict().hasSystemToken(auth(jwt(key, ISSUER, INSTALLATION, 60_000), "inst-1")))
        .isTrue();
  }

  @Test
  void rejectsExpiredToken() throws Exception {
    assertThat(strict().hasSystemToken(auth(jwt(key, ISSUER, INSTALLATION, -60_000)))).isFalse();
  }

  @Test
  void rejectsExpiredTokenWithoutSignatureCheck() throws Exception {
    assertThat(unsigned().hasSystemToken(auth(jwt(key, "x", INSTALLATION, -60_000)))).isFalse();
  }

  @Test
  void acceptsUnexpiredTokenWithoutSignatureCheck() throws Exception {
    assertThat(unsigned().hasSystemToken(auth(jwt(key, "x", INSTALLATION, 60_000)))).isTrue();
  }

  @Test
  void rejectsWrongIssuer() throws Exception {
    assertThat(strict().hasSystemToken(auth(jwt(key, "evil", INSTALLATION, 60_000)))).isFalse();
  }

  @Test
  void rejectsTokenOfAnotherInstallation() throws Exception {
    var token = jwt(key, ISSUER, "ari:cloud:ecosystem::installation/inst-2", 60_000);
    assertThat(strict().hasSystemToken(auth(token))).isFalse();
  }

  @Test
  void rejectsWhenInvocationHasNoInstallation() throws Exception {
    assertThat(strict().hasSystemToken(auth(jwt(key, ISSUER, INSTALLATION, 60_000), null)))
        .isFalse();
  }

  @Test
  void rejectsForgedSignature() throws Exception {
    assertThat(strict().hasSystemToken(auth(jwt(otherKey, ISSUER, INSTALLATION, 60_000))))
        .isFalse();
  }

  @Test
  void rejectsMissingToken() {
    assertThat(unsigned().hasSystemToken(auth(null))).isFalse();
  }

  @Test
  void rejectsNonJwtToken() {
    assertThat(unsigned().hasSystemToken(auth("not-a-jwt"))).isFalse();
  }
}
