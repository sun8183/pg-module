# Payment / Wallet API

고객 지갑(Wallet)에 대해 외부 결제 게이트웨이(PG) 연동을 통해 충전(CHARGE)·결제(PAYMENT)를 수행하고, 처리 이력을 원장(Ledger)으로 남기는 과제 프로젝트입니다.

## 기술 스택

- Java 17
- Spring Boot 3.3.4 (Web, Validation, Data JPA, AOP)
- H2 (in-memory)
- Gradle

## 아키텍처 개요

```
com.switchwon.payment
├── common
│   ├── response     # ApiResponse, ResponseCode
│   ├── exception    # ApiException, GlobalExceptionHandler
│   └── logging      # PaymentAuditLoggingAspect (AOP 감사 로깅)
├── config          # DataInitializer (테스트 계정 시딩)
├── payment
│   ├── controller   # PaymentController
│   ├── service      # PaymentService (오케스트레이션), PaymentLedgerCommandService (원장 상태 변경)
│   ├── domain        # PaymentLedger, LedgerType, LedgerStatus, FailureReason
│   ├── gateway       # PaymentGatewayClient(인터페이스), FakePaymentGatewayClient, TimeoutPaymentGatewayClient
│   └── repository
└── wallet
    ├── controller   # WalletController
    ├── service      # WalletService
    ├── domain       # CustomerWallet
    └── repository
```

**트랜잭션 경계**: 원장 PENDING 저장(TX1) → 외부 PG 호출(트랜잭션 밖) → 정산/실패 반영(TX2)의 3단계로 분리해, 외부 IO 대기 중 DB 커넥션을 점유하지 않도록 설계했습니다.

**외부 PG 연동**: 결제(차감)뿐 아니라 충전도 동일하게 `PaymentGatewayClient.approve()`를 거친 뒤 지갑에 반영합니다. `FakePaymentGatewayClient`가 가상의 PG 역할을 하며, `TimeoutPaymentGatewayClient`가 응답 지연 시 타임아웃 처리를 감싸는 데코레이터입니다.

**멱등성**: 결제·충전 요청은 클라이언트가 발급한 `Idempotency-Key` 헤더로 중복 요청을 막습니다. 같은 키로 재요청하면 PG를 다시 호출하지 않고 기존 원장 결과를 그대로 반환합니다. 동시 요청 경합은 DB unique 제약이 최종 방어선입니다.

**감사 로깅**: 잔액 변동(`WalletService.charge`/`deductIfSufficient`)과 PG 승인(`PaymentGatewayClient.approve`) 호출만 AOP(`PaymentAuditLoggingAspect`)로 가로채 `logs/payment-audit.log`에 별도로 남깁니다. 트랜잭션 커밋/롤백이 끝난 뒤 기록되도록 어드바이스 순서를 조정했습니다.

## 도메인 모델

| Enum | 값 |
|---|---|
| `LedgerType` | `PAYMENT`, `CHARGE` |
| `LedgerStatus` | `PENDING`, `COMPLETED`, `FAILED` |
| `FailureReason` | `INSUFFICIENT_BALANCE`, `SYSTEM_ERROR` |

## API

| Method | URL | 설명 |
|---|---|---|
| `POST` | `/api/wallets/charge` | 지갑 충전 (PG 승인 → 잔액 반영). `Idempotency-Key` 헤더 필수 |
| `GET` | `/api/wallets/{customerId}` | 지갑 잔액 조회 |
| `POST` | `/api/payments` | 결제 (PG 승인 → 잔액 차감). `Idempotency-Key` 헤더 필수 |
| `GET` | `/api/payments/{customerId}` | 결제/충전 이력 조회 (페이징) |

응답은 공통 포맷 `ApiResponse { code, message, returnObject }` 사용.

### Idempotency-Key

- `POST /api/wallets/charge`, `POST /api/payments` 요청 시 `Idempotency-Key` 헤더 필수, 2~10자 문자열.
- 누락하거나 길이 벗어나면 `400 INVALID_REQUEST`.
- 같은 키로 재요청하면 PG를 다시 호출하지 않고 기존 처리 결과(성공/실패 무관)를 그대로 반환합니다.

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pay-0001" \
  -d '{"customerId":"test-customer","amount":30000}'
```

### 페이징 (결제/충전 이력)

`page`, `size`, `sort` 쿼리 파라미터 지원 (기본: `size=20`, `sort=requestedAt,desc`).

```bash
curl "http://localhost:8080/api/payments/test-customer?page=0&size=10"
```

응답 `returnObject`는 `Page<PaymentResponse>` 형태로 `content`, `totalElements`, `totalPages` 등을 포함합니다.

## 테스트 계정

앱 기동 시 `DataInitializer`가 자동 생성합니다.

- `customerId`: `test-customer`
- 초기 잔액: `100,000`

## 테스트 흐름

### 1. 지갑 잔액 조회

```bash
curl http://localhost:8080/api/wallets/test-customer
```

### 2. 충전 (외부 PG 연동 → 잔액 증가)

```bash
curl -X POST http://localhost:8080/api/wallets/charge \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: charge01" \
  -d '{"customerId":"test-customer","amount":50000}'
```

### 3. 결제 (외부 PG 승인 → 잔액 차감)

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pay-0001" \
  -d '{"customerId":"test-customer","amount":30000}'
```

### 4. 결제 이력 조회 (페이징)

```bash
curl "http://localhost:8080/api/payments/test-customer?page=0&size=10"
```

### 5. 잔액 부족 시나리오 (`FAILED` / `INSUFFICIENT_BALANCE`)

지갑 잔액보다 큰 금액으로 결제 요청 시, PG 승인은 성공하지만 잔액 차감 단계에서 실패하여 원장이 `FAILED`, `reason=INSUFFICIENT_BALANCE`로 저장됩니다.

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pay-0002" \
  -d '{"customerId":"test-customer","amount":999999999}'
```

### 6. PG 오류 / 타임아웃 시나리오 (`FAILED` / `SYSTEM_ERROR`)

`application.properties`의 아래 값을 조정해 PG 장애 상황을 재현할 수 있습니다.

```properties
# 0.0 ~ 1.0, PG 승인 실패율
payment.gateway.fake.failure-rate=0.3
# 가짜 PG 응답 지연(ms)
payment.gateway.fake.latency-ms=0
# PG 응답 대기 제한 시간(ms), 초과 시 SYSTEM_ERROR 처리
payment.gateway.timeout-ms=3000
```

`failure-rate`를 0보다 크게 주면 확률적으로 `PaymentGatewayException`이 발생하고, `latency-ms`를 `timeout-ms`보다 크게 주면 타임아웃이 발생합니다. 두 경우 모두 원장은 `FAILED`, `reason=SYSTEM_ERROR`로 저장됩니다.

### 7. 멱등키 재요청 (중복 승인 방지 확인)

같은 `Idempotency-Key`로 동일 요청을 다시 보내면 PG를 재호출하지 않고 기존 원장 결과를 그대로 반환합니다 (`ledgerId` 동일).

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pay-0001" \
  -d '{"customerId":"test-customer","amount":30000}'
```

## 로깅 정책

### 감사 로그

`WalletService.charge`/`deductIfSufficient`, `PaymentGatewayClient.approve` 호출은 AOP(`PaymentAuditLoggingAspect`)로 가로채 콘솔과 `logs/payment-audit.log`에 별도로 기록됩니다. `@Transactional`보다 바깥 순위로 둬서, 트랜잭션 커밋/롤백이 끝난 뒤 최종 확정된 결과만 로그로 남깁니다.

롤링 정책(`logback-spring.xml`):

| 항목 | 값 |
|---|---|
| 파일 분할 | 날짜별 + 파일당 100MB 초과 시 추가 분할 |
| 보관 기간 | 30일 |
| 전체 용량 상한 | 2GB (초과 시 오래된 파일부터 자동 삭제) |

### SQL 쿼리 로그

Hibernate가 생성하는 SQL과 바인딩 파라미터 값은 `org.hibernate.SQL`(DEBUG), `org.hibernate.orm.jdbc.bind`(TRACE) 로거로 콘솔에 출력됩니다 (`spring.jpa.show-sql` 대신 로거 기반으로 전환해 다른 로그와 동일한 포맷/타임스탬프를 갖도록 함).

기본은 켜져 있으며(로컬 개발 편의), 트래픽이 많아 로그가 과도해지면 코드 수정 없이 환경변수로 끌 수 있습니다.

```bash
SQL_LOG_LEVEL=OFF SQL_BIND_LOG_LEVEL=OFF ./gradlew bootRun
```

바인딩 파라미터 로그는 실제 값(고객 ID, 금액, idempotency key 등)을 그대로 출력하므로, 운영 환경에서는 기본값으로 켜두지 않는 것을 권장합니다.

콘솔(stdout) 자체의 용량/보관 정책은 logback이 관여하지 않는 영역으로, 배포 환경의 로그 드라이버(docker, systemd/journald, k8s 등)에서 별도로 관리해야 합니다.

## 실행

```bash
./gradlew bootRun
```

## H2 콘솔로 데이터 확인

1. 앱 기동 후 브라우저로 `http://localhost:8080/h2-console` 접속.
2. 로그인 화면에서 아래 값 입력 후 `Connect`.
   - JDBC URL: `jdbc:h2:mem:payment`
   - User Name: `sa`
   - Password: (빈 값)
3. 좌측 테이블 목록에서 `PAYMENT_LEDGER`, `CUSTOMER_WALLET` 선택하거나 아래 쿼리로 직접 조회.

```sql
SELECT * FROM payment_ledger ORDER BY requested_at DESC;
SELECT * FROM customer_wallet;
```

`PAYMENT_LEDGER`에서 `IDEMPOTENCY_KEY`(요청 중복 방지 키), `EXTERNAL_TXN_ID`(PG 거래ID, 실패 건은 `NULL`), `STATUS`/`REASON`(처리 결과)을 바로 확인할 수 있습니다.

인메모리 DB(`jdbc:h2:mem:`)라 앱을 재시작하면 데이터가 초기화됩니다.
