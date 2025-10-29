CREATE TABLE metrics_new (
    metric TEXT NOT NULL PRIMARY KEY,
    unit   TEXT
) STRICT;

CREATE TABLE measurements_new (
    chash  TEXT NOT NULL,
    metric TEXT NOT NULL,
    value  REAL NOT NULL,
    source TEXT,
    PRIMARY KEY (chash, metric),
    FOREIGN KEY (chash) REFERENCES commits (chash) ON DELETE CASCADE,
    FOREIGN KEY (metric) REFERENCES metrics_new (metric) ON DELETE CASCADE
) STRICT;

INSERT INTO metrics_new
SELECT metric, unit
FROM metrics;

INSERT INTO measurements_new
SELECT chash, metric, value, source
FROM measurements;

DROP TABLE measurements;
DROP TABLE metrics;

ALTER TABLE metrics_new
    RENAME TO metrics;
ALTER TABLE measurements_new
    RENAME TO measurements;
