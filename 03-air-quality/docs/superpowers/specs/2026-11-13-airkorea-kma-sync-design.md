# 에어코리아·기상청 API 일일 자동 수집 — 설계

## 목적
관리자가 CSV를 올리지 않아도 측정소별 실제 측정값이 매일 자동으로 쌓이고, 쌓일 때마다 다음날 예측이 실행되는 화면을 만든다.

- 목표: 매일 어제치(일평균) 측정값 자동 저장 + 예측, 관리자 화면에서 수동 수집·결과 확인
- 비목표: 모델 재학습(데이터가 몇 달 쌓인 뒤 기존 `train.py --csv`로 따로), 90일 이전 과거 데이터 일괄 수집
- 실패로 보는 것: 한 측정소/한 API 실패가 다른 측정소 수집을 막는 것, 결측값을 0으로 저장해 예측을 왜곡하는 것

## 접근 방식 (선택: A)
- **A. Spring 웹 서버 안에서 매일 수집 (선택)** — 기존 `upsertAndPredict` 재사용, 결과를 관리자 화면에서 확인. 웹 서버가 꺼져 있던 날은 수동 버튼으로 보충.
- B. Python 스크립트 + Windows 작업 스케줄러 → `/admin/import` 업로드 — 웹 코드 변경은 적지만 스케줄러·로그인·키 관리가 따로 생기고 상태가 화면에 안 보임.
- 제외: AI 서버(FastAPI)가 DB에 직접 저장 — "DB는 웹, 예측은 AI" 역할 분리를 깨므로 제외.

## 설계
1. **측정소 매핑** — `station` 테이블에 `airkorea_name`(에어코리아 측정소명), `kma_stn_id`(기상청 ASOS 지점번호) 컬럼 추가(둘 다 NULL 허용). 둘 다 채워진 활성 측정소만 수집 대상. 관리자 측정소 폼에서 입력.
   기존 DB는 `ALTER TABLE ... ADD COLUMN`으로 추가(데이터 보존), `schema.sql`도 함께 갱신.
2. **API 호출** (`OpenApiClient`, 기존 `RestClient` 사용)
   - 에어코리아 `ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty` (stationName, dataTerm=MONTH, JSON) → 대상 날짜의 시간값만 골라 PM10·PM2.5·O3·NO2·CO·SO2 평균. `-`·빈값은 평균에서 제외.
   - 기상청 `AsosDalyInfoService/getWthrDataList` (stnIds, dataCd=ASOS, dateCd=DAY, startDt=endDt=대상일) → 평균기온, 평균상대습도, 평균풍속, 일강수량(빈값=0).
3. **수집 서비스** (`DataSyncService.sync(date)`) — 측정소마다 두 API 결과를 합쳐 `MeasurementForm`을 만들고 기존 `upsertAndPredict` 호출. 측정소별 try/catch로 실패를 모아 결과(성공/실패/사유) 반환. 필수 대기오염 값이 하루 전부 결측이면 저장하지 않고 실패로 기록.
4. **스케줄** — `@EnableScheduling` + 매일 11:00(설정값 `app.sync.cron`) 어제 날짜로 `sync` 실행. 기상청 일자료가 D-1까지 제공되므로 오전 늦게 실행.
5. **관리자 화면** — 대시보드에 "어제치 지금 수집" + 날짜 지정 수집 폼, 실행 결과를 flash 메시지로 표시(기존 CSV 등록과 같은 방식).
6. **설정·인증키** — `app.openapi.service-key`(공공데이터포털 인증키, 두 API 공용)를 로컬 오버라이드 `web/config/application.yml`에 둔다. 키가 비어 있으면 스케줄은 경고 로그만 남기고 건너뜀.

## 남은 확인 / 빌더 재량
- 실제 응답 필드명(`pm10Value`, `avgTa` 등)은 첫 호출 결과로 확인 후 맞춘다(공식 활용가이드 기준).
- 측정소 4곳의 기본 매핑값(예: 중구↔108 서울)은 제안값으로 채우고 관리자 화면에서 수정 가능.
- 테스트: 응답 JSON 파싱·일평균 계산·결측 처리 단위 테스트, 실제 API는 키 발급 후 수동 확인.
