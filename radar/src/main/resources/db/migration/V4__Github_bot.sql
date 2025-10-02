CREATE TABLE github_last_checked (
    last_checked_time TEXT NOT NULL
) STRICT;

CREATE TABLE github_command (
    owner_and_repo TEXT NOT NULL,
    id             TEXT NOT NULL,
    pr_number      TEXT NOT NULL,
    reply_id       TEXT,
    reply_content  TEXT NOT NULL,
    reply_tries    INT,
    PRIMARY KEY (owner_and_repo, id)
) STRICT;

CREATE TABLE github_command_resolved (
    owner_and_repo TEXT NOT NULL,
    id             TEXT NOT NULL,
    head_chash     TEXT NOT NULL REFERENCES commits (chash),
    base_chash     TEXT NOT NULL REFERENCES commits (chash),
    active         INT  NOT NULL,
    PRIMARY KEY (owner_and_repo, id),
    FOREIGN KEY (owner_and_repo, id) REFERENCES github_command ON DELETE CASCADE
) STRICT;
