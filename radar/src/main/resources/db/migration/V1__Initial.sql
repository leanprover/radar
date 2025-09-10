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
    parent TEXT NOT NULL,
    child  TEXT NOT NULL,
    PRIMARY KEY (parent, child),
    FOREIGN KEY (parent) REFERENCES commits (chash) ON DELETE CASCADE,
    FOREIGN KEY (child) REFERENCES commits (chash) ON DELETE CASCADE
) STRICT;

CREATE TABLE refs (
    name    TEXT NOT NULL PRIMARY KEY,
    chash   TEXT NOT NULL,
    tracked INT  NOT NULL,
    FOREIGN KEY (chash) REFERENCES commits (chash) ON DELETE CASCADE
) STRICT;

CREATE TABLE history (
    position INT  NOT NULL PRIMARY KEY,
    chash    TEXT NOT NULL UNIQUE,
    FOREIGN KEY (chash) REFERENCES commits (chash)
) STRICT;

CREATE TABLE queue (
    chash       TEXT NOT NULL PRIMARY KEY,
    queued_time TEXT NOT NULL,
    priority    INT  NOT NULL DEFAULT 0,
    FOREIGN KEY (chash) REFERENCES commits (chash)
) STRICT;

CREATE TABLE metrics (
    name      TEXT NOT NULL PRIMARY KEY,
    direction INT  NOT NULL,
    CHECK (-1 <= direction AND direction <= 1)
) STRICT;

CREATE TABLE runs (
    chash       TEXT NOT NULL PRIMARY KEY,
    chash_bench TEXT NOT NULL,
    start_time  TEXT NOT NULL,
    end_time    TEXT NOT NULL,
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
