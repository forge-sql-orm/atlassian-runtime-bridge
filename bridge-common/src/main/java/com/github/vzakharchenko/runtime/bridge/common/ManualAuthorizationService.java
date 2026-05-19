package com.github.vzakharchenko.runtime.bridge.common;

import com.atlassian.connect.spring.AtlassianHost;
import com.atlassian.connect.spring.AtlassianHostUser;
import java.util.Optional;

public interface ManualAuthorizationService {
  void authorize(AtlassianHostUser atlassianHostUser);

  void authorize(AtlassianHost atlassianHost);

  void authorize(String cloudId, String installationId, Optional<String> accountId);
}
