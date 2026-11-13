-- 3개 프로젝트 DB + 공용 계정 생성
-- Docker 를 쓰면 최초 기동 시 자동 실행된다.
-- 로컬 MySQL 을 쓰면 root 로 접속해 이 파일을 한 번 실행한다:
--   mysql -u root -p < db/init/01-create-databases.sql

CREATE DATABASE IF NOT EXISTS trademark_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS stress_db    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS air_db       DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'aiuser'@'%' IDENTIFIED BY 'aipass1234';
GRANT ALL PRIVILEGES ON trademark_db.* TO 'aiuser'@'%';
GRANT ALL PRIVILEGES ON stress_db.*    TO 'aiuser'@'%';
GRANT ALL PRIVILEGES ON air_db.*       TO 'aiuser'@'%';
FLUSH PRIVILEGES;

-- 테이블은 각 Spring Boot 앱이 시작할 때 src/main/resources/schema.sql 로 자동 생성한다.
