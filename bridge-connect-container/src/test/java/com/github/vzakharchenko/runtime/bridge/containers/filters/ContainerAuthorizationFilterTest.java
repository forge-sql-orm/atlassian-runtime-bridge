package com.github.vzakharchenko.runtime.bridge.containers.filters;

import static com.github.vzakharchenko.runtime.bridge.containers.ForgeIngressHeaders.INVOCATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.vzakharchenko.runtime.bridge.containers.ContainersTestFixtures;
import com.github.vzakharchenko.runtime.bridge.containers.ForgeContextService;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class ContainerAuthorizationFilterTest {

  private static final String INVOCATION_ID_VALUE = "inv-1";

  @Mock private ForgeContextService forgeContextService;

  private ContainerAuthorizationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new ContainerAuthorizationFilter(forgeContextService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void withoutInvocationId_skipsAuthentication() throws Exception {
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verifyNoInteractions(forgeContextService);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void withInvocationId_setsAuthenticationForChainThenClears() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader(INVOCATION_ID, INVOCATION_ID_VALUE);
    var response = new MockHttpServletResponse();
    var forgeAuth =
        ContainersTestFixtures.forgeAuthentication(
            ContainersTestFixtures.hostUser("cloud", "inst", "acc"));
    when(forgeContextService.emulateForgeAuthentication(INVOCATION_ID_VALUE))
        .thenReturn(Optional.of(forgeAuth));

    filter.doFilter(
        request,
        response,
        (req, res) ->
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isSameAs(forgeAuth));

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void whenEmulationEmpty_continuesWithoutAuthentication() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader(INVOCATION_ID, INVOCATION_ID_VALUE);
    var response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    when(forgeContextService.emulateForgeAuthentication(INVOCATION_ID_VALUE)).thenReturn(Optional.empty());

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void whenEmulationFails_throwsAccessDenied() {
    var request = new MockHttpServletRequest();
    request.addHeader(INVOCATION_ID, INVOCATION_ID_VALUE);
    var response = new MockHttpServletResponse();
    when(forgeContextService.emulateForgeAuthentication(INVOCATION_ID_VALUE))
        .thenThrow(new RuntimeException("egress down"));

    assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {}))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Failed to set up Forge authentication");
  }
}
