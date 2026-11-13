-- 미세먼지 대기질 등급 예측 시스템 스키마 (MySQL 8)

CREATE TABLE IF NOT EXISTS station (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '측정소 ID',
    name        VARCHAR(50)   NOT NULL COMMENT '측정소명',
    region      VARCHAR(50)   NOT NULL COMMENT '시도',
    address     VARCHAR(200)  NULL     COMMENT '주소',
    latitude    DOUBLE        NULL     COMMENT '위도',
    longitude   DOUBLE        NULL     COMMENT '경도',
    active      TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '운영 여부',
    airkorea_name VARCHAR(50) NULL     COMMENT '에어코리아 측정소명',
    kma_stn_id  INT           NULL     COMMENT '기상청 ASOS 지점번호',
    created_at  DATETIME      NOT NULL COMMENT '등록일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_station_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '측정소';

CREATE TABLE IF NOT EXISTS measurement (
    id             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '측정 ID',
    station_id     BIGINT   NOT NULL COMMENT '측정소 ID',
    measure_date   DATE     NOT NULL COMMENT '측정일',
    pm10           DOUBLE   NOT NULL COMMENT 'PM10 일평균(ug/m3)',
    pm25           DOUBLE   NOT NULL COMMENT 'PM2.5 일평균(ug/m3)',
    o3             DOUBLE   NOT NULL COMMENT '오존(ppm)',
    no2            DOUBLE   NOT NULL COMMENT '이산화질소(ppm)',
    co             DOUBLE   NOT NULL COMMENT '일산화탄소(ppm)',
    so2            DOUBLE   NOT NULL COMMENT '아황산가스(ppm)',
    temperature    DOUBLE   NOT NULL COMMENT '기온(C)',
    humidity       DOUBLE   NOT NULL COMMENT '습도(%)',
    wind_speed     DOUBLE   NOT NULL COMMENT '풍속(m/s)',
    precipitation  DOUBLE   NOT NULL COMMENT '강수량(mm)',
    created_at     DATETIME NOT NULL COMMENT '등록일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_measurement_station_date (station_id, measure_date),
    CONSTRAINT fk_measurement_station FOREIGN KEY (station_id) REFERENCES station (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '측정 데이터';

CREATE TABLE IF NOT EXISTS prediction (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '예측 ID',
    station_id      BIGINT       NOT NULL COMMENT '측정소 ID',
    measurement_id  BIGINT       NOT NULL COMMENT '기준 측정 ID',
    target_date     DATE         NOT NULL COMMENT '예측 대상일(측정일+1)',
    pred_pm10       DOUBLE       NOT NULL COMMENT '예측 PM10',
    pred_pm25       DOUBLE       NOT NULL COMMENT '예측 PM2.5',
    pm10_grade      INT          NOT NULL COMMENT 'PM10 등급 1~4',
    pm25_grade      INT          NOT NULL COMMENT 'PM2.5 등급 1~4',
    grade           INT          NOT NULL COMMENT '통합 등급 1좋음 2보통 3나쁨 4매우나쁨',
    grade_label     VARCHAR(10)  NOT NULL COMMENT '통합 등급명',
    top_factors     TEXT         NULL     COMMENT '주요 영향 요인(JSON)',
    model_version   VARCHAR(50)  NULL     COMMENT '모델 버전',
    created_at      DATETIME     NOT NULL COMMENT '예측일시',
    PRIMARY KEY (id),
    KEY idx_prediction_station_target (station_id, target_date),
    CONSTRAINT fk_prediction_station FOREIGN KEY (station_id) REFERENCES station (id) ON DELETE CASCADE,
    CONSTRAINT fk_prediction_measurement FOREIGN KEY (measurement_id) REFERENCES measurement (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '예측 결과';

-- 기본 측정소 (이미 있으면 무시)
-- airkorea_name / kma_stn_id 는 제안값 (관리자 화면에서 수정)
INSERT IGNORE INTO station (name, region, address, latitude, longitude, active, airkorea_name, kma_stn_id, created_at) VALUES
('중구', '서울', '서울 중구 덕수궁길 15', 37.5643, 126.9750, 1, '중구', 108, NOW()),
('강남구', '서울', '서울 강남구 학동로 426', 37.5175, 127.0473, 1, '강남구', 108, NOW()),
('수원', '경기', '경기 수원시 팔달구', 37.2636, 127.0286, 1, '인계동', 119, NOW()),
('부산 연산동', '부산', '부산 연제구 연산동', 35.1858, 129.0819, 1, '연산동', 159, NOW()),
('대구 수창동', '대구', '대구 중구 수창동', 35.8714, 128.5911, 1, '수창동', 143, NOW());
