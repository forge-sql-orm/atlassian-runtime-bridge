/**
 * Runtime selection between <strong>Connect</strong> and <strong>Forge</strong> when calling
 * Atlassian product REST APIs from the same Spring codebase.
 *
 * <p><b>Entry points for application code</b> (defined in {@code atlassian-runtime-bridge-common}):
 * {@link com.github.vzakharchenko.runtime.bridge.common.JiraProductAdapter}, {@link
 * com.github.vzakharchenko.runtime.bridge.common.ConfluenceProductAdapter}, {@link
 * com.github.vzakharchenko.runtime.bridge.common.OtherProductAdapter}.
 *
 * <p><b>Beans that perform the switch</b>: {@link JiraProductSelectAdapter}, {@link
 * ConfluenceProductSelectAdapter}, {@link OtherProductSelectAdapter} — each checks whether the
 * current {@code Authentication} is {@link
 * com.atlassian.connect.spring.internal.auth.frc.ForgeAuthentication} and delegates to either
 * {@link ConnectProductsAdapter} (Connect {@link
 * com.atlassian.connect.spring.AtlassianHostRestClients}) or the matching {@code
 * *ProductForgeAdapter} (Forge {@link com.atlassian.connect.spring.AtlassianForgeRestClients}).
 *
 * <p>Forge-specific impersonation and bearer construction live in {@link
 * AbstractProductForgeAdapter}.
 */
package com.github.vzakharchenko.runtime.bridge.forge.products;
