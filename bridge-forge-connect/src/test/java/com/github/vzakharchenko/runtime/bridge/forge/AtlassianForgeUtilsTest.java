package com.github.vzakharchenko.runtime.bridge.forge;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AtlassianForgeUtilsTest {

  private static final String JIRA_EX_PREFIX = "https://api.atlassian.com/ex/jira/";

  @Test
  void getContextId_buildsJiraSiteAriFromLastPathSegment() {
    assertThat(AtlassianForgeUtils.getContextId(JIRA_EX_PREFIX + "my-cloud-id"))
        .as("context id must be the Jira site ARI built from the trailing cloud id segment")
        .isEqualTo("ari:cloud:jira::site/my-cloud-id");
  }

  @Test
  void getContextId_handlesSingleSegmentUrl() {
    assertThat(AtlassianForgeUtils.getContextId("only-segment"))
        .as("single path segment still produces a valid site ARI suffix")
        .isEqualTo("ari:cloud:jira::site/only-segment");
  }

  @Test
  void getContextId_usesLastSegmentWhenUrlContainsMultiplePathParts() {
    assertThat(
            AtlassianForgeUtils.getContextId("https://api.atlassian.com/ex/jira/site-a/extra/leaf"))
        .as("the last slash-separated segment becomes the site id in the ARI")
        .isEqualTo("ari:cloud:jira::site/leaf");
  }
}
