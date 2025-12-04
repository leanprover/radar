CREATE TABLE github_command_new (
    owner                      TEXT NOT NULL,
    repo                       TEXT NOT NULL,
    number                     INT  NOT NULL,
    is_pr                      INT  NOT NULL,

    command_id_long            INT  NOT NULL,
    command_created_time       TEXT NOT NULL,
    command_updated_time       TEXT NOT NULL,
    command_author_id_long     INT  NOT NULL,
    command_author_login       TEXT NOT NULL,
    command_author_association TEXT NOT NULL,
    command_body               TEXT NOT NULL,

    reply_id_long              INT           DEFAULT NULL,
    reply_body                 TEXT          DEFAULT NULL,
    reply_tries                INT           DEFAULT NULL,

    -- 0 = command waiting, check status regularly and allow edits
    -- 1 = command failed, no need to check but allow edits
    -- 2 = command succeeded, no need to check and no edits allowed
    status                     INT  NOT NULL DEFAULT 0,

    PRIMARY KEY (owner, repo, command_id_long)
) STRICT;

CREATE TABLE github_command_running (
    owner           TEXT NOT NULL,
    repo            TEXT NOT NULL,
    command_id_long INT  NOT NULL,

    in_repo         TEXT DEFAULT NULL, -- NULL = this repo
    chash_first     TEXT NOT NULL,
    chash_second    TEXT NOT NULL,

    started_time    TEXT NOT NULL,
    completed_time  TEXT DEFAULT NULL,

    PRIMARY KEY (owner, repo, command_id_long),
    FOREIGN KEY (owner, repo, command_id_long) REFERENCES github_command_new ON DELETE CASCADE
) STRICT;

INSERT INTO github_command_new
SELECT owner,
       repo,
       comment_issue_number,
       EXISTS(SELECT 1
              FROM github_command_resolved
              WHERE github_command_resolved.owner = github_command.owner
                AND github_command_resolved.repo = github_command.repo
                AND github_command_resolved.comment_id_long = github_command.comment_id_long),

       comment_id_long,
       comment_created_time,
       comment_created_time,
       comment_author_id_long,
       comment_author_login,
       comment_author_association,
       comment_body,

       reply_id_long,
       reply_content,
       reply_tries,

       2
FROM github_command;

INSERT INTO github_command_running
SELECT owner,
       repo,
       comment_id_long,

       NULL,
       against_chash,
       chash,

       comment_created_time,
       completed_time
FROM github_command_resolved
         JOIN github_command USING (owner, repo, comment_id_long);

DROP TABLE github_command;
DROP TABLE github_command_resolved;

ALTER TABLE github_command_new
    RENAME TO github_command;
