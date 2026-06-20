-- Existing LANCIT database migration for support profile cards and application snapshots.

ALTER TABLE `file`
    MODIFY COLUMN parent_type
        ENUM('PORTFOLIO','PROFILE','PORTFOLIO_PROFILE','PORTFOLIO_BANNER','PORTFOLIO_FILE','RECRUITMENT_IMAGE','CONTRACT','CHAT','TEMP')
        NOT NULL COMMENT '부모 타입';

ALTER TABLE `portfolio_profile`
    ADD COLUMN display_name VARCHAR(100) NULL AFTER freelancer_email,
    ADD COLUMN job_category ENUM('DESIGN','IT','MUSIC','EDUCATION','VIDEO','MARKETING','WRITING','ETC') NULL AFTER display_name,
    ADD COLUMN profile_file_id INT NULL AFTER job_category,
    ADD COLUMN description VARCHAR(200) NOT NULL DEFAULT '' AFTER short_intro;

UPDATE portfolio_profile p
INNER JOIN user u ON u.email = p.freelancer_email
SET
    p.display_name = u.name,
    p.job_category = u.job_category,
    p.profile_file_id = u.profile_file_id
WHERE p.display_name IS NULL
   OR p.job_category IS NULL;

ALTER TABLE `portfolio_profile`
    MODIFY COLUMN display_name VARCHAR(100) NOT NULL,
    MODIFY COLUMN job_category ENUM('DESIGN','IT','MUSIC','EDUCATION','VIDEO','MARKETING','WRITING','ETC') NOT NULL,
    ADD CONSTRAINT fk_portfolio_profile_file
        FOREIGN KEY (profile_file_id) REFERENCES `file` (file_id)
        ON DELETE SET NULL;

CREATE TABLE `recruitment_application_profile_snapshot` (
    application_id             INT             NOT NULL,
    display_name               VARCHAR(100)    NOT NULL,
    job_category               ENUM('DESIGN','IT','MUSIC','EDUCATION','VIDEO','MARKETING','WRITING','ETC') NOT NULL,
    profile_file_id            INT             NULL,
    short_intro                VARCHAR(30)     NOT NULL    DEFAULT '',
    description                VARCHAR(200)    NOT NULL    DEFAULT '',
    is_portfolio_public        TINYINT(1)      NOT NULL    DEFAULT 0,
    source_profile_updated_at  DATETIME        NULL,
    created_at                 DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (application_id),
    CONSTRAINT fk_application_profile_snapshot_application
        FOREIGN KEY (application_id) REFERENCES `recruitment_application` (application_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_application_profile_snapshot_file
        FOREIGN KEY (profile_file_id) REFERENCES `file` (file_id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='지원 당시 포트폴리오 프로필 카드';

CREATE TABLE `recruitment_application_profile_snapshot_tech_stack` (
    application_id   INT             NOT NULL,
    tech_stack       VARCHAR(100)    NOT NULL,
    sort_order       INT             NOT NULL,
    PRIMARY KEY (application_id, tech_stack),
    UNIQUE KEY uk_application_profile_snapshot_tech_order (application_id, sort_order),
    CONSTRAINT fk_application_profile_snapshot_tech_application
        FOREIGN KEY (application_id) REFERENCES `recruitment_application` (application_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='지원 당시 프로필 기술 스택';
