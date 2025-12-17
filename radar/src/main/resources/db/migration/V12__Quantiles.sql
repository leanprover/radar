CREATE TABLE quantile_last_updated (
    last_updated_time TEXT NOT NULL
) STRICT;

CREATE TABLE quantile (
    metric TEXT NOT NULL PRIMARY KEY REFERENCES metrics,
    value  REAL NOT NULL
) STRICT;
