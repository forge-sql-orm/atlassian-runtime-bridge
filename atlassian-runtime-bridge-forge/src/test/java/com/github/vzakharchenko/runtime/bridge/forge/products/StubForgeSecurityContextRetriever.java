package com.github.vzakharchenko.runtime.bridge.forge.products;

import com.atlassian.connect.spring.ForgeApiContext;
import com.atlassian.connect.spring.internal.auth.frc.ForgeSecurityContextRetriever;
import java.util.Optional;

/**
 * Minimal {@link ForgeSecurityContextRetriever} for tests: Mockito cannot subclass this type on
 * some JDKs.
 */
public final class StubForgeSecurityContextRetriever extends ForgeSecurityContextRetriever {

  private Optional<ForgeApiContext> forgeApiContext = Optional.empty();

  public void setForgeApiContext(Optional<ForgeApiContext> ctx) {
    this.forgeApiContext = ctx;
  }

  @Override
  public Optional<ForgeApiContext> getForgeApiContext() {
    return forgeApiContext;
  }
}
