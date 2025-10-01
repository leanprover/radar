CREATE TABLE github_last_checked (
    last_checked_time TEXT NOT NULL
) STRICT;

CREATE TABLE github_command (
    repo          TEXT NOT NULL,
    id            TEXT NOT NULL,
    pr_number     TEXT NOT NULL,
    reply_id      TEXT,
    reply_content TEXT NOT NULL,
    reply_tries   INT,
    PRIMARY KEY (repo, id)
) STRICT;

CREATE TABLE github_command_resolved (
    repo       TEXT NOT NULL,
    id         TEXT NOT NULL,
    head_chash TEXT NOT NULL REFERENCES commits (chash),
    base_chash TEXT NOT NULL REFERENCES commits (chash),
    active     INT  NOT NULL,
    PRIMARY KEY (repo, id),
    FOREIGN KEY (repo, id) REFERENCES github_command ON DELETE CASCADE
) STRICT;
