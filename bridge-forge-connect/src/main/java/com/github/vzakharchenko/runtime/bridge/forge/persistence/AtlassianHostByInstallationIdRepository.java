package com.github.vzakharchenko.runtime.bridge.forge.persistence;

import com.atlassian.connect.spring.AtlassianHost;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * Lookup {@link AtlassianHost} rows by Forge / Connect {@code installationId}.
 *
 * <p>Separate from {@link com.atlassian.connect.spring.AtlassianHostRepository} (keyed by {@code
 * clientKey}) so this module does not need {@code @Primary} or changes to the Connect repository
 * bean. Enabled via {@link
 * com.github.vzakharchenko.runtime.bridge.forge.AtlassianConnectForgeJpaAutoConfiguration} when
 * Connect's {@link com.atlassian.connect.spring.internal.jpa.AtlassianJpaAutoConfiguration} is
 * present.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface AtlassianHostByInstallationIdRepository extends Repository<AtlassianHost, String> {

  Optional<AtlassianHost> findByInstallationId(String installationId);
}
