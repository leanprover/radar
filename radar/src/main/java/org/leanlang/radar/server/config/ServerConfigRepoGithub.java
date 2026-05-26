package org.leanlang.radar.server.config;

import java.util.List;

public final class ServerConfigRepoGithub {
    public List<String> blockingLabels = List.of();

    /**
     * If specified, the GitHub command may only be used by users with one of these associations.
     * Possible values are presumably the same as for <a href="https://docs.github.com/en/graphql/reference/enums#commentauthorassociation">CommentAuthorAssociation</a>.
     */
    public List<String> allowedAuthorAssociations = List.of();
}
