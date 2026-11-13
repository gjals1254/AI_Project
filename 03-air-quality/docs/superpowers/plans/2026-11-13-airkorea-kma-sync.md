# 에어코리아·기상청 일일 자동 수집 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 측정소별로 어제치 대기오염(에어코리아)·날씨(기상청 ASOS) 일평균을 매일 자동 수집해 저장하고 예측까지 실행한다.

**Architecture:** Spring 웹(03-air-quality/web) 안에 `OpenApiClient`(호출·파싱)와 `DataSyncService`(측정소 루프 → 기존 저장·예측 경로)를 추가하고, `@Scheduled` 와 관리자 버튼이 `DataSyncService.sync(date)` 를 부른다.

**Tech Stack:** Spring Boot 3.5.6, RestClient, Jackson, JUnit 5 + Mockito(spring-boot-starter-test), MariaDB/MySQL, Thymeleaf

**Spec:** `docs/superpowers/specs/2026-11-13-airkorea-kma-sync-design.md`

## Global Constraints

- 저장소가 git 이 아니므로 커밋 단계 없음. 각 Task 끝은 `mvn -q test` 통과.
- 인증키는 `app.openapi.service-key`, 로컬 오버라이드 `web/config/application.yml` 에만 둔다(저장소 기본 `application.yml` 에는 빈 값).
- 스케줄: `app.sync.cron` 기본 `0 0 11 * * *`, 대상일 = 어제.
- 결측(`-`, 빈값, null)은 평균에서 제외. 대기오염 6항목 중 하나라도 하루 전부 결측이면 그 측정소·날짜는 저장하지 않고 실패로 기록. 강수량 빈값 = 0.
- 기존 DB 데이터 보존: `station` 은 `ALTER TABLE ... ADD COLUMN` 으로만 변경.

## Review Focus

1. 에어코리아 `dataTime` 의 `YYYY-MM-DD 24:00` 은 그 날짜의 24시(다음날 00:00 아님)로 대상일에 포함해야 한다 → Task 2 테스트 `midnight24BelongsToSameDay`.
2. 응답에 대상일 시간이 하나도 없음(API 지연·측정소명 오타) → 예외 없이 "결측" 처리되어 그 측정소만 실패 → Task 2 `noRowsForDateIsMissing`, Task 3 `missingPollutantSkipsSave`.
3. 한 측정소에서 API 예외 → 나머지 측정소는 계속 저장 → Task 3 `oneStationFailureDoesNotStopOthers`.
4. 매핑(`airkoreaName`/`kmaStnId`) 비어 있는 측정소·비활성 측정소 → 수집 대상에서 조용히 제외 → Task 3 `unmappedStationsAreSkipped`.
5. 인증키 비어 있음 → 스케줄은 경고 로그 후 건너뛰고, 수동 버튼은 "인증키 미설정" 메시지 → Task 4 수동 확인 단계.

---

### Task 1: 측정소 ↔ API 매핑 컬럼

**Files:**
- Modify: `src/main/resources/schema.sql` (station 테이블 + 시드 INSERT)
- Modify: `src/main/java/com/example/air/domain/Station.java`
- Modify: `src/main/java/com/example/air/dto/StationForm.java` (`from`, `applyTo`, getter/setter)
- Modify: `src/main/resources/mapper/StationMapper.xml` (insert, update)
- Modify: `src/main/resources/templates/admin/station-form.html`
- Test: `src/test/java/com/example/air/dto/StationFormTest.java`

**Interfaces:**
- Produces: `Station.getAirkoreaName(): String`, `Station.getKmaStnId(): Integer` (둘 다 nullable), 컬럼 `airkorea_name VARCHAR(50) NULL`, `kma_stn_id INT NULL`

- [ ] **Step 1: 실패하는 테스트** — `StationFormTest.applyToCopiesApiMapping`: form 에 `airkoreaName="중구"`, `kmaStnId=108` 설정 → `applyTo(station)` 후 station 값 동일; `fromCopiesApiMapping`: 역방향. 공백 `airkoreaName` 은 `null` 로 저장.
- [ ] **Step 2:** `mvn -q test -Dtest=StationFormTest` → 컴파일 실패 확인
- [ ] **Step 3:** 필드·getter/setter 추가, JPA 는 `@Column(name="airkorea_name", length=50)`, `@Column(name="kma_stn_id")`. MyBatis insert/update 에 두 컬럼 추가. 폼에 입력칸 2개(“에어코리아 측정소명”, “기상청 ASOS 지점번호”). schema.sql CREATE 에 컬럼 추가, 시드 INSERT 에 값: 중구/중구/108, 강남구/강남구/108, 수원/인계동/119, 부산 연산동/연산동/159 (제안값, 관리자 화면에서 수정 가능).
- [ ] **Step 4:** `mvn -q test` 통과
- [ ] **Step 5: 기존 DB 반영** (데이터 보존)
```sql
ALTER TABLE station ADD COLUMN airkorea_name VARCHAR(50) NULL COMMENT '에어코리아 측정소명',
                    ADD COLUMN kma_stn_id INT NULL COMMENT '기상청 ASOS 지점번호';
UPDATE station SET airkorea_name='중구', kma_stn_id=108 WHERE name='중구';
UPDATE station SET airkorea_name='강남구', kma_stn_id=108 WHERE name='강남구';
UPDATE station SET airkorea_name='인계동', kma_stn_id=119 WHERE name='수원';
UPDATE station SET airkorea_name='연산동', kma_stn_id=159 WHERE name='부산 연산동';
```

### Task 2: OpenApiClient (호출 + 일평균 파싱)

**Files:**
- Create: `src/main/java/com/example/air/openapi/OpenApiClient.java`
- Modify: `src/main/java/com/example/air/config/AppProperties.java` (`OpenApi { serviceKey="", airkoreaBaseUrl="https://apis.data.go.kr/B552584/ArpltnInforInqireSvc", kmaBaseUrl="https://apis.data.go.kr/1360000/AsosDalyInfoService", timeoutMs=10000 }`, `Sync { cron="0 0 11 * * *" }`)
- Modify: `src/main/resources/application.yml` (`app.openapi.*` 기본값, `app.sync.cron`)
- Test: `src/test/java/com/example/air/openapi/OpenApiClientTest.java`

**Interfaces:**
- Produces:
  - `record AirDaily(Double pm10, Double pm25, Double o3, Double no2, Double co, Double so2)` — 항목별 null = 하루 전부 결측
  - `record WeatherDaily(double temperature, double humidity, double windSpeed, double precipitation)`
  - `static AirDaily averageAir(JsonNode body, LocalDate date)` — `response.body.items[]` 중 `dataTime` 날짜가 `date` 인 행(“24:00” 포함)의 `pm10Value, pm25Value, o3Value, no2Value, coValue, so2Value` 평균
  - `static WeatherDaily parseWeather(JsonNode body)` — 첫 item 의 `avgTa, avgRhm, avgWs, sumRn`(빈값→0); item 없으면 `IllegalStateException("기상청 자료 없음")`
  - `AirDaily fetchAir(String stationName, LocalDate date)` — `getMsrstnAcctoRltmMesureDnsty?stationName=&dataTerm=MONTH&returnType=json&numOfRows=1000&ver=1.0&serviceKey=`
  - `WeatherDaily fetchWeather(int stnId, LocalDate date)` — `getWthrDataList?dataCd=ASOS&dateCd=DAY&startDt=&endDt=&stnIds=&dataType=JSON&numOfRows=10&serviceKey=`
  - `boolean hasKey()`

- [ ] **Step 1: 실패하는 테스트** (JSON 문자열을 `ObjectMapper.readTree` 로 만들어 사용)
  - `averagesOnlyTargetDate`: 01-01 13:00 pm10 "40", 01-01 14:00 "60", 01-02 01:00 "999" → `pm10 == 50.0`
  - `midnight24BelongsToSameDay`: 01-01 23:00 "10", "2025-01-01 24:00" "30" → 01-01 평균 20.0
  - `dashAndBlankExcluded`: "-", "", "20" → 20.0
  - `noRowsForDateIsMissing`: 대상일 행 없음 → 6항목 모두 null
  - `weatherBlankRainIsZero`: `sumRn=""` → `precipitation == 0.0`, `avgTa="3.4"` → 3.4
- [ ] **Step 2:** `mvn -q test -Dtest=OpenApiClientTest` → 실패 확인
- [ ] **Step 3:** 구현. `RestClient` 는 기존 `AirPredictionClient` 처럼 `SimpleClientHttpRequestFactory` 타임아웃. 인증키는 공공데이터포털 “Decoding 키”를 쿼리 파라미터로 넣어 URL 인코딩되게 한다(`UriBuilder.queryParam`). 숫자 파싱 실패 값은 결측 취급.
- [ ] **Step 4:** `mvn -q test` 통과

### Task 3: DataSyncService

**Files:**
- Create: `src/main/java/com/example/air/service/DataSyncService.java`
- Modify: `src/main/java/com/example/air/service/AirQualityService.java` — `upsertAndPredict(MeasurementForm)` 를 `public` 으로
- Modify: `src/main/java/com/example/air/dto/Views.java` — `record SyncResult(LocalDate date, int success, int failed, List<String> errors)`
- Test: `src/test/java/com/example/air/service/DataSyncServiceTest.java` (Mockito: `StationService`, `OpenApiClient`, `AirQualityService`)

**Interfaces:**
- Consumes: Task 1 `Station.getAirkoreaName/getKmaStnId`, Task 2 `fetchAir/fetchWeather`, `AirQualityService.upsertAndPredict(MeasurementForm)`, `StationService.findAll(true)`
- Produces: `SyncResult sync(LocalDate date)`

- [ ] **Step 1: 실패하는 테스트**
  - `savesMergedMeasurement`: 측정소 1개, 정상 AirDaily/WeatherDaily → `upsertAndPredict` 1회, 전달된 form 의 `measureDate == date`, `pm10`, `temperature`, `precipitation` 값 일치; 결과 `success=1, failed=0`
  - `oneStationFailureDoesNotStopOthers`: 측정소 2개, 첫째 `fetchAir` 가 `RestClientException` → 둘째는 저장; `success=1, failed=1`, errors[0] 에 첫 측정소명 포함
  - `missingPollutantSkipsSave`: `AirDaily.pm25 == null` → `upsertAndPredict` 호출 없음, `failed=1`, 사유에 “PM2.5 결측”
  - `unmappedStationsAreSkipped`: `airkoreaName == null` 측정소 → API 호출 없음, success/failed 모두 0
- [ ] **Step 2:** `mvn -q test -Dtest=DataSyncServiceTest` → 실패 확인
- [ ] **Step 3:** 구현. 측정소별 try/catch, 실패 사유는 “측정소명: 메시지”. `log.info` 로 날짜별 요약 1줄.
- [ ] **Step 4:** `mvn -q test` 통과

### Task 4: 스케줄 + 관리자 화면

**Files:**
- Modify: `src/main/java/com/example/air/AirApplication.java` — `@EnableScheduling`
- Modify: `DataSyncService` — `@Scheduled(cron = "${app.sync.cron:0 0 11 * * *}") void syncYesterday()`: `!client.hasKey()` 이면 `log.warn("공공데이터 인증키 미설정 - 자동 수집 건너뜀")` 후 return, 아니면 `sync(LocalDate.now().minusDays(1))`
- Modify: `src/main/java/com/example/air/controller/AdminController.java` — `@PostMapping("/sync") sync(@RequestParam(required=false) LocalDate date, RedirectAttributes)`: date 없으면 어제, 키 없으면 flash warning “공공데이터 인증키(app.openapi.service-key)가 설정되지 않았습니다.”, 있으면 결과를 기존 CSV 등록과 같은 flash 형식(“API 수집 완료(날짜): 성공 n건, 실패 n건” + errors)
- Modify: `src/main/resources/templates/admin/dashboard.html` — “API 자동 수집” 카드: 다음 실행 시각 안내(매일 11시), 날짜 입력 + “수집” 버튼(빈 날짜 = 어제), 결과 errors 목록
- Modify: `web/config/application.yml` — `app.openapi.service-key: ""` 자리 + 안내 주석

- [ ] **Step 1:** `mvn -q test` 통과 (기존 + Task 1~3)
- [ ] **Step 2:** 앱 재기동 후 확인: `/admin` 200, 키 없이 “수집” 클릭 → 경고 메시지, 로그에 스케줄 등록 오류 없음
- [ ] **Step 3:** (키 발급 후) `web/config/application.yml` 에 키 입력 → 재기동 → 어제 날짜 수집 → `measurement` 에 4개 측정소 행 + `prediction` 생성 확인. 응답 필드명이 다르면 Task 2 파서와 테스트를 실제 응답에 맞춰 수정.
