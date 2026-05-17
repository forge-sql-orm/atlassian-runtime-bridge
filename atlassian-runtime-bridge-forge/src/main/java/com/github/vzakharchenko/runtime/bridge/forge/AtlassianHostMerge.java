package com.github.vzakharchenko.runtime.bridge.forge;

import com.atlassian.connect.spring.AtlassianHost;
import java.util.function.Consumer;

/**
 * Merges a Connect-persisted {@link AtlassianHost} with a minimally built Forge host.
 *
 * <p>Starts from the stored row, then overlays every non-null field from the Forge-built host so
 * invocation-time values (e.g. {@code cloudId}) win without mutating the JPA-managed instance.
 */
final class AtlassianHostMerge {

  private AtlassianHostMerge() {}

  static AtlassianHost merge(AtlassianHost stored, AtlassianHost minimal) {
    AtlassianHost merged = copy(stored);
    overlay(merged, minimal);
    return merged;
  }

  private static AtlassianHost copy(AtlassianHost source) {
    AtlassianHost copy = new AtlassianHost();
    copy.setClientKey(source.getClientKey());
    copy.setOauthClientId(source.getOauthClientId());
    copy.setSharedSecret(source.getSharedSecret());
    copy.setAuthentication(source.getAuthentication());
    copy.setCloudId(source.getCloudId());
    copy.setBaseUrl(source.getBaseUrl());
    copy.setDisplayUrl(source.getDisplayUrl());
    copy.setDisplayUrlServicedeskHelpCenter(source.getDisplayUrlServicedeskHelpCenter());
    copy.setCapabilitySet(source.getCapabilitySet());
    copy.setProductType(source.getProductType());
    copy.setDescription(source.getDescription());
    copy.setServiceEntitlementNumber(source.getServiceEntitlementNumber());
    copy.setEntitlementId(source.getEntitlementId());
    copy.setEntitlementNumber(source.getEntitlementNumber());
    copy.setInstallationId(source.getInstallationId());
    copy.setAddonInstalled(source.isAddonInstalled());
    copy.setCreatedDate(source.getCreatedDate());
    copy.setLastModifiedDate(source.getLastModifiedDate());
    copy.setCreatedBy(source.getCreatedBy());
    copy.setLastModifiedBy(source.getLastModifiedBy());
    return copy;
  }

  private static void overlay(AtlassianHost target, AtlassianHost minimal) {
    setIfNotNull(minimal.getClientKey(), target::setClientKey);
    setIfNotNull(minimal.getOauthClientId(), target::setOauthClientId);
    setIfNotNull(minimal.getSharedSecret(), target::setSharedSecret);
    if (minimal.getAuthentication() != null) {
      target.setAuthentication(minimal.getAuthentication());
    }
    setIfNotNull(minimal.getCloudId(), target::setCloudId);
    setIfNotNull(minimal.getBaseUrl(), target::setBaseUrl);
    setIfNotNull(minimal.getDisplayUrl(), target::setDisplayUrl);
    setIfNotNull(
        minimal.getDisplayUrlServicedeskHelpCenter(), target::setDisplayUrlServicedeskHelpCenter);
    setIfNotNull(minimal.getCapabilitySet(), target::setCapabilitySet);
    setIfNotNull(minimal.getProductType(), target::setProductType);
    setIfNotNull(minimal.getDescription(), target::setDescription);
    setIfNotNull(minimal.getServiceEntitlementNumber(), target::setServiceEntitlementNumber);
    setIfNotNull(minimal.getEntitlementId(), target::setEntitlementId);
    setIfNotNull(minimal.getEntitlementNumber(), target::setEntitlementNumber);
    setIfNotNull(minimal.getInstallationId(), target::setInstallationId);
    target.setAddonInstalled(minimal.isAddonInstalled());
  }

  private static void setIfNotNull(String value, Consumer<String> setter) {
    if (value != null) {
      setter.accept(value);
    }
  }
}
