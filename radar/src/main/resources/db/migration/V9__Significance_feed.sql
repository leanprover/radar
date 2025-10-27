CREATE TABLE significance_feed (
    chash       TEXT NOT NULL PRIMARY KEY,
    significant INT,
    FOREIGN KEY (chash) REFERENCES commits ON DELETE CASCADE
) STRICT;
