CREATE TABLE commits (
    chash          TEXT NOT NULL PRIMARY KEY,
    author         TEXT NOT NULL,
    author_time    INT  NOT NULL,
    committer      TEXT NOT NULL,
    committer_time INT  NOT NULL,
    message        TEXT NOT NULL
) STRICT;

CREATE TABLE commit_relationships (
    parent TEXT NOT NULL,
    child  TEXT NOT NULL,
    PRIMARY KEY (parent, child),
    FOREIGN KEY (parent) REFERENCES commits (chash) ON DELETE CASCADE,
    FOREIGN KEY (child) REFERENCES commits (chash) ON DELETE CASCADE
) STRICT;

CREATE TABLE branches (
    name  TEXT NOT NULL PRIMARY KEY,
    chash TEXT NOT NULL,
    FOREIGN KEY (chash) REFERENCES commits (chash) ON DELETE CASCADE
) STRICT;

CREATE TABLE history (
    position INT  NOT NULL PRIMARY KEY,
    chash    TEXT NOT NULL UNIQUE,
    FOREIGN KEY (chash) REFERENCES commits (chash)
) STRICT;

CREATE TABLE queue (
    chash       TEXT NOT NULL PRIMARY KEY,
    queued_time INT  NOT NULL,
    priority    INT  NOT NULL DEFAULT 0,
    FOREIGN KEY (chash) REFERENCES commits (chash)
) STRICT;

CREATE TABLE metrics (
    name      TEXT NOT NULL PRIMARY KEY,
    direction INT  NOT NULL,
    CHECK (-1 <= direction AND direction <= 1)
) STRICT;

CREATE TABLE run (
    chash       TEXT NOT NULL PRIMARY KEY,
    chash_bench TEXT NOT NULL,
    start_time  INT  NOT NULL,
    end_time    INT  NOT NULL,
    FOREIGN KEY (chash) REFERENCES commits (chash) ON DELETE CASCADE
) STRICT;

CREATE TABLE measurements (
    chash TEXT NOT NULL,
    name  TEXT NOT NULL,
    value INT  NOT NULL,
    PRIMARY KEY (chash, name),
    FOREIGN KEY (chash) REFERENCES commits (chash) ON DELETE CASCADE,
    FOREIGN KEY (name) REFERENCES metrics (name) ON DELETE CASCADE
) STRICT;
