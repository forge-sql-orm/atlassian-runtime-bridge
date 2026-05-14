package com.github.vzakharchenko.runtime.bridge.forge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.ForgeApiContext;
import com.atlassian.connect.spring.ForgeInvocationToken;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AtlassianForgeFilterTest {

  @Mock private AtlassianForgeSecurityBridgeService forgeSecurityBridgeService;

  @Mock private FilterChain filterChain;

  private AtlassianForgeFilter filter;

  @BeforeEach
  void setUp() {
    filter = new AtlassianForgeFilter(forgeSecurityBridgeService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Nested
  class GivenNoAuthentication {

    @BeforeEach
    void prepare() {
      SecurityContextHolder.clearContext();
    }

    @Test
    void doesNotResolveHostUser() throws Exception {
      invokeFilter();

      verify(forgeSecurityBridgeService, never()).getAtlassianHostUserFromContext();
    }

    @Test
    void invokesFilterChain() throws Exception {
      invokeFilter();

      verify(filterChain).doFilter(any(), any());
    }
  }

  @Nested
  class GivenNonForgeAuthentication {

    @BeforeEach
    void prepare() {
      SecurityContextHolder.getContext()
          .setAuthentication(new UsernamePasswordAuthenticationToken("user", "pwd"));
    }

    @Test
    void doesNotResolveHostUser() throws Exception {
      invokeFilter();

      verify(forgeSecurityBridgeService, never()).getAtlassianHostUserFromContext();
    }

    @Test
    void invokesFilterChain() throws Exception {
      invokeFilter();

      verify(filterChain).doFilter(any(), any());
    }
  }

  @Nested
  class GivenForgeAuthenticationAndResolvedUser {

    private ForgeApiContext apiContext;
    private AtlassianHostUser resolved;

    @BeforeEach
    void prepare() {
      apiContext = testForgeApiContext();
      SecurityContextHolder.getContext().setAuthentication(new ForgeAuthentication(apiContext));
      AtlassianHost host = new AtlassianHost();
      host.setClientKey("ck");
      resolved = AtlassianHostUser.builder(host).withUserAccountId("ari:user:1").build();
      when(forgeSecurityBridgeService.getAtlassianHostUserFromContext())
          .thenReturn(Optional.of(resolved));
    }

    @Test
    void preservesForgeApiContextReferenceOnAuthentication() throws Exception {
      invokeFilter();

      ForgeAuthentication forge =
          (ForgeAuthentication) SecurityContextHolder.getContext().getAuthentication();
      assertThat(forge.getDetails())
          .as("Forge API context instance must be unchanged after enrichment")
          .isSameAs(apiContext);
    }

    @Test
    void setsPrincipalToResolvedHostUserInstance() throws Exception {
      invokeFilter();

      ForgeAuthentication forge =
          (ForgeAuthentication) SecurityContextHolder.getContext().getAuthentication();
      assertThat(forge.getPrincipal())
          .as("principal must be the AtlassianHostUser returned by the bridge")
          .isSameAs(resolved);
    }

    @Test
    void invokesFilterChain() throws Exception {
      invokeFilter();

      verify(filterChain).doFilter(any(), any());
    }
  }

  @Nested
  class GivenForgeAuthenticationAndEmptyUser {

    private ForgeAuthentication incoming;

    @BeforeEach
    void prepare() {
      ForgeApiContext apiContext = testForgeApiContext();
      incoming = new ForgeAuthentication(apiContext);
      SecurityContextHolder.getContext().setAuthentication(incoming);
      when(forgeSecurityBridgeService.getAtlassianHostUserFromContext())
          .thenReturn(Optional.empty());
    }

    @Test
    void leavesAuthenticationUnchanged() throws Exception {
      invokeFilter();

      assertThat(SecurityContextHolder.getContext().getAuthentication())
          .as("empty optional must not replace ForgeAuthentication")
          .isSameAs(incoming);
    }

    @Test
    void invokesFilterChain() throws Exception {
      invokeFilter();

      verify(filterChain).doFilter(any(), any());
    }
  }

  @Test
  void doFilterInternal_whenBridgeThrows_stillInvokesChain() throws Exception {
    ForgeApiContext apiContext = testForgeApiContext();
    SecurityContextHolder.getContext().setAuthentication(new ForgeAuthentication(apiContext));

    when(forgeSecurityBridgeService.getAtlassianHostUserFromContext())
        .thenThrow(new IllegalStateException("no context"));

    invokeFilter();

    verify(filterChain).doFilter(any(), any());
  }

  private void invokeFilter() throws IOException, ServletException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilterInternal(request, response, filterChain);
  }

  private static ForgeApiContext testForgeApiContext() {
    return new ForgeApiContext(new ForgeInvocationToken(), Optional.empty(), Optional.empty());
  }
}
