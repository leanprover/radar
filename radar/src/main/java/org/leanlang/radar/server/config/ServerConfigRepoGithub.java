package org.leanlang.radar.server.config;

import java.util.List;

public final class ServerConfigRepoGithub {
    /** Labels that must be absent from the PR before a GitHub command may run. */
    public List<String> blockingLabels = List.of();

    /** Labels that must be present on the PR before a GitHub command may run. */
    public List<String> requiredLabels = List.of();

    /**
     * If specified, the GitHub command may only be used by users with one of these associations.
     * Possible values are presumably the same as for <a href="https://docs.github.com/en/graphql/reference/enums#commentauthorassociation">CommentAuthorAssociation</a>.
     */
    public List<String> allowedAuthorAssociations = List.of();
}
