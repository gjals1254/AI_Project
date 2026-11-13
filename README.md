# AI 프로젝트 3종 (Spring Boot + FastAPI + MySQL)

각 프로젝트는 **웹(Spring Boot, Thymeleaf)** 과 **AI 서버(FastAPI, scikit-learn)** 두 부분으로 되어 있고, DB 하나(MySQL 8 또는 MariaDB)를 함께 씁니다.

| 프로젝트 | 하는 일 | 웹 | AI 서버 | DB |
|---|---|---|---|---|
| `01-trademark-similarity` | 로고 이미지를 올리면 비슷한 등록 상표를 유사도 순으로 보여 줌 | 8081 | 8001 | `trademark_db` |
| `02-parenting-stress` | 부모 설문 10문항으로 스트레스 수준(정상·주의·고위험) 예측 | 8082 | 8002 | `stress_db` |
| `03-air-quality` | 측정소별 미세먼지 실측을 매일 수집하고 다음날 대기질 등급 예측 | 8083 | 8003 | `air_db` |

세 프로젝트는 포트와 DB가 모두 달라서 동시에 띄워도 충돌하지 않습니다.

---

## 준비물

| 도구 | 버전 | 확인 명령 |
|---|---|---|
| JDK | 21 | `java -version` |
| Maven | 3.9 이상 | `mvn -v` |
| Python | 3.12 | `python --version` |
| DB | Docker(MySQL 8) **또는** 로컬 MySQL/MariaDB | `docker -v` / `mysql --version` |

아래 명령은 모두 **저장소 최상위 폴더**(이 README가 있는 곳)에서 시작합니다. Windows는 PowerShell 기준입니다.

---

## 1. DB 준비

### A. Docker를 쓰는 경우 (가장 간단)

```bash
docker compose up -d
```

MySQL 8이 3306 포트로 뜨고, 처음 실행할 때 DB 3개와 공용 계정 `aiuser` / `aipass1234` 가 자동으로 만들어집니다.
테이블은 각 웹이 시작할 때 `schema.sql` 로 자동 생성됩니다.

### B. 로컬 MySQL·MariaDB를 쓰는 경우

root 계정으로 초기화 스크립트를 **한 번** 실행합니다(비밀번호는 직접 입력).

```bash
mysql -u root -p -e "source db/init/01-create-databases.sql"
```

> PowerShell에서는 `mysql ... < 파일` 형태의 입력 리다이렉션이 안 되므로 위처럼 `-e "source ..."` 를 씁니다.

이 기능이 추가되기 전에 만든 기존 `air_db` 가 있다면 마이그레이션도 한 번 실행합니다.

```bash
mysql -u aiuser -p air_db -e "source db/migration/2026-11-13-air-station-api-mapping.sql"
```

---

## 2. 로컬 설정 파일 (필요한 경우만)

각 웹은 `web/config/application.yml` 이 있으면 자동으로 읽습니다. 이 파일은 git에서 제외되어 있으므로 예시를 복사해서 만듭니다.

```bash
copy 03-air-quality\web\config\application.example.yml 03-air-quality\web\config\application.yml
```

- **MariaDB를 쓰면 세 프로젝트 모두 필요합니다.** MySQL 드라이버가 MariaDB에 없는 정보를 조회하다 기동에 실패하기 때문에, 예시 파일에 방언 설정이 들어 있습니다. (01, 02도 같은 방법으로 복사)
- **Docker MySQL이면 01, 02는 필요 없습니다.** 03은 공공데이터 인증키를 쓸 때만 필요합니다(아래 5장).

---

## 3. AI 서버 가상환경 만들기

```bash
.\scripts\setup-python.bat
```

(macOS·Linux: `bash scripts/setup-python.sh`) 각 `ai-server/.venv` 에 패키지가 설치됩니다.

01을 ResNet50 딥러닝 모델로 돌리려면 추가로 설치합니다. 설치하지 않으면 가벼운 HOG+색상 추출기(`lite-hog-color-v1`)로 동작합니다.

```bash
.\01-trademark-similarity\ai-server\.venv\Scripts\pip install -r 01-trademark-similarity\ai-server\requirements-torch.txt --index-url https://download.pytorch.org/whl/cpu
```

---

## 4. 모델 준비

학습된 모델 파일(`ai-server/model/`)과 데이터 CSV(`ai-server/data/`)는 저장소에 들어 있지 않습니다.

| 프로젝트 | 해야 할 일 |
|---|---|
| 01 | 없음. 미리 학습된 특징 추출기를 그대로 씁니다. |
| 02 | **설문 데이터 CSV가 필요합니다.** `02-parenting-stress/ai-server/data/parenting_stress.csv` 에 넣으면 AI 서버가 처음 뜰 때 자동으로 학습합니다. 컬럼 형식은 `ai-server/stress_model.py` 의 `FEATURES`(CSV 컬럼명)와 `psi_severity` 를 따릅니다. |
| 03 | 없어도 뜹니다(처음엔 합성 데이터로 학습). 실제 데이터로 학습하려면 아래 명령으로 Open-Meteo 2년치를 받아 학습합니다(키 불필요, 인터넷 필요). |

03 실제 데이터 학습:

```bash
cd 03-air-quality\ai-server
```

```bash
.\.venv\Scripts\python fetch_openmeteo.py data\openmeteo_train.csv 2024-11-01 2026-11-11
```

```bash
.\.venv\Scripts\python train.py --csv data\openmeteo_train.csv
```

날짜는 원하는 기간으로 바꿔도 됩니다. 웹의 관리자 화면 "모델 재학습" 버튼도 같은 일(최근 2년, 어제까지)을 합니다.

---

## 5. 실행

프로젝트마다 **AI 서버 → 웹** 순서로, 각각 별도 터미널에서 실행합니다.

**AI 서버** (저장소 최상위에서)

```bash
.\scripts\run-ai.bat trademark
```

`trademark` 대신 `stress`(8002), `air`(8003). (macOS·Linux: `bash scripts/run-ai.sh trademark`)

**웹** (반드시 각 프로젝트의 `web` 폴더로 이동한 뒤)

```bash
cd 01-trademark-similarity\web
```

```bash
mvn spring-boot:run
```

MyBatis 모드로 실행하려면 `mvn spring-boot:run -Dspring-boot.run.profiles=mybatis`.

> `web` 폴더가 아닌 곳에서 `mvn spring-boot:run` 을 실행하면 `No plugin found for prefix 'spring-boot'` 오류가 납니다. 또 `web/config/application.yml` 도 `web` 폴더에서 실행해야 읽힙니다.

### 접속 주소와 계정

| 프로젝트 | 주소 | 관리자 | 일반 |
|---|---|---|---|
| 01 상표 유사도 | http://localhost:8081 | admin / admin1234 | user / user1234 |
| 02 양육 스트레스 | http://localhost:8082 | admin / admin1234 | counselor / counselor1234 |
| 03 미세먼지 | http://localhost:8083 | admin / admin1234 | operator / operator1234 |

계정은 데모용입니다. 실제로 배포한다면 각 `application.yml` 의 `app.security` 값을 바꾸세요.
AI 서버 API 문서는 `http://localhost:8001/docs` 처럼 각 AI 서버 포트의 `/docs` 에서 볼 수 있습니다.

---

## 6. 프로젝트별 사용법

### 01 상표 유사도
- **한 건 등록:** 로그인 → "상표 등록" (출원번호·상표명·이미지). 저장할 때 AI 서버가 이미지 특징 벡터를 만듭니다.
- **여러 건 한꺼번에 등록:** 파일명을 `출원번호_상표명.png` 로 맞춘 이미지 폴더를 지정합니다.

  ```bash
  .\01-trademark-similarity\ai-server\.venv\Scripts\python .\01-trademark-similarity\scripts\bulk_register.py "D:\상표\이미지\폴더"
  ```

  옵션: `--applicant 홍길동 --nice-class 9`, 계정 `--user`/`--password`. 이미 있는 출원번호는 건너뜁니다.
- **검색:** "이미지로 검색"에서 로고를 올리면 유사도 순으로 결과가 나옵니다(85% 이상 '높음').
- AI 서버를 켜지 않고 등록한 상표는 관리자 > 벡터 관리 > "전체 벡터 재추출"로 벡터를 다시 만듭니다.

### 02 양육 스트레스
- 로그인 → "새 설문"에서 10문항(나이, 자녀 수, 막내 나이, 수면, 근로시간, 양육 효능감·사회적 지지·가족 환경·회복탄력성·삶의 질 0~100)을 입력하면 수준·위험점수·주요 요인이 나옵니다.
- 결과는 진단이 아닌 **선별 참고 자료**입니다.
- 관리자 > 모델·통계의 "재학습"은 `ai-server/data/parenting_stress.csv` 로 다시 학습합니다.

### 03 미세먼지
- **자동 수집:** 매일 11:00에 측정소별 전날 값을 수집하고 다음날 등급을 예측합니다. 관리자 대시보드에서 날짜를 지정해 수동으로 수집할 수도 있습니다.
- **수집 출처** (`app.sync.source`):
  - `openmeteo` (기본) — 키 불필요. 측정소 위경도로 받는 **대기 모델 추정값**입니다.
  - `publicdata` — 에어코리아 + 기상청 ASOS **실측값**. 공공데이터포털에서 "한국환경공단 에어코리아 대기오염정보"와 "기상청 지상(종관, ASOS) 일자료 조회서비스"를 활용신청하고, 일반 인증키(Decoding)를 `03-air-quality/web/config/application.yml` 의 `app.openapi.service-key` 에 넣은 뒤 `app.sync.source: publicdata` 로 바꾸고 웹을 재시작합니다.
- **측정소:** 관리자 > 측정소 관리에서 추가·수정합니다. Open-Meteo는 위도·경도, 공공데이터는 에어코리아 측정소명과 기상청 지점번호가 있어야 수집 대상이 됩니다.
- **CSV 등록:** 관리자 대시보드 > "CSV 일괄 등록" (형식은 `web/src/main/resources/static/sample/measurements_60days.csv` 참고).
- **재학습:** 관리자 대시보드 "모델 재학습"(Open-Meteo 최근 2년, 약 40초).

---

## 7. 테스트

```bash
cd 03-air-quality\web
```

```bash
mvn test
```

AI 서버 테스트는 각 `ai-server` 폴더에서 `.\.venv\Scripts\python -m pytest -q`.

---

## 8. 자주 만나는 문제

| 증상 | 원인과 해결 |
|---|---|
| 웹 기동 시 `Access denied for user 'aiuser'` | DB 초기화 스크립트를 안 돌렸습니다. 1장 실행. |
| 웹 기동 시 `Unable to determine Dialect` / `Unknown column 'RESERVED'` | DB가 MariaDB입니다. 2장의 설정 파일을 복사하세요. |
| `No plugin found for prefix 'spring-boot'` | `web` 폴더가 아닌 곳에서 실행했습니다. |
| `'ai-server' 모듈을 로드할 수 없습니다` (PowerShell) | 상대 경로 실행 파일 앞에 `.\` 가 필요하거나 폴더 위치가 다릅니다. 전체 경로로 실행하세요. |
| `Port 8081 was already in use` | 같은 웹이 이미 떠 있습니다. 기존 프로세스를 끄고 다시 실행. |
| 03 수집이 "등록되지 않은 서비스키"로 실패 | 공공데이터 키가 아직 활성화 전입니다(승인 후 몇 시간). 그동안은 `openmeteo` 출처를 쓰세요. |
| 02 AI 서버가 뜨자마자 종료 | `ai-server/data/parenting_stress.csv` 가 없습니다. 4장 참고. |

---

## 구조: JPA / MyBatis

서비스는 `repository/*Store` 인터페이스에만 의존하고, 구현은 프로파일로 고릅니다.
- `repository/jpa` : Spring Data JPA (`@Profile("jpa")`, 기본값)
- `repository/mybatis` + `resources/mapper/*.xml` : MyBatis (`@Profile("mybatis")`)

두 모드는 같은 `schema.sql` 테이블을 씁니다.
