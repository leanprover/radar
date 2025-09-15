CREATE TABLE commits (
    chash            TEXT NOT NULL PRIMARY KEY,
    author_name      TEXT NOT NULL,
    author_email     TEXT NOT NULL,
    author_time      TEXT NOT NULL,
    author_offset    INT  NOT NULL,
    committer_name   TEXT NOT NULL,
    committer_email  TEXT NOT NULL,
    committer_time   TEXT NOT NULL,
    committer_offset INT  NOT NULL,
    message_title    TEXT NOT NULL,
    message_body     TEXT,
    seen             INT  NOT NULL DEFAULT 0
) STRICT;

CREATE TABLE commit_relationships (
    child           TEXT NOT NULL,
    parent          TEXT NOT NULL,
    parent_position INT  NOT NULL,
    PRIMARY KEY (child, parent),
    FOREIGN KEY (child) REFERENCES commits (chash) ON DELETE CASCADE,
    FOREIGN KEY (parent) REFERENCES commits (chash) ON DELETE CASCADE
) STRICT;

CREATE TABLE history (
    position INT  NOT NULL PRIMARY KEY,
    chash    TEXT NOT NULL UNIQUE,
    FOREIGN KEY (chash) REFERENCES commits (chash)
) STRICT;

CREATE TABLE queue (
    chash       TEXT NOT NULL PRIMARY KEY,
    queued_time TEXT NOT NULL,
    bumped_time TEXT NOT NULL,
    priority    INT  NOT NULL DEFAULT 0,
    FOREIGN KEY (chash) REFERENCES commits (chash)
) STRICT;

CREATE TABLE metrics (
    metric    TEXT NOT NULL PRIMARY KEY,
    unit      TEXT,
    direction INT  NOT NULL,
    CHECK (-1 <= direction AND direction <= 1)
) STRICT;

CREATE TABLE runs (
    chash       TEXT NOT NULL,
    runner      TEXT NOT NULL,
    script      TEXT NOT NULL,
    chash_bench TEXT NOT NULL,
    start_time  TEXT NOT NULL,
    end_time    TEXT NOT NULL,
    exit_code   INT  NOT NULL,
    PRIMARY KEY (chash, runner, script),
    FOREIGN KEY (chash) REFERENCES commits (chash) ON DELETE CASCADE
) STRICT;

CREATE TABLE measurements (
    chash  TEXT NOT NULL,
    metric TEXT NOT NULL,
    value  REAL NOT NULL,
    PRIMARY KEY (chash, metric),
    FOREIGN KEY (chash) REFERENCES commits (chash) ON DELETE CASCADE,
    FOREIGN KEY (metric) REFERENCES metrics (metric) ON DELETE CASCADE
) STRICT;
