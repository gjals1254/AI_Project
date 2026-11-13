# AI 프로젝트 3종 (Spring Boot + FastAPI + MySQL)

| 프로젝트 | 웹 | AI 서버 | DB |
|---|---|---|---|
| 01-trademark-similarity — AI 기반 상표 이미지 유사도 검색 및 추천 | 8081 | 8001 | trademark_db |
| 02-parenting-stress — 양육 스트레스 노출군 정신건강 스트레스 수준 예측 | 8082 | 8002 | stress_db |
| 03-air-quality — 미세먼지 대기질 등급 예측 | 8083 | 8003 | air_db |

## 빠른 시작

```bash
docker compose up -d                       # 1) MySQL (DB 3개 + aiuser 계정 자동 생성)
bash scripts/setup-python.sh               # 2) AI 서버 가상환경 (Windows: scripts\setup-python.bat)
bash scripts/run-ai.sh trademark           # 3) AI 서버 실행 (stress / air)
cd 01-trademark-similarity/web
mvn spring-boot:run                        # 4) 웹 실행 (JPA 모드)
mvn spring-boot:run -Dspring-boot.run.profiles=mybatis   # MyBatis 모드
```

## JPA / MyBatis
서비스는 `repository/*Store` 인터페이스에만 의존합니다.
- `repository/jpa` : Spring Data JPA (`@Profile("jpa")`)
- `repository/mybatis` + `resources/mapper/*.xml` : MyBatis (`@Profile("mybatis")`)

두 모드는 같은 `schema.sql` 테이블을 사용합니다.

## 기본 계정
| 프로젝트 | 관리자 | 일반 |
|---|---|---|
| 01 | admin / admin1234 | user / user1234 |
| 02 | admin / admin1234 | counselor / counselor1234 |
| 03 | admin / admin1234 | operator / operator1234 |

## 주의
- 02, 03 모델은 **합성 데이터**로 학습되어 있습니다. 실제 데이터 CSV 로 `python train.py --csv data.csv` 재학습하세요.
- 01 은 PyTorch 가 없으면 lite(HOG+색상) 추출기로 동작합니다. `requirements-torch.txt` 설치 시 ResNet50 사용.

## 03 미세먼지: 에어코리아·기상청 자동 수집
- 공공데이터포털에서 "에어코리아 대기오염정보"와 "기상청 지상(종관, ASOS) 일자료" 활용신청 → 일반 인증키(Decoding)를
  `03-air-quality/web/config/application.yml` 의 `app.openapi.service-key` 에 넣고 웹 재시작.
- 매일 11시 전날치를 자동 수집·예측합니다. 관리자 대시보드에서 날짜를 지정해 수동 수집도 가능합니다(최근 약 30일).
- 수집 대상: 관리자 > 측정소 관리에서 에어코리아 측정소명과 기상청 지점번호를 모두 입력한 측정소.
- **이 기능 이전에 만든 `air_db`** 는 `db/migration/2026-11-13-air-station-api-mapping.sql` 을 한 번 실행해야 합니다.

자세한 내용은 설계서 / 작업환경 구축 매뉴얼 문서를 참고하세요.
