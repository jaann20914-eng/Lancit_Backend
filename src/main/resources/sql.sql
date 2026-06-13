SELECT CURRENT_USER();
SHOW GRANTS;
-- user 만들기
CREATE USER 'lancit'@'localhost'
IDENTIFIED BY 'lancit602!!';
-- 권한 부여
GRANT ALL PRIVILEGES ON *.* TO 'lancit'@'localhost';
FLUSH PRIVILEGES;
-- 확인
SELECT User, Host
FROM mysql.user;

-- 데이터베이스 만들기
CREATE DATABASE lancit;
use lancit;

delete from file where file_id=49;
SELECT * FROM file;
ALTER TABLE `file`
MODIFY COLUMN parent_type 
ENUM('PORTFOLIO','PROFILE','PORTFOLIO_BANNER','PORTFOLIO_FILE','RECRUITMENT_IMAGE','CONTRACT','CHAT','TEMP') NOT NULL;

SELECT * FROM user;
-- test 계정 비번은 test
ALTER TABLE user ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP;
INSERT INTO `user` (email, password, name, phone, job_category, pushable, profile_file_id)
VALUES (
    'test３@lancit.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LjZAzsuPPmC', -- test1234
    '테스트유저３',
    '010-1234-5678',
    'IT',
    0,
    NULL
);


SELECT * FROM company;
INSERT INTO company (email, password, name, company_name, phone, job_category, pushable)
VALUES ('company@lancit.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LjZAzsuPPmC', '담당자', '테스트회사', '010-9876-5432', 'IT', 0);
-- 비밀번호 : test1234
-- 폴리테루 :polyteru

SELECT * FROM category;
SELECT * FROM task;
SELECT * FROM holiday;

SELECT * FROM portfolio;
INSERT INTO portfolio (email, category, title, summary, content, work_start_at, work_end_at, is_public, banner_file_id)
VALUES ('test@lancit.com', 'WEB_APP', '테스트 포트폴리오', '테스트 요약', '포트폴리오 내용', '2026-01-01 00:00:00', '2026-06-01 00:00:00', 0, NULL);

SELECT * FROM recruitment;
SELECT * FROM recruitment_tech_stack;
SELECT * FROM recruitment_application;
SELECT * FROM portfolio_permission;
SELECT * FROM bookmark;
SELECT * FROM recruitment_bookmark;
SELECT * FROM contract;
SELECT * FROM chat_room;
SELECT * FROM message;
SELECT * FROM proposal;
SELECT * FROM file_delete_queue;



-- ============================================================
--  LANCIT DDL  최종본 v3
--  생성 순서대로 실행할 것
-- ============================================================

-- ============================================================
--  1. file
-- ============================================================
CREATE TABLE `file` (
    file_id         INT             NOT NULL    AUTO_INCREMENT,
    user_email      VARCHAR(255)    NULL                        COMMENT '업로더 이메일 (프리랜서)',
    company_email   VARCHAR(255)    NULL                        COMMENT '업로더 이메일 (회사)',
    sys_name        VARCHAR(255)    NOT NULL                    COMMENT '시스템 파일명 (UUID)',
    ori_name        VARCHAR(255)    NOT NULL                    COMMENT '원본 파일명',
    parent_type     ENUM('PORTFOLIO','PROFILE','PORTFOLIO_BANNER','PORTFOLIO_FILE','RECRUITMENT_IMAGE','CONTRACT','CHAT','TEMP')
                                    NOT NULL                    COMMENT '부모 타입',
    parent_id       INT             NULL                        COMMENT '부모 ID (PROFILE은 null)',
    upload_path     VARCHAR(500)    NOT NULL                    COMMENT 'GCS 오브젝트 경로',
    created_at      DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    file_size       INT             NOT NULL                    COMMENT '파일 크기 (bytes)',
    PRIMARY KEY (file_id),
    CONSTRAINT chk_file_owner
        CHECK (
            (user_email IS NOT NULL AND company_email IS NULL)
            OR
            (user_email IS NULL AND company_email IS NOT NULL)
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='파일';

-- ============================================================
--  2. user (프리랜서)
-- ============================================================
CREATE TABLE `user` (
    email               VARCHAR(255)    NOT NULL                    COMMENT '이메일 (PK)',
    password            VARCHAR(255)    NOT NULL                    COMMENT 'BCrypt 암호화',
    name                VARCHAR(100)    NOT NULL,
    phone               VARCHAR(20)     NOT NULL,
    job_category        ENUM('DESIGN','IT','MUSIC','EDUCATION','VIDEO','MARKETING','WRITING','ETC')
                                        NOT NULL,
    pushable            TINYINT(1)      NOT NULL    DEFAULT 0       COMMENT '알림 수신 여부',
    profile_file_id     INT             NULL                        COMMENT '프로필 사진 파일 ID',
    PRIMARY KEY (email),
    CONSTRAINT fk_user_profile_file
        FOREIGN KEY (profile_file_id) REFERENCES `file` (file_id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='프리랜서 유저';

-- ============================================================
--  3. company (회사)
-- ============================================================
CREATE TABLE `company` (
    email                       VARCHAR(255)    NOT NULL                COMMENT '이메일 (PK)',
    password                    VARCHAR(255)    NOT NULL                COMMENT 'BCrypt 암호화',
    name                        VARCHAR(100)    NOT NULL                COMMENT '담당자 이름',
    company_name                VARCHAR(255)    NOT NULL,
    phone                       VARCHAR(20)     NOT NULL,
    job_category                ENUM('DESIGN','IT','MUSIC','EDUCATION','VIDEO','MARKETING','WRITING','ETC')
                                                NOT NULL,
    pushable                    TINYINT(1)      NOT NULL    DEFAULT 0   COMMENT '알림 수신 여부',
    business_number             VARCHAR(20)     NULL,
    business_number_verified    TINYINT(1)      NOT NULL    DEFAULT 0,
    profile_file_id             INT             NULL                    COMMENT '프로필 사진 파일 ID',
    PRIMARY KEY (email),
    CONSTRAINT fk_company_profile_file
        FOREIGN KEY (profile_file_id) REFERENCES `file` (file_id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회사 유저';

-- ============================================================
--  file → user / company FK (순환 참조 해소)
-- ============================================================
ALTER TABLE `file`
    ADD CONSTRAINT fk_file_user
        FOREIGN KEY (user_email) REFERENCES `user` (email)
        ON DELETE CASCADE,
    ADD CONSTRAINT fk_file_company
        FOREIGN KEY (company_email) REFERENCES `company` (email)
        ON DELETE CASCADE;

-- ============================================================
--  4. category (캘린더 카테고리 - 프리랜서/회사 공용)
-- ============================================================
CREATE TABLE `category` (
    category_id     INT             NOT NULL    AUTO_INCREMENT,
    email           VARCHAR(255)    NOT NULL                    COMMENT '소유자 이메일',
    owner_type      ENUM('USER','COMPANY')
                                    NOT NULL                    COMMENT '소유자 타입',
    category_name   VARCHAR(100)    NOT NULL,
    color           VARCHAR(7)      NOT NULL                    COMMENT 'hex 코드 (#FF5733)',
    PRIMARY KEY (category_id),
    INDEX idx_category_owner (email, owner_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='캘린더 카테고리';

-- ============================================================
--  5. task (캘린더 일정)
-- ============================================================
CREATE TABLE `task` (
    task_id                 INT             NOT NULL    AUTO_INCREMENT,
    email                   VARCHAR(255)    NOT NULL                    COMMENT '소유자 이메일',
    owner_type              ENUM('USER','COMPANY')
                                            NOT NULL                    COMMENT '소유자 타입',
    category_id             INT             NOT NULL,
    title                   VARCHAR(255)    NOT NULL,
    content                 TEXT            NULL,
    memo                    TEXT            NULL                        COMMENT '장소, 링크, 준비물 등 부가 정보',
    status                  ENUM('IN_PROGRESS','COMPLETED','CANCELLED')
                                            NOT NULL    DEFAULT 'IN_PROGRESS',
    start_at                DATETIME        NOT NULL,
    end_at                  DATETIME        NOT NULL,
    client_company          VARCHAR(255)    NULL                        COMMENT '의뢰 회사',
    budget                  INT             NOT NULL    DEFAULT 0,
    paid_at                 DATETIME        NULL                        COMMENT 'null = 미입금',
    auto_registered         TINYINT(1)      NOT NULL    DEFAULT 0,
    auto_registered_source  TEXT            NULL,
    PRIMARY KEY (task_id),
    CONSTRAINT fk_task_category
        FOREIGN KEY (category_id) REFERENCES `category` (category_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='캘린더 일정';

-- ============================================================
--  6. holiday (공휴일 - 배치 수집)
-- ============================================================
CREATE TABLE `holiday` (
    id          BIGINT          NOT NULL    AUTO_INCREMENT,
    date        DATE            NOT NULL                        COMMENT '공휴일 날짜',
    name        VARCHAR(50)     NOT NULL                        COMMENT '공휴일 명칭',
    year        INT             NOT NULL                        COMMENT '연도',
    is_holiday  TINYINT(1)      NOT NULL    DEFAULT 1           COMMENT '1=공휴일, 0=대체공휴일',
    PRIMARY KEY (id),
    UNIQUE KEY uk_holiday_date (date),
    INDEX idx_holiday_year (year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공휴일 정보 (캘린더 빨간 날 표시용)';

-- ============================================================
--  7. portfolio (포트폴리오 - 프리랜서 전용)
-- ============================================================
CREATE TABLE `portfolio` (
    portfolio_id    INT             NOT NULL    AUTO_INCREMENT,
    email           VARCHAR(255)    NOT NULL,
    category        ENUM('WEB_APP','DESIGN','BRANDING','MARKETING','PLANNING')
                                    NOT NULL    DEFAULT 'WEB_APP',
    title           VARCHAR(255)    NOT NULL,
    summary         VARCHAR(30)     NOT NULL    DEFAULT ''       COMMENT '한줄 소개',
    content         TEXT            NULL,
    work_start_at   DATETIME        NULL,
    work_end_at     DATETIME        NULL,
    is_public       TINYINT(1)      NOT NULL    DEFAULT 0,
    banner_file_id  INT             NULL,
    created_at      DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1)      NOT NULL    DEFAULT 0       COMMENT 'soft delete',
    deleted_at      DATETIME        NULL,
    PRIMARY KEY (portfolio_id),
    CONSTRAINT fk_portfolio_user
        FOREIGN KEY (email) REFERENCES `user` (email)
        ON DELETE CASCADE,
    CONSTRAINT fk_portfolio_banner
        FOREIGN KEY (banner_file_id) REFERENCES `file` (file_id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='포트폴리오';

-- ============================================================
--  7-1. portfolio_profile (포트폴리오 프로필 카드)
-- ============================================================
CREATE TABLE `portfolio_profile` (
    freelancer_email       VARCHAR(255)    NOT NULL,
    is_portfolio_public    TINYINT(1)      NOT NULL    DEFAULT 0,
    short_intro            VARCHAR(30)     NOT NULL    DEFAULT '',
    created_at             DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (freelancer_email),
    CONSTRAINT fk_portfolio_profile_user
        FOREIGN KEY (freelancer_email) REFERENCES `user` (email)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='포트폴리오 프로필 카드';

-- ============================================================
--  7-2. portfolio_profile_tech_stack (포트폴리오 프로필 기술 스택)
-- ============================================================
CREATE TABLE `portfolio_profile_tech_stack` (
    id                 BIGINT          NOT NULL    AUTO_INCREMENT,
    freelancer_email   VARCHAR(255)    NOT NULL,
    tech_stack         VARCHAR(100)    NOT NULL,
    created_at         DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_portfolio_profile_tech_stack (freelancer_email, tech_stack),
    CONSTRAINT fk_profile_tech_stack_user
        FOREIGN KEY (freelancer_email) REFERENCES `user` (email)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='포트폴리오 프로필 기술 스택';

-- ============================================================
--  8. recruitment (공고문 - 회사 전용)
-- ============================================================
CREATE TABLE `recruitment` (
    recruitment_id          INT             NOT NULL    AUTO_INCREMENT,
    company_email           VARCHAR(255)    NOT NULL                    COMMENT '회사 이메일',
    title                   VARCHAR(255)    NOT NULL,
    summary                 VARCHAR(100)    NOT NULL                    COMMENT '한줄 소개',
    content                 TEXT            NOT NULL,
    requirements            TEXT            NULL,
    job_category            ENUM('DESIGN','IT','MUSIC','EDUCATION','VIDEO','MARKETING','WRITING','ETC')
                                            NOT NULL                    COMMENT '공고 분야',
    recruitment_category    ENUM('WEB_APP','DESIGN','BRANDING','MARKETING','PLANNING')
                                            NOT NULL                    COMMENT '공고 카테고리',
    status                  ENUM('OPEN','CLOSED','CANCELLED')
                                            NOT NULL    DEFAULT 'OPEN'  COMMENT 'EXPIRED는 조회 시 계산',
    work_location           VARCHAR(255)    NULL,
    budget                  INT             NOT NULL    DEFAULT 0,
    image_file_id           INT             NULL,
    contract_start_at       DATETIME        NULL                        COMMENT '예상 시작일',
    contract_end_at         DATETIME        NULL                        COMMENT '예상 종료일',
    recruitment_start_at    DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    recruitment_end_at      DATETIME        NULL,
    created_at              DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted              TINYINT(1)      NOT NULL    DEFAULT 0,
    deleted_at              DATETIME        NULL,
    PRIMARY KEY (recruitment_id),
    INDEX idx_recruitment_list (is_deleted, status, recruitment_end_at, created_at),
    INDEX idx_recruitment_company (company_email, is_deleted, created_at),
    CONSTRAINT fk_recruitment_company
        FOREIGN KEY (company_email) REFERENCES `company` (email)
        ON DELETE CASCADE,
    CONSTRAINT fk_recruitment_image_file
        FOREIGN KEY (image_file_id) REFERENCES `file` (file_id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공고문';

-- ============================================================
--  9. recruitment_tech_stack (공고 기술 스택)
-- ============================================================
CREATE TABLE `recruitment_tech_stack` (
    recruitment_id  INT             NOT NULL,
    tag_name        VARCHAR(50)     NOT NULL,
    created_at      DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (recruitment_id, tag_name),
    CONSTRAINT fk_recruitment_tech_stack_recruitment
        FOREIGN KEY (recruitment_id) REFERENCES `recruitment` (recruitment_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공고 기술 스택';

-- ============================================================
--  10. recruitment_application (공고문 지원)
-- ============================================================
CREATE TABLE `recruitment_application` (
    application_id              INT             NOT NULL    AUTO_INCREMENT,
    recruitment_id              INT             NOT NULL,
    applicant_email             VARCHAR(255)    NOT NULL,
    intro                       TEXT            NULL                        COMMENT '지원 소개',
    applied_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    canceled_at                 DATETIME        NULL,
    status                      ENUM('PENDING','ACCEPTED','REJECTED','CANCELLED')
                                                NOT NULL    DEFAULT 'PENDING',
    is_bookmarked_by_company    TINYINT(1)      NOT NULL    DEFAULT 0,
    viewed_at                   DATETIME        NULL,
    PRIMARY KEY (application_id),
    UNIQUE KEY uk_application_recruitment_applicant (recruitment_id, applicant_email),
    CONSTRAINT fk_application_recruitment
        FOREIGN KEY (recruitment_id) REFERENCES `recruitment` (recruitment_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_application_user
        FOREIGN KEY (applicant_email) REFERENCES `user` (email)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공고문 지원';

-- ============================================================
--  11. portfolio_permission (포트폴리오 열람 권한)
-- ============================================================
CREATE TABLE `portfolio_permission` (
    permission_id   INT         NOT NULL    AUTO_INCREMENT,
    application_id  INT         NOT NULL,
    portfolio_id    INT         NOT NULL,
    is_public       TINYINT(1)  NOT NULL    DEFAULT 0,
    PRIMARY KEY (permission_id),
    UNIQUE KEY uk_permission_application_portfolio (application_id, portfolio_id),
    CONSTRAINT fk_permission_application
        FOREIGN KEY (application_id) REFERENCES `recruitment_application` (application_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_permission_portfolio
        FOREIGN KEY (portfolio_id) REFERENCES `portfolio` (portfolio_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='포트폴리오 열람 권한';

-- ============================================================
--  12. bookmark (회사가 프리랜서 찜 - CompanyBookmark)
-- ============================================================
CREATE TABLE `bookmark` (
    bookmark_id         INT             NOT NULL    AUTO_INCREMENT,
    company_email       VARCHAR(255)    NOT NULL,
    freelancer_email    VARCHAR(255)    NOT NULL,
    application_id      INT             NULL                    COMMENT 'null = 직접 찜',
    bookmarked_at       DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (bookmark_id),
    CONSTRAINT fk_bookmark_company
        FOREIGN KEY (company_email) REFERENCES `company` (email)
        ON DELETE CASCADE,
    CONSTRAINT fk_bookmark_user
        FOREIGN KEY (freelancer_email) REFERENCES `user` (email)
        ON DELETE CASCADE,
    CONSTRAINT fk_bookmark_application
        FOREIGN KEY (application_id) REFERENCES `recruitment_application` (application_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회사가 프리랜서 찜';

-- ============================================================
--  13. recruitment_bookmark (프리랜서가 공고 찜 - FreelancerBookmark)
-- ============================================================
CREATE TABLE `recruitment_bookmark` (
    id               BIGINT          NOT NULL    AUTO_INCREMENT,
    freelancer_email VARCHAR(255)    NOT NULL,
    recruitment_id   INT             NOT NULL,
    bookmarked_at    DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_freelancer_recruitment (freelancer_email, recruitment_id),
    CONSTRAINT fk_rb_user
        FOREIGN KEY (freelancer_email) REFERENCES `user` (email)
        ON DELETE CASCADE,
    CONSTRAINT fk_rb_recruitment
        FOREIGN KEY (recruitment_id) REFERENCES `recruitment` (recruitment_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='프리랜서가 공고문 찜';

-- ============================================================
--  14. contract (계약서)
-- ============================================================
CREATE TABLE `contract` (
    contract_id                 INT             NOT NULL    AUTO_INCREMENT,
    company_email               VARCHAR(255)    NOT NULL,
    freelancer_email            VARCHAR(255)    NOT NULL,
    status                      ENUM('NEGOTIATING_A','NEGOTIATING_B','NEGOTIATING_C',
                                     'IN_PROGRESS','COMPLETED_PENDING','COMPLETED','CANCELLED')
                                                NOT NULL    DEFAULT 'NEGOTIATING_A',
    party_a                     VARCHAR(255)    NULL                    COMMENT '갑 (회사명)',
    party_b                     VARCHAR(100)    NULL                    COMMENT '을 (프리랜서 이름)',
    contract_start_at           DATETIME        NULL,
    contract_end_at             DATETIME        NULL,
    work_location               VARCHAR(255)    NULL,
    work_description            TEXT            NULL,
    work_days                   VARCHAR(100)    NULL                    COMMENT '콤마 구분 (MON,TUE,...)',
    work_start_time             TIME            NULL,
    work_end_time               TIME            NULL,
    break_time                  TIME            NULL,
    monthly_wage                INT             NOT NULL    DEFAULT 0,
    base_pay                    INT             NOT NULL    DEFAULT 0,
    base_pay_basis              TIME            NULL,
    overtime_pay                INT             NOT NULL    DEFAULT 0,
    overtime_pay_basis          TIME            NULL,
    holiday_pay                 INT             NOT NULL    DEFAULT 0,
    holiday_pay_basis           TIME            NULL,
    meal_allowance              INT             NOT NULL    DEFAULT 0,
    total_wage                  INT             NOT NULL    DEFAULT 0,
    contract_written_at         DATETIME        NULL,
    representative_name         VARCHAR(100)    NULL,
    company_address             VARCHAR(500)    NULL,
    representative_sign_file_id INT             NULL,
    freelancer_sign_file_id     INT             NULL,
    freelancer_birth_date       DATE            NULL,
    freelancer_address          VARCHAR(500)    NULL,
    confirm_sign_file_id        INT             NULL,
    PRIMARY KEY (contract_id),
    CONSTRAINT fk_contract_company
        FOREIGN KEY (company_email) REFERENCES `company` (email)
        ON DELETE RESTRICT,
    CONSTRAINT fk_contract_user
        FOREIGN KEY (freelancer_email) REFERENCES `user` (email)
        ON DELETE RESTRICT,
    CONSTRAINT fk_contract_rep_sign
        FOREIGN KEY (representative_sign_file_id) REFERENCES `file` (file_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_contract_free_sign
        FOREIGN KEY (freelancer_sign_file_id) REFERENCES `file` (file_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_contract_confirm_sign
        FOREIGN KEY (confirm_sign_file_id) REFERENCES `file` (file_id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='계약서';

-- ============================================================
--  15. chat_room (계약서 협의 채팅방)
-- ============================================================
CREATE TABLE `chat_room` (
    chat_room_id        INT             NOT NULL    AUTO_INCREMENT,
    contract_id         INT             NOT NULL,
    freelancer_email    VARCHAR(255)    NOT NULL,
    company_email       VARCHAR(255)    NOT NULL,
    PRIMARY KEY (chat_room_id),
    CONSTRAINT fk_chatroom_contract
        FOREIGN KEY (contract_id) REFERENCES `contract` (contract_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_chatroom_user
        FOREIGN KEY (freelancer_email) REFERENCES `user` (email)
        ON DELETE CASCADE,
    CONSTRAINT fk_chatroom_company
        FOREIGN KEY (company_email) REFERENCES `company` (email)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='협의 채팅방';

-- ============================================================
--  16. message (채팅 메시지)
-- ============================================================
CREATE TABLE `message` (
    message_id      INT             NOT NULL    AUTO_INCREMENT,
    chat_room_id    INT             NOT NULL,
    email           VARCHAR(255)    NOT NULL                    COMMENT '발신자 이메일',
    message         TEXT            NOT NULL,
    message_type    ENUM('TEXT','FILE','IMAGE')
                                    NOT NULL    DEFAULT 'TEXT',
    is_read         TINYINT(1)      NOT NULL    DEFAULT 0,
    is_deleted      TINYINT(1)      NOT NULL    DEFAULT 0       COMMENT 'soft delete',
    is_updated      TINYINT(1)      NOT NULL    DEFAULT 0,
    created_at      DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id),
    CONSTRAINT fk_message_chatroom
        FOREIGN KEY (chat_room_id) REFERENCES `chat_room` (chat_room_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='협의 채팅 메시지';

-- ============================================================
--  17. proposal (제안서 - 회사가 프리랜서에게 발송)
-- ============================================================
CREATE TABLE `proposal` (
    proposal_id         INT             NOT NULL    AUTO_INCREMENT,
    company_email       VARCHAR(255)    NOT NULL,
    freelancer_email    VARCHAR(255)    NOT NULL,
    title               VARCHAR(255)    NOT NULL,
    content             TEXT            NULL,
    status              ENUM('PENDING','ACCEPTED','REJECTED')
                                        NOT NULL    DEFAULT 'PENDING',
    sent_at             DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (proposal_id),
    CONSTRAINT fk_proposal_company
        FOREIGN KEY (company_email) REFERENCES `company` (email)
        ON DELETE CASCADE,
    CONSTRAINT fk_proposal_user
        FOREIGN KEY (freelancer_email) REFERENCES `user` (email)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='제안서';


-- ============================================================
--  18. 삭제 실패한 파일 목록 저장해 놓는 곳
-- ============================================================
CREATE TABLE file_delete_queue (
    file_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    upload_path VARCHAR(500) NOT NULL,
    retry_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);




-- ============================================================
--  테이블 생성 순서 요약
-- ============================================================
--   1.  file
--   2.  user             (→ file)
--   3.  company          (→ file)
--   ALTER file           (→ user, → company)
--   4.  category
--   5.  task             (→ category)
--   6.  holiday
--   7.  portfolio        (→ user, → file)
--   7-1. portfolio_profile             (→ user)
--   7-2. portfolio_profile_tech_stack  (→ user)
--   8.  recruitment      (→ company)
--   9.  recruitment_tech_stack    (→ recruitment)
--  10.  recruitment_application   (→ recruitment, → user)
--  11.  portfolio_permission      (→ recruitment_application, → portfolio)
--  12.  bookmark                  (→ company, → user, → recruitment_application)
--  13.  recruitment_bookmark      (→ user, → recruitment)
--  14.  contract         (→ company, → user, → file×3)
--  15.  chat_room        (→ contract, → user, → company)
--  16.  message          (→ chat_room)
--  17.  proposal         (→ company, → user)
--  18.  file_delete_queue
-- ============================================================
