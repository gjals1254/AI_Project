-- 03-air-quality: 에어코리아·기상청 자동 수집용 측정소 매핑 컬럼 추가 (기존 데이터 보존)
-- 새 DB 는 schema.sql 이 컬럼까지 만들므로 실행할 필요 없다.
-- 이 기능 이전에 만든 air_db 에서 한 번만 실행:
--   mysql -u aiuser -p air_db -e "source db/migration/2026-11-13-air-station-api-mapping.sql"
-- (MySQL 8 은 ADD COLUMN IF NOT EXISTS 를 지원하지 않아 두 번 실행하면 'Duplicate column' 오류가 난다 — 무시해도 된다)

ALTER TABLE station
    ADD COLUMN airkorea_name VARCHAR(50) NULL COMMENT '에어코리아 측정소명' AFTER active,
    ADD COLUMN kma_stn_id    INT         NULL COMMENT '기상청 ASOS 지점번호' AFTER airkorea_name;

-- 기본 측정소 제안값 (관리자 > 측정소 관리에서 수정 가능)
UPDATE station SET airkorea_name = '중구',   kma_stn_id = 108 WHERE name = '중구'       AND airkorea_name IS NULL;
UPDATE station SET airkorea_name = '강남구', kma_stn_id = 108 WHERE name = '강남구'     AND airkorea_name IS NULL;
UPDATE station SET airkorea_name = '인계동', kma_stn_id = 119 WHERE name = '수원'       AND airkorea_name IS NULL;
UPDATE station SET airkorea_name = '연산동', kma_stn_id = 159 WHERE name = '부산 연산동' AND airkorea_name IS NULL;
