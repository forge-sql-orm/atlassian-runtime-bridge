package sample.connect.spring.atlaskit.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * JSON body returned by Jira Cloud {@code GET /rest/api/3/myself}.
 * <p>
 * Unknown fields are ignored so the model stays tolerant to API extensions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraMyselfResponse(
        String accountId,
        String accountType,
        boolean active,
        ApplicationRoles applicationRoles,
        Map<String, String> avatarUrls,
        String displayName,
        String emailAddress,
        Groups groups,
        String key,
        String name,
        String self,
        String timeZone
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApplicationRoles(List<Object> items, int size) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Groups(List<Object> items, int size) {
    }
}
