-- 상표 이미지 유사도 검색 시스템 스키마 (MySQL 8)
-- JPA / MyBatis 두 모드가 같은 테이블을 사용한다.

CREATE TABLE IF NOT EXISTS trademark (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '상표 ID',
    application_no  VARCHAR(30)   NOT NULL COMMENT '출원번호',
    name            VARCHAR(100)  NOT NULL COMMENT '상표명',
    applicant       VARCHAR(100)  NULL     COMMENT '출원인',
    nice_class      INT           NULL     COMMENT '니스 상품분류(1~45류)',
    description     VARCHAR(1000) NULL     COMMENT '설명',
    image_path      VARCHAR(255)  NOT NULL COMMENT '저장 이미지 상대경로',
    feature_vector  MEDIUMTEXT    NULL     COMMENT 'AI 특징 벡터(콤마 구분)',
    model_name      VARCHAR(50)   NULL     COMMENT '벡터 생성 모델명',
    vector_dim      INT           NULL     COMMENT '벡터 차원',
    created_at      DATETIME      NOT NULL COMMENT '등록일시',
    updated_at      DATETIME      NOT NULL COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trademark_application_no (application_no),
    KEY idx_trademark_model (model_name, nice_class)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '상표';

CREATE TABLE IF NOT EXISTS search_history (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '검색 ID',
    query_image_path  VARCHAR(255) NOT NULL COMMENT '검색 이미지 경로',
    nice_class        INT          NULL     COMMENT '검색 시 지정한 상품분류',
    model_name        VARCHAR(50)  NULL     COMMENT '사용 모델',
    result_count      INT          NOT NULL DEFAULT 0 COMMENT '결과 수',
    top_score         DOUBLE       NULL     COMMENT '최고 유사도',
    top_trademark_id  BIGINT       NULL     COMMENT '최고 유사 상표 ID',
    client_ip         VARCHAR(45)  NULL     COMMENT '요청 IP',
    searched_at       DATETIME     NOT NULL COMMENT '검색일시',
    PRIMARY KEY (id),
    KEY idx_search_history_searched_at (searched_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '검색 이력';

CREATE TABLE IF NOT EXISTS search_result (
    id            BIGINT NOT NULL AUTO_INCREMENT COMMENT '결과 ID',
    history_id    BIGINT NOT NULL COMMENT '검색 ID',
    trademark_id  BIGINT NOT NULL COMMENT '상표 ID',
    rank_no       INT    NOT NULL COMMENT '순위',
    score         DOUBLE NOT NULL COMMENT '코사인 유사도',
    PRIMARY KEY (id),
    KEY idx_search_result_history (history_id),
    CONSTRAINT fk_search_result_history FOREIGN KEY (history_id)
        REFERENCES search_history (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '검색 결과';
