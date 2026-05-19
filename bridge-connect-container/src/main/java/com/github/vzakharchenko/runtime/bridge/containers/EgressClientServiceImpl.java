package com.github.vzakharchenko.runtime.bridge.containers;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.vzakharchenko.runtime.bridge.containers.exceptions.EgressRequestException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * Default {@link EgressClientService}: {@link RestClient} for JSON sidecar APIs and {@link
 * RestTemplate} factory for Jira paths via {@code /jira}.
 *
 * <p>Proxy base URL: {@code egress.proxy.url} (default {@code http://localhost:7072} for local
 * docker-compose sidecar).
 */
@Component
public class EgressClientServiceImpl implements EgressClientService {

  private static final Logger log = LoggerFactory.getLogger(EgressClientServiceImpl.class);

  /** {@code as=} value in {@code forge-proxy-authorization} for app- or user-scoped Jira calls. */
  public enum AuthType {
    /** Act as the installed app (system). */
    APP("app"),
    /** Act as a specific user ({@code accountId} required). */
    USER("user");

    private final String wireValue;

    AuthType(String wireValue) {
      this.wireValue = wireValue;
    }

    /**
     * Lowercase token expected by the Forge egress sidecar in {@code as=…}; the enum name follows
     * Java conventions, this preserves the wire format.
     */
    @Override
    public String toString() {
      return wireValue;
    }
  }

  /**
   * Authorization context for an installation-scoped Forge request. Use the static factories for
   * the common cases:
   *
   * <ul>
   *   <li>{@link #installation(String)} — installation only, no {@code as=} clause
   *   <li>{@link #asApp(String)} — act as the app
   *   <li>{@link #asUser(String, String)} — act as a specific user
   * </ul>
   */
  public record InstallationAuth(
      String installationId, @Nullable AuthType authType, @Nullable String accountId) {

    public static InstallationAuth installation(final String installationId) {
      return new InstallationAuth(installationId, null, null);
    }

    public static InstallationAuth asApp(final String installationId) {
      return new InstallationAuth(installationId, AuthType.APP, null);
    }

    public static InstallationAuth asUser(final String installationId, final String accountId) {
      return new InstallationAuth(installationId, AuthType.USER, accountId);
    }
  }

  private final RestClient restClient;

  @Value("${egress.proxy.url:http://localhost:7072}")
  private String egressProxyUrl;

  /**
   * @param restClientBuilder optional builder; defaults to {@link RestClient#builder()}
   */
  public EgressClientServiceImpl(final Optional<RestClient.Builder> restClientBuilder) {
    this.restClient = restClientBuilder.orElseGet(RestClient::builder).build();
  }

  @Override
  public ResponseEntity<JsonNode> getInvocationContext(final String invocationId) {
    return sendInvocationTokenRequest(
        "Context request", invocationId, HttpMethod.GET, buildUri("/invocation/context"), null);
  }

  /**
   * Sidecar request authorized with {@code Forge id=<invocationId>}.
   *
   * @throws com.github.vzakharchenko.runtime.bridge.containers.exceptions.EgressRequestException on
   *     non-2xx responses
   */
  public ResponseEntity<JsonNode> sendInvocationTokenRequest(
      final String requestType,
      final String invocationId,
      final HttpMethod httpMethod,
      final URI uri,
      @Nullable final Object body) {
    return execute(requestType, httpMethod, uri, body, invocationAuthHeader(invocationId));
  }

  public ResponseEntity<JsonNode> sendRequestWithBody(
      final String requestType,
      final InstallationAuth auth,
      final HttpMethod httpMethod,
      final URI uri,
      @Nullable final Object body) {
    return execute(requestType, httpMethod, uri, body, installationAuthHeader(auth));
  }

  /**
   * Jira API call via {@code <proxy>/jira/<apiPath>} with installation auth.
   *
   * @param apiPath Jira REST suffix (with or without leading {@code /})
   */
  public ResponseEntity<JsonNode> sendJiraRequest(
      final InstallationAuth auth,
      final HttpMethod httpMethod,
      final String apiPath,
      @Nullable final Object body) {
    return sendRequestWithBody("Jira request", auth, httpMethod, buildJiraUri(apiPath), body);
  }

  @Override
  public RestTemplate jiraTemplateRequest(final InstallationAuth auth) {
    var template = new RestTemplate();
    template.setUriTemplateHandler(new DefaultUriBuilderFactory(jiraEgressBaseUrl()));
    template
        .getInterceptors()
        .add(
            (request, body, execution) -> {
              request
                  .getHeaders()
                  .add(ForgeEgressHeaders.FORGE_AUTHORIZATION, installationAuthHeader(auth));
              return execution.execute(request, body);
            });
    return template;
  }

  private URI buildUri(final String path) {
    return URI.create(normalizeEgressProxyUrl() + path);
  }

  private URI buildJiraUri(final String apiPath) {
    final String suffix = apiPath.startsWith("/") ? apiPath : "/" + apiPath;
    return URI.create(jiraEgressBaseUrl() + suffix);
  }

  /** {@code <egressProxyUrl>/jira} whether {@code egressProxyUrl} ends with {@code /} or not. */
  private String jiraEgressBaseUrl() {
    return egressProxyUrl.endsWith("/") ? egressProxyUrl + "jira" : egressProxyUrl + "/jira";
  }

  private String normalizeEgressProxyUrl() {
    return egressProxyUrl.endsWith("/")
        ? egressProxyUrl.substring(0, egressProxyUrl.length() - 1)
        : egressProxyUrl;
  }

  private ResponseEntity<JsonNode> execute(
      final String requestType,
      final HttpMethod httpMethod,
      final URI uri,
      @Nullable final Object body,
      final String authHeader) {

    var request =
        restClient
            .method(httpMethod)
            .uri(uri)
            .header(ForgeEgressHeaders.FORGE_AUTHORIZATION, authHeader);

    Optional.ofNullable(body).ifPresent(request::body);

    return request
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            (httpRequest, response) -> {
              String errorBody = readBody(response.getBody());
              if (log.isErrorEnabled()) {
                log.error("{} failed with status {}", requestType, response.getStatusCode());
              }
              throw new EgressRequestException(
                  requestType
                      + " failed with status "
                      + response.getStatusCode()
                      + ": "
                      + errorBody);
            })
        .toEntity(JsonNode.class);
  }

  private static String readBody(final java.io.InputStream body) {
    try {
      return StreamUtils.copyToString(body, StandardCharsets.UTF_8);
    } catch (IOException e) {
      if (log.isWarnEnabled()) {
        log.warn("Failed to read egress error response body", e);
      }
      return "<body unreadable: " + e.getMessage() + ">";
    }
  }

  private static String invocationAuthHeader(final String invocationId) {
    return "Forge id=" + invocationId;
  }

  private static String installationAuthHeader(final InstallationAuth auth) {
    final var header = new StringBuilder("Forge installationId=").append(auth.installationId());
    if (auth.authType() != null) {
      header.append(",as=").append(auth.authType());
    }
    if (auth.accountId() != null) {
      header.append(",accountId=").append(auth.accountId());
    }
    return header.toString();
  }
}
