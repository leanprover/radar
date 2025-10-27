CREATE TABLE queue_seen (
    chash TEXT NOT NULL PRIMARY KEY
        REFERENCES commits ON DELETE CASCADE
) STRICT;

INSERT INTO queue_seen
SELECT chash
FROM commits
WHERE seen != 0;

ALTER TABLE commits
    DROP COLUMN seen;
