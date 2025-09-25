-- Add fields "script_start_time", "script_end_time" to table "runs"

CREATE TABLE runs_new (
    chash             TEXT NOT NULL,
    name              TEXT NOT NULL,
    script            TEXT NOT NULL,
    runner            TEXT NOT NULL,
    chash_bench       TEXT NOT NULL,
    start_time        TEXT NOT NULL,
    end_time          TEXT NOT NULL,
    script_start_time TEXT,
    script_end_time   TEXT,
    exit_code         INT  NOT NULL,
    PRIMARY KEY (chash, name),
    FOREIGN KEY (chash) REFERENCES commits (chash) ON DELETE CASCADE
) STRICT;

INSERT INTO runs_new
SELECT chash,
       name,
       script,
       runner,
       chash_bench,
       start_time,
       end_time,
       start_time,
       end_time,
       exit_code
FROM runs;

DROP TABLE runs;
ALTER TABLE runs_new
    RENAME TO runs;
