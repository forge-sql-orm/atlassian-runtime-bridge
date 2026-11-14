package com.github.vzakharchenko.runtime.bridge.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.ForgeApiContext;
import com.atlassian.connect.spring.ForgeInvocationToken;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

class SystemCallVerifierTest {

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  private static ForgeApiContext context(String principal) {
    var token = new ForgeInvocationToken();
    token.setPrincipal(principal);
    return new ForgeApiContext(token, Optional.empty(), Optional.empty());
  }

  private static AtlassianHostUser hostUser(String accountId) {
    var host = new AtlassianHost();
    host.setClientKey("ck");
    var builder = AtlassianHostUser.builder(host);
    return accountId == null ? builder.build() : builder.withUserAccountId(accountId).build();
  }

  @Test
  void forgeCallWithoutUserIsSystemCall() {
    assertThat(SystemCallVerifier.isSystemCall(new ForgeAuthentication(context(null)))).isTrue();
  }

  @Test
  void forgeCallWithHostUserWithoutAccountIsSystemCall() {
    assertThat(
            SystemCallVerifier.isSystemCall(new ForgeAuthentication(context(null), hostUser(null))))
        .isTrue();
  }

  @Test
  void principalInTokenMakesItUserCall() {
    assertThat(SystemCallVerifier.isSystemCall(new ForgeAuthentication(context("acc-1"))))
        .isFalse();
  }

  @Test
  void accountIdOnHostUserMakesItUserCall() {
    assertThat(
            SystemCallVerifier.isSystemCall(
                new ForgeAuthentication(context(null), hostUser("acc-1"))))
        .isFalse();
  }

  @Test
  void missingAuthenticationIsNotSystemCall() {
    assertThat(SystemCallVerifier.isSystemCall(null)).isFalse();
  }

  @Test
  void nonForgeAuthenticationIsNotSystemCall() {
    assertThat(SystemCallVerifier.isSystemCall(new TestingAuthenticationToken("u", "p"))).isFalse();
  }

  @Test
  void assertSystemCallThrowsForUserCall() {
    SecurityContextHolder.getContext().setAuthentication(new ForgeAuthentication(context("a")));
    assertThatThrownBy(SystemCallVerifier::assertSystemCall)
        .isInstanceOf(AccessDeniedException.class);
  }

  @SystemCallOnly
  static class Guarded {
    public void run() {}
  }

  static class Open {
    public void run() {}

    @SystemCallOnly
    public void guarded() {}
  }

  private static boolean preHandle(Object bean, String method) throws Exception {
    Method m = bean.getClass().getMethod(method);
    return new SystemCallInterceptor(null)
        .preHandle(
            new MockHttpServletRequest(),
            new MockHttpServletResponse(),
            new HandlerMethod(bean, m));
  }

  @Test
  void interceptorRejectsUserCallOnAnnotatedClass() {
    SecurityContextHolder.getContext().setAuthentication(new ForgeAuthentication(context("a")));
    assertThatThrownBy(() -> preHandle(new Guarded(), "run"))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void interceptorRejectsUserCallOnAnnotatedMethod() {
    SecurityContextHolder.getContext().setAuthentication(new ForgeAuthentication(context("a")));
    assertThatThrownBy(() -> preHandle(new Open(), "guarded"))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void interceptorAllowsSystemCall() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(new ForgeAuthentication(context(null)));
    assertThat(preHandle(new Guarded(), "run")).isTrue();
  }

  @Test
  void interceptorIgnoresUnannotatedHandlers() {
    SecurityContextHolder.getContext().setAuthentication(new ForgeAuthentication(context("a")));
    assertThatCode(() -> preHandle(new Open(), "run")).doesNotThrowAnyException();
  }

  @Test
  void tokenPolicyRejectsSystemCallWithoutToken() {
    SecurityContextHolder.getContext().setAuthentication(new ForgeAuthentication(context(null)));
    assertThatThrownBy(() -> SystemCallVerifier.assertSystemCall(auth -> false))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void tokenPolicyAcceptsSystemCallWithToken() {
    SecurityContextHolder.getContext().setAuthentication(new ForgeAuthentication(context(null)));
    assertThatCode(() -> SystemCallVerifier.assertSystemCall(auth -> true))
        .doesNotThrowAnyException();
  }
}
