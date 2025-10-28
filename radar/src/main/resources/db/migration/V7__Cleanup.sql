CREATE TABLE metrics_new (
    metric TEXT NOT NULL PRIMARY KEY,
    unit   TEXT
) STRICT;

INSERT INTO metrics_new
SELECT metric, unit
FROM metrics;

DROP TABLE metrics;

ALTER TABLE metrics_new
    RENAME TO metrics;
