# 프로필별 설정 비교표

## 📊 전체 비교

| 설정 항목 | Dev (개발) | Test (테스트) | Prod (운영) |
|----------|-----------|--------------|------------|
| **목적** | 로컬 개발 | 자동화 테스트 | 프로덕션 서비스 |
| **우선순위** | 디버깅 용이성 | 테스트 속도 | 성능 + 보안 |

---

## 🗄️ DataSource & HikariCP

| 설정 | Dev | Test | Prod |
|------|-----|------|------|
| **Pool Size (max)** | 10 | 5 | 20 |
| **Pool Size (min)** | 5 | 2 | 10 |
| **Connection Timeout** | 30s | 10s | 30s |
| **Idle Timeout** | 600s | 300s | 300s |
| **Max Lifetime** | 1800s | 600s | 1800s |
| **Leak Detection** | 60s ✅ | - | 120s ✅ |
| **PrepStmt Cache** | - | - | 250 ⚡ |
| **Batch Rewrite** | ✅ | - | ✅ ⚡ |

**권장 이유:**
- **Dev**: 로컬 부담 최소화, 누수 감지 활성화
- **Test**: 최소 리소스로 빠른 테스트
- **Prod**: 대용량 트래픽 대응, 최적화 설정

---

## 🔧 JPA / Hibernate

| 설정 | Dev | Test | Prod |
|------|-----|------|------|
| **ddl-auto** | validate ✅ | create-drop | none ⚠️ |
| **show-sql** | true ✅ | true | false |
| **format_sql** | true | true | true |
| **use_sql_comments** | true ✅ | false | false |
| **generate_statistics** | true ✅ | false | false |
| **batch_size** | 100 | 50 | 100 |

**ddl-auto 설명:**
- `validate`: 스키마 검증만 (운영 안전)
- `create-drop`: 테스트 시작/종료 시 생성/삭제
- `none`: 아무것도 안 함 (프로덕션 필수)

---

## 📝 MyBatis

| 설정 | Dev | Test | Prod |
|------|-----|------|------|
| **log-impl** | Slf4jImpl ✅ | NoLoggingImpl | NoLoggingImpl |
| **cache-enabled** | true | false | true ⚡ |
| **local-cache-scope** | STATEMENT | STATEMENT | SESSION ⚡ |
| **default-fetch-size** | 100 | 100 | 200 ⚡ |
| **default-executor-type** | REUSE | REUSE | REUSE |

**Executor 타입:**
- `SIMPLE`: 매번 새로운 Statement
- `REUSE`: PreparedStatement 재사용 (권장)
- `BATCH`: 배치 업데이트 최적화

---

## 📊 Logging Levels

### Application Logging

| Logger | Dev | Test | Prod |
|--------|-----|------|------|
| **root** | info | warn | warn |
| **com.mudosa.musinsa** | debug ✅ | info | info |

### JPA Logging

| Logger | Dev | Test | Prod |
|--------|-----|------|------|
| **org.hibernate.SQL** | debug ✅ | info | warn |
| **org.hibernate.type** | trace ✅ | warn | warn |
| **org.hibernate.stat** | debug ✅ | warn | warn |

**출력 예시 (Dev):**
```sql
Hibernate: 
    SELECT
        o.order_id,
        o.user_id,
        o.total_price 
    FROM
        `Order` o 
    WHERE
        o.order_id=?
2024-01-01 12:00:00.123 TRACE o.h.type.descriptor.sql.BasicBinder - binding parameter [1] as [BIGINT] - [123]
```

### MyBatis Logging

| Logger | Dev | Test | Prod |
|--------|-----|------|------|
| **domain.*.mapper** | trace ✅ | info | warn |

**출력 예시 (Dev):**
```
==>  Preparing: SELECT * FROM Payment WHERE order_id = ?
==> Parameters: 123(Long)
<==    Columns: payment_id, order_id, amount, status
<==        Row: 456, 123, 50000.00, APPROVED
<==      Total: 1
```

### Spring Framework

| Logger | Dev | Test | Prod |
|--------|-----|------|------|
| **org.springframework.web** | debug ✅ | info | warn |
| **org.springframework.security** | debug ✅ | warn | warn |
| **org.springframework.jdbc** | debug ✅ | warn | warn |
| **org.springframework.transaction** | debug ✅ | warn | warn |

### Connection Pool

| Logger | Dev | Test | Prod |
|--------|-----|------|------|
| **com.zaxxer.hikari** | debug ✅ | warn | warn |
| **HikariPool** | debug ✅ | warn | warn |

---

## 🔐 Security & JWT

| 설정 | Dev | Test | Prod |
|------|-----|------|------|
| **JWT Secret** | 개발용 키 | 테스트용 키 | 환경변수 ⚠️ |
| **Access Token** | 1시간 | 1시간 | 30분 🔒 |
| **Refresh Token** | 7일 | 7일 | 14일 |
| **SSL Required** | false | false | true ⚠️ |

**프로덕션 주의:**
- JWT Secret은 반드시 256bit 이상
- 환경 변수로만 관리
- 절대 하드코딩 금지

---

## 📚 Swagger / API Docs

| 설정 | Dev | Test | Prod |
|------|-----|------|------|
| **Swagger UI** | ✅ Enabled | ❌ Disabled | ❌ Disabled |
| **API Docs** | ✅ Enabled | ❌ Disabled | ❌ Disabled |
| **Request Duration** | ✅ Show | - | - |

**접근 경로 (Dev):**
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API Docs: `http://localhost:8080/api-docs`

---

## 🏥 Management / Actuator

| Endpoint | Dev | Test | Prod |
|----------|-----|------|------|
| **health** | ✅ always | ✅ never | ✅ when-authorized |
| **info** | ✅ | - | ❌ |
| **metrics** | ✅ | - | ✅ |
| **prometheus** | ✅ | - | ✅ |

**프로덕션 보안:**
- 인증된 사용자만 상세 정보 확인
- 민감한 정보 노출 방지

---

## ⚙️ Server Configuration

| 설정 | Dev | Test | Prod |
|------|-----|------|------|
| **Port** | 8080 | 8080 | 8080 |
| **HTTP/2** | - | - | ✅ Enabled ⚡ |
| **Compression** | ✅ | - | ✅ ⚡ |
| **Error Stacktrace** | always ✅ | never | never 🔒 |
| **Error Exception** | true ✅ | false | false 🔒 |
| **Graceful Shutdown** | 30s | - | 30s ✅ |

---

## 🌐 Static Resources

| 설정 | Dev | Test | Prod |
|------|-----|------|------|
| **Caching** | 0s (비활성화) | 비활성화 | 1년 ⚡ |
| **Add Mappings** | true | false | true |

---

## 📁 File Logging

| 설정 | Dev | Test | Prod |
|------|-----|------|------|
| **File Logging** | ❌ Console만 | ❌ | ✅ |
| **Path** | - | - | `/var/log/musinsa/` |
| **Max Size** | - | - | 100MB |
| **Max History** | - | - | 30일 |
| **Total Cap** | - | - | 3GB |

---

## 🎯 사용 시나리오

### Dev Profile - 언제 사용?

```bash
# 로컬 개발
./gradlew bootRun --args='--spring.profiles.active=dev'
```

**사용 상황:**
- ✅ 로컬에서 개발 중
- ✅ 디버깅 필요
- ✅ SQL 쿼리 확인 필요
- ✅ Swagger로 API 테스트

**특징:**
- 🔍 모든 SQL + 파라미터 출력
- 🔍 상세한 로그
- 🔍 Swagger UI 활성화
- ⚡ 변경 사항 즉시 반영

### Test Profile - 언제 사용?

```bash
# 테스트 실행
./gradlew test
```

**사용 상황:**
- ✅ JUnit 테스트 실행
- ✅ 통합 테스트
- ✅ CI/CD 파이프라인

**특징:**
- ⚡ 빠른 실행
- 🗄️ DB 자동 생성/삭제
- 📝 최소 로깅
- 🔒 격리된 환경

### Prod Profile - 언제 사용?

```bash
# 프로덕션 배포
java -jar -Dspring.profiles.active=prod app.jar
```

**사용 상황:**
- ✅ 실제 서비스 운영
- ✅ 스테이징 환경
- ✅ 프리프로덕션

**특징:**
- 🚀 최고 성능
- 🔒 최대 보안
- 📝 최소 로깅
- ⚡ 캐시 최대 활용

---

## 📈 성능 비교

### 쿼리 실행 시간 (예상)

| 작업 | Dev | Test | Prod |
|------|-----|------|------|
| **단순 조회** | ~5ms | ~3ms | ~2ms |
| **복잡한 조인** | ~50ms | ~40ms | ~30ms |
| **배치 INSERT (100건)** | ~100ms | ~80ms | ~50ms |

**프로덕션 최적화 요소:**
- ✅ PreparedStatement 캐싱
- ✅ 배치 리라이트
- ✅ 커넥션 풀 최적화
- ✅ 2차 캐시 활용

---

## 🎓 학습 포인트

### 초급 개발자

- `ddl-auto`의 위험성 이해
- 로깅 레벨의 의미
- 커넥션 풀의 필요성

### 중급 개발자

- HikariCP 튜닝
- JPA N+1 문제 해결
- MyBatis 캐싱 전략

### 고급 개발자

- 프로파일링 및 최적화
- 프로덕션 모니터링
- 장애 대응 전략

---

## 🔗 관련 문서

- [환경 설정 가이드](SETUP.md)
- [MyBatis 가이드](MYBATIS_GUIDE.md)
- [트러블슈팅 가이드](TROUBLESHOOTING.md)
