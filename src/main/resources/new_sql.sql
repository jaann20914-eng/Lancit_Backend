-- ============================================================
--  LANCIT DDL  최종 통합본
-- ============================================================

USE lancit;

-- ============================================================
--  기존 테이블 전체 DROP (역순 의존성 고려, FK 체크 해제)
-- ============================================================
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS contract_cancel_request;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS proposal;
DROP TABLE IF EXISTS message_file;
DROP TABLE IF EXISTS message;
DROP TABLE IF EXISTS chat_room;
DROP TABLE IF EXISTS contract_file;
DROP TABLE IF EXISTS contract_document;
DROP TABLE IF EXISTS contract;
DROP TABLE IF EXISTS recruitment_bookmark;
DROP TABLE IF EXISTS bookmark;
DROP TABLE IF EXISTS portfolio_permission;
DROP TABLE IF EXISTS recruitment_application;
DROP TABLE IF EXISTS recruitment;
DROP TABLE IF EXISTS portfolio;
DROP TABLE IF EXISTS holiday;
DROP TABLE IF EXISTS task;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS file_delete_queue;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `company`;
DROP TABLE IF EXISTS `file`;

SET FOREIGN_KEY_CHECKS = 1;


-- ============================================================
--  1. file
-- ============================================================
CREATE TABLE `file` (
    file_id         INT             NOT NULL    AUTO_INCREMENT,
    user_email      VARCHAR(255)    NULL                        COMMENT '업로더 이메일 (프리랜서)',
    company_email   VARCHAR(255)    NULL                        COMMENT '업로더 이메일 (회사)',
    sys_name        VARCHAR(255)    NOT NULL                    COMMENT '시스템 파일명 (UUID)',
    ori_name        VARCHAR(255)    NOT NULL                    COMMENT '원본 파일명',
    parent_type     ENUM('PROFILE','PORTFOLIO_BANNER','PORTFOLIO_FILE','CONTRACT','CHAT','TEMP')
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
    created_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
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
--  4. category
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
--  5. task
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
--  6. holiday
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공휴일 정보';

-- ============================================================
--  7. portfolio
-- ============================================================
CREATE TABLE `portfolio` (
    portfolio_id    INT             NOT NULL    AUTO_INCREMENT,
    email           VARCHAR(255)    NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    content         TEXT            NULL,
    work_start_at   DATETIME        NULL,
    work_end_at     DATETIME        NULL,
    is_public       TINYINT(1)      NOT NULL    DEFAULT 0,
    banner_file_id  INT             NULL,
    PRIMARY KEY (portfolio_id),
    CONSTRAINT fk_portfolio_user
        FOREIGN KEY (email) REFERENCES `user` (email)
        ON DELETE CASCADE,
    CONSTRAINT fk_portfolio_banner
        FOREIGN KEY (banner_file_id) REFERENCES `file` (file_id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='포트폴리오';

-- ============================================================
--  8. recruitment
-- ============================================================
CREATE TABLE `recruitment` (
    recruitment_id      INT             NOT NULL    AUTO_INCREMENT,
    email               VARCHAR(255)    NOT NULL                    COMMENT '회사 이메일',
    title               VARCHAR(255)    NOT NULL,
    content             TEXT            NULL,
    job_category        ENUM('DESIGN','IT','MUSIC','EDUCATION','VIDEO','MARKETING','WRITING','ETC')
                                        NOT NULL,
    status              ENUM('OPEN','CLOSED','CANCELLED')
                                        NOT NULL    DEFAULT 'OPEN',
    created_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    contract_start_at   DATETIME        NULL,
    contract_end_at     DATETIME        NULL,
    budget              INT             NOT NULL    DEFAULT 0,
    PRIMARY KEY (recruitment_id),
    CONSTRAINT fk_recruitment_company
        FOREIGN KEY (email) REFERENCES `company` (email)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공고문';

-- ============================================================
--  9. recruitment_application
-- ============================================================
CREATE TABLE `recruitment_application` (
    application_id              INT             NOT NULL    AUTO_INCREMENT,
    recruitment_id              INT             NOT NULL,
    applicant_email             VARCHAR(255)    NOT NULL,
    applied_at                  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    status                      ENUM('PENDING','ACCEPTED','REJECTED')
                                                NOT NULL    DEFAULT 'PENDING',
    is_bookmarked_by_company    TINYINT(1)      NOT NULL    DEFAULT 0,
    PRIMARY KEY (application_id),
    CONSTRAINT fk_application_recruitment
        FOREIGN KEY (recruitment_id) REFERENCES `recruitment` (recruitment_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_application_user
        FOREIGN KEY (applicant_email) REFERENCES `user` (email)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공고문 지원';

-- ============================================================
--  10. portfolio_permission
-- ============================================================
CREATE TABLE `portfolio_permission` (
    permission_id   INT         NOT NULL    AUTO_INCREMENT,
    application_id  INT         NOT NULL,
    portfolio_id    INT         NOT NULL,
    is_public       TINYINT(1)  NOT NULL    DEFAULT 0,
    PRIMARY KEY (permission_id),
    CONSTRAINT fk_permission_application
        FOREIGN KEY (application_id) REFERENCES `recruitment_application` (application_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_permission_portfolio
        FOREIGN KEY (portfolio_id) REFERENCES `portfolio` (portfolio_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='포트폴리오 열람 권한';

-- ============================================================
--  11. bookmark
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
--  12. recruitment_bookmark
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
--  13. contract  (v3 - 최소 상태정보)
-- ============================================================
CREATE TABLE `contract` (
    contract_id         INT             NOT NULL    AUTO_INCREMENT,
    recruitment_id      INT             NULL,
    company_email       VARCHAR(255)    NOT NULL,
    freelancer_email    VARCHAR(255)    NOT NULL,
    status ENUM(
        'WAITING',
        'NEGOTIATING_A',
        'NEGOTIATING_B',
        'NEGOTIATING_C',
        'IN_PROGRESS',
        'COMPLETED_PENDING',
        'COMPLETED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'WAITING',
    PRIMARY KEY (contract_id),
    CONSTRAINT fk_contract_recruitment
        FOREIGN KEY (recruitment_id) REFERENCES `recruitment` (recruitment_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_contract_company
        FOREIGN KEY (company_email) REFERENCES `company` (email)
        ON DELETE RESTRICT,
    CONSTRAINT fk_contract_user
        FOREIGN KEY (freelancer_email) REFERENCES `user` (email)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='계약';

-- ============================================================
--  14. contract_document (계약서 본문)
-- ============================================================
CREATE TABLE `contract_document` (
    contract_id  INT PRIMARY KEY,

    party_a                     VARCHAR(255),
    representative_name         VARCHAR(100),
    company_address             VARCHAR(500),

    party_b                     VARCHAR(100),
    freelancer_birth_date       DATE,
    freelancer_address          VARCHAR(500),

    work_location               VARCHAR(255),
    work_description            TEXT,
    work_days                   VARCHAR(100),

    work_start_time             TIME,
    work_end_time               TIME,
    break_time                  TIME,

    contract_start_date         DATE,
    contract_end_date           DATE,

    monthly_wage                INT DEFAULT 0,

    base_pay                    INT DEFAULT 0,
    base_pay_basis              TIME,

    overtime_pay                INT DEFAULT 0,
    overtime_pay_basis          TIME,

    holiday_pay                 INT DEFAULT 0,
    holiday_pay_basis           TIME,

    meal_allowance              INT DEFAULT 0,
    total_wage                  INT DEFAULT 0,

    representative_sign_file_id INT NULL,
    freelancer_sign_file_id     INT NULL,

    contract_written_at         DATETIME,
    confirmed_at                DATETIME,

    CONSTRAINT fk_contract_document_contract
        FOREIGN KEY (contract_id) REFERENCES `contract` (contract_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_contract_document_rep_sign
        FOREIGN KEY (representative_sign_file_id) REFERENCES `file` (file_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_contract_document_free_sign
        FOREIGN KEY (freelancer_sign_file_id) REFERENCES `file` (file_id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='계약서 본문';

-- ============================================================
--  15. contract_file
-- ============================================================
CREATE TABLE `contract_file` (
    contract_file_id    INT NOT NULL AUTO_INCREMENT,
    contract_id         INT NOT NULL,
    file_id             INT NOT NULL,
    type ENUM('CONFIRM','PDF') NOT NULL,
    uploader_email      VARCHAR(255) NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (contract_file_id),
    CONSTRAINT fk_contract_file_contract
        FOREIGN KEY (contract_id) REFERENCES `contract` (contract_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_contract_file_file
        FOREIGN KEY (file_id) REFERENCES `file` (file_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='계약 파일';

-- ============================================================
--  16. chat_room
-- ============================================================
CREATE TABLE `chat_room` (
    chat_room_id        INT NOT NULL AUTO_INCREMENT,
    contract_id         INT NOT NULL,
    freelancer_email    VARCHAR(255) NOT NULL,
    company_email       VARCHAR(255) NOT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (chat_room_id),
    UNIQUE KEY uk_chatroom_contract (contract_id),
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
--  17. message
--  ※ message NULL 허용 (파일 전용 메시지 대응)
-- ============================================================
CREATE TABLE `message` (
    message_id      INT NOT NULL AUTO_INCREMENT,
    chat_room_id    INT NOT NULL,
    sender_email    VARCHAR(255) NOT NULL,
    message_type ENUM('TEXT','FILE','IMAGE') NOT NULL DEFAULT 'TEXT',
    message         TEXT NULL,
    is_deleted      TINYINT(1) NOT NULL DEFAULT 0,
    is_updated      TINYINT(1) NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id),
    CONSTRAINT fk_message_chatroom
        FOREIGN KEY (chat_room_id) REFERENCES `chat_room` (chat_room_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='채팅 메시지';

-- ============================================================
--  18. message_file
-- ============================================================
CREATE TABLE `message_file` (
    message_file_id INT NOT NULL AUTO_INCREMENT,
    message_id      INT NOT NULL,
    file_id         INT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_file_id),
    CONSTRAINT fk_message_file_message
        FOREIGN KEY (message_id) REFERENCES `message` (message_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_message_file_file
        FOREIGN KEY (file_id) REFERENCES `file` (file_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='채팅 첨부파일';

-- ============================================================
--  19. proposal (v3 - 연결 테이블)
-- ============================================================
CREATE TABLE `proposal` (
    proposal_id         INT NOT NULL AUTO_INCREMENT,
    company_email       VARCHAR(255) NOT NULL,
    freelancer_email    VARCHAR(255) NOT NULL,
    recruitment_id      INT NOT NULL,
    contract_id         INT NULL,
    chat_room_id        INT NULL,
    PRIMARY KEY (proposal_id),
    CONSTRAINT fk_proposal_company
        FOREIGN KEY (company_email) REFERENCES `company` (email)
        ON DELETE CASCADE,
    CONSTRAINT fk_proposal_user
        FOREIGN KEY (freelancer_email) REFERENCES `user` (email)
        ON DELETE CASCADE,
    CONSTRAINT fk_proposal_recruitment
        FOREIGN KEY (recruitment_id) REFERENCES `recruitment` (recruitment_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_proposal_contract
        FOREIGN KEY (contract_id) REFERENCES `contract` (contract_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_proposal_chatroom
        FOREIGN KEY (chat_room_id) REFERENCES `chat_room` (chat_room_id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='제안';

-- ============================================================
--  20. notification
-- ============================================================
CREATE TABLE `notification` (
    notification_id INT NOT NULL AUTO_INCREMENT,
    receiver_email VARCHAR(255) NOT NULL,
    type ENUM(
        'PROPOSAL',
        'CHAT',
        'CONTRACT_CANCEL_REQUEST',
        'CONTRACT_COMPLETED',
        'CONTRACT_COMPLETED_PENDING',
        'CONFIRM_FILE'
    ) NOT NULL,
    target_id INT NOT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='알림';

-- ============================================================
--  21. contract_cancel_request
-- ============================================================
CREATE TABLE `contract_cancel_request` (
    contract_id     INT NOT NULL,
    requester_email VARCHAR(255) NOT NULL,
    requested_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (contract_id),
    CONSTRAINT fk_cancel_request_contract
        FOREIGN KEY (contract_id) REFERENCES `contract` (contract_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='계약 파기 요청';

-- ============================================================
--  22. file_delete_queue
-- ============================================================
CREATE TABLE `file_delete_queue` (
    file_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    upload_path VARCHAR(500) NOT NULL,
    retry_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='삭제 실패 파일 큐';

-- ============================================================
--  생성 순서 요약
-- ============================================================
--  1. file
--  2. user             (-> file)
--  3. company          (-> file)
--     ALTER file       (-> user, company)
--  4. category
--  5. task             (-> category)
--  6. holiday
--  7. portfolio        (-> user, file)
--  8. recruitment      (-> company)
--  9. recruitment_application (-> recruitment, user)
-- 10. portfolio_permission    (-> recruitment_application, portfolio)
-- 11. bookmark                (-> company, user, recruitment_application)
-- 12. recruitment_bookmark    (-> user, recruitment)
-- 13. contract                (-> recruitment, company, user)
-- 14. contract_document       (-> contract, file x2)
-- 15. contract_file           (-> contract, file)
-- 16. chat_room               (-> contract, user, company)
-- 17. message                 (-> chat_room)
-- 18. message_file            (-> message, file)
-- 19. proposal                (-> company, user, recruitment, contract, chat_room)
-- 20. notification
-- 21. contract_cancel_request (-> contract)
-- 22. file_delete_queue
-- ============================================================

-- ============================================================
--  더미 데이터
--  공통 비밀번호 해시 (test1234)
--  $2a$10$i2FVNHQ6Smfo73oUin6wnOMHCdKxNJHP8PQaA4xsJDaSxpMdXK.8.
-- ============================================================

-- ── 1. user (프리랜서) ──────────────────────────────────────
INSERT INTO `user` (email, password, name, phone, job_category, pushable, profile_file_id) VALUES
('test@lancit.com',  '$2a$10$i2FVNHQ6Smfo73oUin6wnOMHCdKxNJHP8PQaA4xsJDaSxpMdXK.8.', '김프리', '010-1111-1111', 'IT',        1, NULL),
('test2@lancit.com', '$2a$10$i2FVNHQ6Smfo73oUin6wnOMHCdKxNJHP8PQaA4xsJDaSxpMdXK.8.', '이디자인', '010-2222-2222', 'DESIGN',    1, NULL),
('test3@lancit.com', '$2a$10$i2FVNHQ6Smfo73oUin6wnOMHCdKxNJHP8PQaA4xsJDaSxpMdXK.8.', '박영상', '010-3333-3333', 'VIDEO',     0, NULL);

-- ── 2. company (회사) ───────────────────────────────────────
INSERT INTO `company` (email, password, name, company_name, phone, job_category, pushable, business_number, business_number_verified, profile_file_id) VALUES
('company@lancit.com', '$2a$10$i2FVNHQ6Smfo73oUin6wnOMHCdKxNJHP8PQaA4xsJDaSxpMdXK.8.', '담당자A', '테스트회사',     '010-9000-0001', 'IT',        1, '123-45-67890', 1, NULL),
('polyteru@polyteru.com', '$2a$10$i2FVNHQ6Smfo73oUin6wnOMHCdKxNJHP8PQaA4xsJDaSxpMdXK.8.', '담당자B', '폴리테루',     '010-9000-0002', 'MARKETING', 0, NULL,          0, NULL);

-- ── 3. category ─────────────────────────────────────────────
INSERT INTO `category` (email, owner_type, category_name, color) VALUES
('test@lancit.com', 'USER',    '업무',   '#FF5733'),
('test@lancit.com', 'USER',    '개인',   '#33A1FF'),
('company@lancit.com', 'COMPANY', '프로젝트', '#33FF57');

-- ── 4. task ──────────────────────────────────────────────────
INSERT INTO `task` (email, owner_type, category_id, title, content, memo, status, start_at, end_at, client_company, budget, paid_at, auto_registered, auto_registered_source) VALUES
('test@lancit.com', 'USER', 1, '백엔드 개발 작업', 'API 설계 및 구현', '재택근무, Zoom 미팅 매주 월요일', 'IN_PROGRESS', '2026-06-01 09:00:00', '2026-06-30 18:00:00', '테스트회사', 3000000, NULL, 0, NULL),
('test@lancit.com', 'USER', 2, '병원 예약', '정기 검진', '강남역 3번출구', 'COMPLETED', '2026-06-10 14:00:00', '2026-06-10 15:00:00', NULL, 0, NULL, 0, NULL);

-- ── 5. portfolio ────────────────────────────────────────────
INSERT INTO `portfolio` (email, title, content, work_start_at, work_end_at, is_public, banner_file_id) VALUES
('test@lancit.com', '커머스 백엔드 프로젝트', 'Spring Boot 기반 커머스 API 서버 구축', '2025-01-01 00:00:00', '2025-06-01 00:00:00', 1, NULL),
('test2@lancit.com', '브랜드 리뉴얼 디자인', 'CI/BI 및 웹사이트 디자인', '2025-03-01 00:00:00', '2025-05-01 00:00:00', 1, NULL);

-- ── 6. recruitment ──────────────────────────────────────────
INSERT INTO `recruitment` (email, title, content, job_category, status, contract_start_at, contract_end_at, budget) VALUES
('company@lancit.com', '백엔드 개발자 모집',  'Spring Boot 기반 서비스 개발 프리랜서 모집', 'IT',        'OPEN', '2026-07-01 00:00:00', '2026-12-31 00:00:00', 3000000),
('company@lancit.com', '디자이너 모집',       'UI/UX 디자이너 단기 프로젝트',              'DESIGN',    'OPEN', '2026-07-01 00:00:00', '2026-09-30 00:00:00', 2000000),
('polyteru@polyteru.com', '영상 편집자 모집', '유튜브 영상 편집 프리랜서 모집',            'VIDEO',     'OPEN', '2026-07-01 00:00:00', '2026-10-31 00:00:00', 1500000);

-- ── 7. recruitment_application ─────────────────────────────
INSERT INTO `recruitment_application` (recruitment_id, applicant_email, status, is_bookmarked_by_company) VALUES
(1, 'test@lancit.com',  'ACCEPTED', 1),
(2, 'test2@lancit.com', 'PENDING',  0),
(3, 'test3@lancit.com', 'PENDING',  0);

-- ── 8. portfolio_permission ─────────────────────────────────
INSERT INTO `portfolio_permission` (application_id, portfolio_id, is_public) VALUES
(1, 1, 1),
(2, 2, 1);

-- ── 9. bookmark (회사가 프리랜서 찜) ─────────────────────────
INSERT INTO `bookmark` (company_email, freelancer_email, application_id) VALUES
('company@lancit.com', 'test@lancit.com', 1);

-- ── 10. recruitment_bookmark (프리랜서가 공고 찜) ────────────
INSERT INTO `recruitment_bookmark` (freelancer_email, recruitment_id) VALUES
('test2@lancit.com', 1),
('test3@lancit.com', 2);

-- ── 11. contract ─────────────────────────────────────────────
-- contract_id 1: WAITING (제안만 보낸 상태)
-- contract_id 2: IN_PROGRESS (협의 완료, 진행중)
INSERT INTO `contract` (recruitment_id, company_email, freelancer_email, status) VALUES
(1, 'company@lancit.com', 'test@lancit.com', 'WAITING'),
(1, 'company@lancit.com', 'test2@lancit.com', 'IN_PROGRESS');

-- ── 12. contract_document (contract_id=2 진행중 계약의 본문) ─
INSERT INTO `contract_document` (
    contract_id, party_a, representative_name, company_address,
    party_b, freelancer_birth_date, freelancer_address,
    work_location, work_description, work_days,
    work_start_time, work_end_time, break_time,
    contract_start_date, contract_end_date,
    monthly_wage, base_pay, base_pay_basis,
    overtime_pay, overtime_pay_basis,
    holiday_pay, holiday_pay_basis,
    meal_allowance, total_wage,
    representative_sign_file_id, freelancer_sign_file_id,
    contract_written_at, confirmed_at
) VALUES (
    2, '테스트회사', '담당자A', '서울특별시 강남구 테헤란로 123',
    '이디자인', '1995-03-15', '서울특별시 마포구 양화로 45',
    '서울 (재택 가능)', 'UI/UX 디자인 및 배너 제작', 'MON,TUE,WED,THU,FRI',
    '09:00:00', '18:00:00', '12:00:00',
    '2026-07-01', '2026-12-31',
    2500000, 2000000, '160:00:00',
    300000, '10:00:00',
    100000, '08:00:00',
    100000, 2500000,
    NULL, NULL,
    '2026-06-14 00:00:00', NULL
);

-- ── 13. chat_room (각 계약당 1개) ────────────────────────────
INSERT INTO `chat_room` (contract_id, freelancer_email, company_email) VALUES
(1, 'test@lancit.com', 'company@lancit.com'),
(2, 'test2@lancit.com', 'company@lancit.com');

-- ── 14. message ──────────────────────────────────────────────
INSERT INTO `message` (chat_room_id, sender_email, message_type, message, is_deleted, is_updated) VALUES
(2, 'company@lancit.com', 'TEXT', '안녕하세요! 계약 관련해서 몇 가지 논의드리고 싶습니다.', 0, 0),
(2, 'test2@lancit.com',  'TEXT', '네 안녕하세요, 편하게 말씀해주세요!', 0, 0),
(2, 'company@lancit.com', 'TEXT', '근무 시작일을 7월 1일로 조정 가능할까요?', 0, 0);

-- ── 15. proposal (recruitment <-> contract/chatroom 연결) ────
INSERT INTO `proposal` (company_email, freelancer_email, recruitment_id, contract_id, chat_room_id) VALUES
('company@lancit.com', 'test@lancit.com',  1, 1, 1),
('company@lancit.com', 'test2@lancit.com', 1, 2, 2);

-- ── 16. notification ─────────────────────────────────────────
INSERT INTO `notification` (receiver_email, type, target_id, is_read) VALUES
('test@lancit.com',  'PROPOSAL', 1, 0),
('test2@lancit.com', 'CHAT',     2, 0),
('company@lancit.com', 'CHAT',   2, 1);