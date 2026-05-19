package com.github.vzakharchenko.runtime.bridge.containers.filters;

import static com.github.vzakharchenko.runtime.bridge.containers.ForgeIngressHeaders.INVOCATION_ID;
import static com.github.vzakharchenko.runtime.bridge.containers.ForgeIngressHeaders.INVOCATION_LOGGING_ATTRIBUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.UnitTestShouldIncludeAssert"})
class InvocationIdLoggingFilterTest {

  private final InvocationIdLoggingFilter filter = new InvocationIdLoggingFilter();

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void putsInvocationHeadersIntoMdcForRequest() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader(INVOCATION_ID, "inv-42");
    request.addHeader(INVOCATION_LOGGING_ATTRIBUTES, "attrs");
    var response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    assertThat(MDC.get(INVOCATION_ID)).isNull();
    assertThat(MDC.get(INVOCATION_LOGGING_ATTRIBUTES)).isNull();
  }

  @Test
  void exposesMdcValuesDuringChain() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader(INVOCATION_ID, "inv-42");
    var response = new MockHttpServletResponse();

    filter.doFilter(
        request, response, (req, res) -> assertThat(MDC.get(INVOCATION_ID)).isEqualTo("inv-42"));
  }
}
