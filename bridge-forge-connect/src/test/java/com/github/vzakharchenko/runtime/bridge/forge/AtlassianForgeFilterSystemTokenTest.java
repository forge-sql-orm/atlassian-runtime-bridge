package com.github.vzakharchenko.runtime.bridge.forge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.atlassian.connect.spring.ForgeApiContext;
import com.atlassian.connect.spring.ForgeInvocationToken;
import com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication;
import com.github.vzakharchenko.runtime.bridge.common.SystemTokenPolicy;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class AtlassianForgeFilterSystemTokenTest {

  private static final String TOKEN = "token";

  private final FilterChain chain = mock(FilterChain.class);
  private final MockHttpServletResponse response = new MockHttpServletResponse();

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  private void run(Optional<String> appToken, boolean valid) throws Exception {
    SystemTokenPolicy policy = auth -> valid;
    var service = mock(AtlassianForgeSecurityBridgeService.class);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new ForgeAuthentication(
                new ForgeApiContext(new ForgeInvocationToken(), Optional.empty(), appToken)));
    new AtlassianForgeFilter(service, policy)
        .doFilterInternal(new MockHttpServletRequest(), response, chain);
  }

  @Test
  void invalidSystemTokenIsRejectedWith403() throws Exception {
    run(Optional.of(TOKEN), false);
    assertThat(response.getStatus()).isEqualTo(403);
  }

  @Test
  void invalidSystemTokenStopsTheChain() throws Exception {
    run(Optional.of(TOKEN), false);
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  void invalidSystemTokenClearsSecurityContext() throws Exception {
    run(Optional.of(TOKEN), false);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void validSystemTokenContinuesTheChain() throws Exception {
    run(Optional.of(TOKEN), true);
    verify(chain).doFilter(any(), any());
  }

  @Test
  void requestWithoutSystemTokenIsNotValidated() throws Exception {
    run(Optional.empty(), false);
    verify(chain).doFilter(any(), any());
  }
}
