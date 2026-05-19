package com.github.vzakharchenko.runtime.bridge.containers;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Outbound calls to the Forge Containers egress sidecar ({@code FORGE_EGRESS_PROXY_URL}).
 *
 * <p>All requests use the {@value
 * com.github.vzakharchenko.runtime.bridge.containers.ForgeEgressHeaders#FORGE_AUTHORIZATION}
 * header. Jira product REST calls are routed under {@code /jira/…} on the proxy.
 */
public interface EgressClientService {

  /**
   * Fetches invocation context for {@code x-forge-invocation-id} ({@code GET /invocation/context}).
   *
   * @param invocationId Forge invocation id from the ingress request
   * @return parsed JSON body from the sidecar
   */
  ResponseEntity<JsonNode> getInvocationContext(String invocationId);

  /**
   * {@link RestTemplate} whose relative paths are resolved against {@code <proxy>/jira} and whose
   * requests carry installation-scoped {@code forge-proxy-authorization}.
   *
   * @param auth installation / app / user authorization for the egress proxy
   * @return template for Jira REST paths such as {@code /rest/api/3/myself}
   */
  RestTemplate jiraTemplateRequest(EgressClientServiceImpl.InstallationAuth auth);
}
