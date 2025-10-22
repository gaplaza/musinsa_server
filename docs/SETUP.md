# 무신사 클론 프로젝트 환경 설정 가이드

## 📋 목차
1. [로컬 개발 환경 설정](#로컬-개발-환경-설정)
2. [데이터베이스 스키마 적용](#데이터베이스-스키마-적용)
3. [환경별 프로필 설정](#환경별-프로필-설정)
4. [IntelliJ 실행 설정](#intellij-실행-설정)
5. [주요 설정 항목 상세](#주요-설정-항목-상세)

---

## 🛠️ 로컬 개발 환경 설정

### 1단계: 로컬 MySQL 데이터베이스 생성

```sql
-- MySQL에 접속 후 실행
CREATE DATABASE musinsa_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'musinsa_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON musinsa_dev.* TO 'musinsa_user'@'localhost';
FLUSH PRIVILEGES;

-- 테스트용 DB도 함께 생성
CREATE DATABASE musinsa_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON musinsa_test.* TO 'musinsa_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2단계: 환경 변수 파일 생성

프로젝트 루트에 `.env.dev` 파일 생성

```bash
# .env.dev 파일 생성 및 편집
cat > .env.dev << 'EOF'
DB_HOST=localhost
DB_PORT=3306
DB_NAME=musinsa_dev
DB_USERNAME=musinsa_user
DB_PASSWORD=your_password

JWT_SECRET=dev-musinsa-secret-key-for-development-environment-minimum-256-bits-required-1234567890
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

SERVER_PORT=8080
EOF
```

⚠️ **주의**: `.env.dev` 파일은 Git에 커밋되지 않습니다! 각자 로컬에서만 관리하세요.

### 3단계: Gradle 빌드

```bash
# 의존성 다운로드 및 빌드
./gradlew clean build

# 빌드 테스트 (테스트 스킵)
./gradlew clean build -x test
```

---

## 🗄️ 데이터베이스 스키마 적용

### 방법 1: IntelliJ Database Tool 사용 (권장)

1. **View → Tool Windows → Database** 열기
2. 좌측 상단 `+` 버튼 → **Data Source → MySQL** 선택
3. 연결 정보 입력:
   - Host: `localhost`
   - Port: `3306`
   - Database: `musinsa_dev`
   - User: `musinsa_user`
   - Password: 위에서 설정한 비밀번호
4. **Test Connection** 클릭하여 연결 확인
5. `db/schema/` 폴더의 SQL 파일들을 순서대로 실행:
   - `V1__init_schema.sql`
   - `V2__create_payment_tables.sql`
   - ...

### 방법 2: MySQL CLI 사용

```bash
# db/schema 폴더로 이동
cd db/schema

# SQL 파일 실행
mysql -u musinsa_user -p musinsa_dev < V1__init_schema.sql
mysql -u musinsa_user -p musinsa_dev < V2__create_payment_tables.sql
```

---

## 🔧 환경별 프로필 설정

### Dev (로컬 개발) - `application-dev.yml`

**목적**: 로컬에서 개발하며 상세한 로그 확인

| 설정 항목 | 값 | 설명 |
|----------|-----|------|
| **DataSource** |
| Pool Size | 10 (max) / 5 (min) | 로컬 환경에 맞게 작게 설정 |
| Connection Timeout | 30초 | 연결 대기 시간 |
| Leak Detection | 60초 | 커넥션 누수 감지 |
| **JPA** |
| ddl-auto | validate | 스키마 검증만 (자동 생성 안 함) |
| show-sql | true | SQL 콘솔 출력 ✅ |
| generate_statistics | true | Hibernate 통계 수집 |
| **MyBatis** |
| log-impl | Slf4jImpl | 상세 SQL 로깅 ✅ |
| local-cache-scope | STATEMENT | 변경 즉시 반영 |
| **Logging** |
| Application | DEBUG | 애플리케이션 로그 상세 |
| Hibernate | DEBUG/TRACE | SQL + 파라미터 상세 |
| MyBatis | TRACE | 모든 쿼리 + 결과 |
| **기타** |
| Swagger | ✅ Enabled | API 문서 활성화 |
| Actuator | health, metrics | 헬스체크 + 메트릭 |

**주요 로그 출력:**
```
# JPA 예시
Hibernate: SELECT * FROM `Order` WHERE order_id = ?
2024-01-01 12:00:00.123 TRACE o.h.type.descriptor.sql.BasicBinder - binding parameter [1] as [BIGINT] - [123]

# MyBatis 예시
==>  Preparing: SELECT * FROM Payment WHERE order_id = ?
==> Parameters: 123(Long)
<==      Total: 1
```

### Test (테스트) - `application-test.yml`

**목적**: 자동화된 테스트 실행, 빠른 피드백

| 설정 항목 | 값 | 설명 |
|----------|-----|------|
| **DataSource** |
| Pool Size | 5 (max) / 2 (min) | 테스트용 작은 풀 |
| **JPA** |
| ddl-auto | create-drop | 테스트 시작/종료 시 생성/삭제 |
| show-sql | true | 테스트 SQL 확인용 |
| **MyBatis** |
| log-impl | NoLoggingImpl | 로깅 비활성화 (속도) |
| cache-enabled | false | 격리된 테스트 |
| **Logging** |
| Application | INFO | 최소 로깅 |
| Frameworks | WARN | 경고만 출력 |
| **기타** |
| Swagger | ❌ Disabled | 불필요 |
| Actuator | health만 | 최소 설정 |

### Prod (프로덕션) - `application-prod.yml`

**목적**: 최고 성능, 최소 로깅, 최대 보안

| 설정 항목 | 값 | 설명 |
|----------|-----|------|
| **DataSource** |
| Pool Size | 20 (max) / 10 (min) | 대용량 트래픽 대비 |
| Connection Timeout | 30초 | |
| Leak Detection | 120초 | |
| PrepStmt Cache | 250 | 성능 최적화 ⚡ |
| **JPA** |
| ddl-auto | none | 절대 사용 안 함 ⚠️ |
| show-sql | false | 성능 최적화 |
| generate_statistics | false | 성능 최적화 |
| **MyBatis** |
| log-impl | NoLoggingImpl | 로깅 비활성화 |
| cache-enabled | true | 캐시 최대 활용 ⚡ |
| fetch-size | 200 | 대량 조회 최적화 |
| **Logging** |
| Application | INFO | 필수 정보만 |
| Frameworks | WARN | 경고 이상만 |
| File | /var/log/musinsa/ | 파일 로깅 |
| **보안** |
| Swagger | ❌ Disabled | 보안 |
| Error Details | ❌ Disabled | 정보 노출 방지 |
| SSL | ✅ Required | HTTPS 강제 |
| **기타** |
| HTTP/2 | ✅ Enabled | 성능 최적화 |
| Compression | ✅ Enabled | 대역폭 절약 |
| Graceful Shutdown | 30초 | 안전한 종료 |

---

## 🚀 IntelliJ 실행 설정

### 방법 1: Run Configuration 설정 (기본)

1. **Run → Edit Configurations**
2. **+** 버튼 → **Spring Boot** 선택
3. 설정:
   - **Name**: `Musinsa Dev Server`
   - **Main class**: `com.mudosa.musinsa.ServerApplication`
   - **Active profiles**: `dev`
   - **Environment variables**: 
     ```
     DB_HOST=localhost;DB_PORT=3306;DB_NAME=musinsa_dev;DB_USERNAME=musinsa_user;DB_PASSWORD=your_password;JWT_SECRET=dev-musinsa-secret-key-for-development-environment-minimum-256-bits-required-1234567890
     ```
   - **VM options** (선택사항):
     ```
     -Xms512m -Xmx1024m -Dfile.encoding=UTF-8
     ```

### 방법 2: EnvFile 플러그인 사용 (권장) ⭐

1. **Preferences → Plugins → EnvFile** 설치
2. **Run → Edit Configurations**
3. **EnvFile** 탭 활성화
4. **+** 버튼 → `.env.dev` 파일 추가
5. ✅ Enable EnvFile 체크

**장점:**
- 환경 변수 관리 편리
- `.env` 파일 하나로 관리
- 프로필 전환 간편

### 애플리케이션 실행

```bash
# Gradle로 실행
./gradlew bootRun --args='--spring.profiles.active=dev'

# 또는 IDE에서 실행
# Run 'ServerApplication' (Shift + F10)
```

### 실행 확인

```bash
# 1. 애플리케이션 정상 기동 확인
curl http://localhost:8080/actuator/health

# 2. Swagger UI 접속
http://localhost:8080/swagger-ui.html

# 3. API Docs 확인
http://localhost:8080/api-docs
```

---

## 📊 주요 설정 항목 상세

### 1. HikariCP 커넥션 풀 설정

```yaml
hikari:
  maximum-pool-size: 20        # 최대 커넥션 수
  minimum-idle: 10             # 최소 유휴 커넥션
  connection-timeout: 30000    # 커넥션 대기 시간 (ms)
  idle-timeout: 300000         # 유휴 커넥션 타임아웃
  max-lifetime: 1800000        # 커넥션 최대 수명 (30분)
  leak-detection-threshold: 60000  # 누수 감지 시간
```

**권장값:**
- **Dev**: max=10, min=5 (로컬 부담 최소화)
- **Test**: max=5, min=2 (빠른 테스트)
- **Prod**: max=20, min=10 (트래픽 대응)

### 2. JPA 설정

```yaml
jpa:
  hibernate:
    ddl-auto: validate  # none, validate, update, create, create-drop
  show-sql: true        # SQL 콘솔 출력
  properties:
    hibernate:
      format_sql: true              # SQL 포맷팅
      use_sql_comments: true        # 주석 추가
      jdbc.batch_size: 100          # 배치 처리 크기
      generate_statistics: true      # 통계 수집
```

**ddl-auto 옵션:**
- `none`: 아무것도 안 함
- `validate`: 스키마 검증만 (운영 권장)
- `update`: 변경 사항 반영 (위험! 사용 지양)
- `create`: 시작 시 생성
- `create-drop`: 시작 시 생성, 종료 시 삭제 (테스트용)

### 3. MyBatis 설정

```yaml
mybatis:
  mapper-locations: classpath:/mappers/**/*.xml
  configuration:
    map-underscore-to-camel-case: true  # snake_case → camelCase
    cache-enabled: true                  # 2차 캐시
    default-executor-type: REUSE        # PreparedStatement 재사용
    log-impl: Slf4jImpl                 # 로깅 구현체
```

**Executor 타입:**
- `SIMPLE`: 매번 새 PreparedStatement
- `REUSE`: PreparedStatement 재사용 (권장)
- `BATCH`: 배치 업데이트 최적화

### 4. 로깅 레벨

```yaml
logging:
  level:
    root: info                           # 루트 로거
    com.mudosa.musinsa: debug           # 애플리케이션
    org.hibernate.SQL: debug            # JPA SQL
    org.hibernate.type: trace           # JPA 파라미터
    com.mudosa.musinsa.domain.*.mapper: trace  # MyBatis
```

**레벨 종류:**
- `TRACE`: 모든 정보 (가장 상세)
- `DEBUG`: 디버그 정보
- `INFO`: 일반 정보
- `WARN`: 경고
- `ERROR`: 에러만

---

## 📝 주의사항

### 🔒 보안

- ✅ `.env` 파일은 절대 Git에 커밋하지 않습니다
- ✅ `application-*.yml`에는 민감한 정보를 하드코딩하지 않습니다
- ✅ JWT Secret은 프로덕션에서 256bit 이상의 강력한 키 사용
- ✅ 프로덕션 환경 변수는 서버에서만 관리

```bash
# 안전한 JWT Secret 생성
openssl rand -base64 64
```

### 🗄️ 데이터베이스

- ✅ 로컬 개발 시 `ddl-auto: validate` 사용 (자동 생성 안 함)
- ✅ 스키마 변경은 SQL 파일로 관리하고 팀원과 공유
- ✅ 프로덕션에서는 **절대** `ddl-auto` 사용하지 않음
- ✅ 커넥션 풀 크기는 실제 트래픽에 맞게 조정

### 👥 협업

- ✅ 스키마 변경 시 `db/schema/` 폴더에 버전별 SQL 파일 생성
- ✅ 변경 사항을 Git으로 공유
- ✅ 팀원은 새로운 SQL 파일을 pull 받아 수동으로 실행
- ✅ 각자의 `.env.dev` 파일은 로컬 환경에 맞게 관리

---

## 🆘 문제 해결

### 데이터베이스 연결 실패

```
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
```

**해결책:**
1. MySQL 서비스가 실행 중인지 확인
   ```bash
   # Mac
   brew services list
   
   # Linux
   systemctl status mysql
   ```
2. `.env.dev` 파일의 DB 정보 확인
3. 방화벽 설정 확인

### JWT Secret 길이 오류

```
WeakKeyException: The signing key's size is ... bit(s) which is not secure enough
```

**해결책:**
- `.env.dev`의 `JWT_SECRET`을 256bit (43자) 이상으로 변경

### 스키마 검증 실패

```
Schema-validation: wrong column type encountered
```

**해결책:**
1. Entity와 실제 DB 스키마 불일치 확인
2. SQL 파일을 다시 확인하고 실행
3. `ddl-auto: validate` 대신 임시로 `none` 사용 후 확인

### HikariCP 커넥션 풀 고갈

```
SQLTransientConnectionException: HikariPool - Connection is not available
```

**해결책:**
1. `maximum-pool-size` 증가
2. 커넥션 누수 확인 (`leak-detection-threshold`)
3. 트랜잭션이 제대로 종료되는지 확인

### MyBatis Mapper를 찾을 수 없음

```
BindingException: Invalid bound statement (not found)
```

**해결책:**
1. Mapper XML의 namespace가 Interface 패키지명과 일치하는지 확인
2. `mapper-locations` 설정 확인
3. XML 파일이 `resources/mappers/` 하위에 있는지 확인

---

## 📚 참고 자료

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [HikariCP Documentation](https://github.com/brettwooldridge/HikariCP)
- [MyBatis Documentation](https://mybatis.org/mybatis-3/)
- [Hibernate Documentation](https://hibernate.org/orm/documentation/)

---

## 🤝 기여 및 문의

설정 관련 이슈나 개선 사항은 팀 슬랙 채널에 공유해주세요!

## 📞 문의처

- Slack: #musinsa-backend
- Email: team@musinsa.com
