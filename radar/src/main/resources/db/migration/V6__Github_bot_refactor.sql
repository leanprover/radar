DROP TABLE github_command_resolved;
DROP TABLE github_command;

CREATE TABLE github_command (
    owner                      TEXT NOT NULL,
    repo                       TEXT NOT NULL,

    comment_id_long            INT  NOT NULL,
    comment_issue_number       INT  NOT NULL,
    comment_created_time       TEXT NOT NULL,
    comment_author_id_long     INT  NOT NULL,
    comment_author_login       TEXT NOT NULL,
    comment_author_association TEXT NOT NULL,
    comment_body               TEXT NOT NULL,

    reply_id_long              INT,
    reply_content              TEXT,
    reply_tries                INT,

    PRIMARY KEY (owner, repo, comment_id_long)
) STRICT;

CREATE TABLE github_command_resolved (
    owner                   TEXT NOT NULL,
    repo                    TEXT NOT NULL,
    comment_id_long         INT  NOT NULL,

    pull_id_long            INT  NOT NULL,
    pull_number             INT  NOT NULL,
    pull_created_time       TEXT NOT NULL,
    pull_author_id_long     INT  NOT NULL,
    pull_author_login       TEXT NOT NULL,
    pull_author_association TEXT NOT NULL,
    pull_head_sha           TEXT NOT NULL,
    pull_head_ref           TEXT NOT NULL,
    pull_head_owner         TEXT NOT NULL,
    pull_head_repo          TEXT NOT NULL,
    pull_base_sha           TEXT NOT NULL,
    pull_base_ref           TEXT NOT NULL,

    chash                   TEXT NOT NULL REFERENCES commits (chash),
    against_chash           TEXT NOT NULL REFERENCES commits (chash),
    detected_time           TEXT NOT NULL,
    completed_time          TEXT,

    PRIMARY KEY (owner, repo, comment_id_long),
    FOREIGN KEY (owner, repo, comment_id_long) REFERENCES github_command ON DELETE CASCADE
) STRICT;
