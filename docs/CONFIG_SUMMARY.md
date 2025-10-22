# 설정 파일 완벽 가이드 - 빠른 참조

## 🎯 핵심 요약

### 프로필 선택 기준

```
로컬 개발 → dev
테스트 실행 → test  
프로덕션 배포 → prod
```

---

## 📦 의존성 (build.gradle)

### 주요 라이브러리

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| Spring Boot | 3.5.6 | 프레임워크 |
| Java | 21 | 언어 |
| JPA | Starter | ORM (CRUD) |
| MyBatis | 3.0.5 | SQL Mapper (복잡한 쿼리) |
| MySQL | Connector | 데이터베이스 |
| Security | Starter | 인증/인가 |
| JWT | 0.12.6 | 토큰 인증 |
| Swagger | 2.3.0 | API 문서화 |
| Lombok | Starter | 보일러플레이트 제거 |

---

## 🔧 공통 설정 (application.yml)

### 반드시 포함되어야 하는 설정

```yaml
# 1. JPA 기본 설정
spring.jpa:
  open-in-view: false  # ⚠️ 중요: OSIV 비활성화
  properties.hibernate:
    format_sql: true
    jdbc.batch_size: 100

# 2. MyBatis 기본 설정
mybatis:
  mapper-locations: classpath:/mappers/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    default-executor-type: REUSE

# 3. Jackson 날짜 형식
spring.jackson:
  time-zone: Asia/Seoul
  date-format: yyyy-MM-dd HH:mm:ss

# 4. Multipart 파일 업로드
spring.servlet.multipart:
  max-file-size: 10MB
  max-request-size: 10MB
```

---

## 🎨 프로필별 핵심 차이점

### Dev (개발)

```yaml
# 목적: 디버깅과 개발 편의성
datasource.hikari.maximum-pool-size: 10
jpa.hibernate.ddl-auto: validate  # ⚠️ 자동 생성 안 함
jpa.show-sql: true                # ✅ SQL 출력
mybatis.configuration.log-impl: Slf4jImpl
logging.level:
  com.mudosa.musinsa: debug       # ✅ 상세 로그
  org.hibernate.SQL: debug
  *.mapper: trace                 # ✅ MyBatis 상세
```

**특징**: 모든 것이 보인다 🔍

### Test (테스트)

```yaml
# 목적: 빠른 테스트 실행
datasource.hikari.maximum-pool-size: 5
jpa.hibernate.ddl-auto: create-drop  # 테스트용
mybatis.configuration.log-impl: NoLoggingImpl
logging.level:
  root: warn                      # 최소 로깅
```

**특징**: 빠르고 격리됨 ⚡

### Prod (프로덕션)

```yaml
# 목적: 최고 성능과 보안
datasource.hikari:
  maximum-pool-size: 20
  data-source-properties:
    cachePrepStmts: true          # ⚡ 성능 최적화
    prepStmtCacheSize: 250
jpa.hibernate.ddl-auto: none      # ⚠️ 절대 사용 안 함
jpa.show-sql: false               # SQL 비활성화
mybatis.configuration:
  cache-enabled: true             # ⚡ 캐시 활용
  default-fetch-size: 200
springdoc.swagger-ui.enabled: false  # 🔒 보안
logging.level:
  root: warn                      # 최소 로깅
  com.mudosa.musinsa: info
```

**특징**: 빠르고 안전함 🚀🔒

---

## 🌍 환경 변수 (.env 파일)

### .env.dev (로컬 개발)

```bash
# 필수 변수
DB_HOST=localhost
DB_PORT=3306
DB_NAME=musinsa_dev
DB_USERNAME=root
DB_PASSWORD=your_password

# JWT (개발용 - 아무거나 OK)
JWT_SECRET=dev-musinsa-secret-key-for-development-environment-minimum-256-bits-required-1234567890
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# 서버
SERVER_PORT=8080
```

### .env.prod (프로덕션)

```bash
# ⚠️ 절대 Git에 커밋하지 말 것!
DB_HOST=production-db.rds.amazonaws.com
DB_NAME=musinsa_prod
DB_USERNAME=prod_user
DB_PASSWORD=SUPER_STRONG_PASSWORD_HERE

# JWT (256bit 이상 필수!)
JWT_SECRET=GENERATE_WITH_openssl_rand_base64_64
JWT_ACCESS_EXPIRATION=1800000  # 30분
JWT_REFRESH_EXPIRATION=1209600000  # 14일
```

**안전한 시크릿 생성:**
```bash
openssl rand -base64 64
```

---

## 🚀 빠른 시작

### 1. 환경 구성

```bash
# 1) MySQL DB 생성
CREATE DATABASE musinsa_dev CHARACTER SET utf8mb4;

# 2) .env.dev 파일 생성
cat > .env.dev << EOF
DB_HOST=localhost
DB_PORT=3306
DB_NAME=musinsa_dev
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=dev-musinsa-secret-key-for-development-environment-minimum-256-bits-required-1234567890
SERVER_PORT=8080
EOF

# 3) 스키마 적용 (IntelliJ DB Tool 사용)
# db/schema/*.sql 파일들을 순서대로 실행
```

### 2. 애플리케이션 실행

```bash
# Gradle로 실행
./gradlew bootRun --args='--spring.profiles.active=dev'

# 또는 IDE에서 Run Configuration 설정 후 실행
```

### 3. 확인

```bash
# Health Check
curl http://localhost:8080/actuator/health

# Swagger UI
http://localhost:8080/swagger-ui.html
```

---

## ⚠️ 중요 체크리스트

### 개발 시작 전

- [ ] MySQL이 설치되고 실행 중인가?
- [ ] `.env.dev` 파일이 생성되었는가?
- [ ] DB 스키마가 적용되었는가?
- [ ] `ddl-auto: validate`로 설정되었는가?

### Git 커밋 전

- [ ] `.env.dev` 파일이 `.gitignore`에 있는가?
- [ ] 하드코딩된 비밀번호가 없는가?
- [ ] 테스트가 통과하는가?

### 프로덕션 배포 전

- [ ] `spring.profiles.active=prod`로 설정했는가?
- [ ] 환경 변수가 서버에 설정되었는가?
- [ ] JWT Secret이 256bit 이상인가?
- [ ] `ddl-auto: none`인가?
- [ ] Swagger가 비활성화되었는가?
- [ ] SSL이 활성화되었는가?

---

## 🐛 흔한 문제 해결

### 1. 데이터베이스 연결 실패

```
CommunicationsException: Communications link failure
```

**해결:**
```bash
# MySQL 실행 확인
brew services list  # Mac
systemctl status mysql  # Linux

# .env.dev 파일 확인
cat .env.dev
```

### 2. JWT Secret 길이 오류

```
WeakKeyException: The signing key's size is ... bit(s)
```

**해결:**
```bash
# 더 긴 Secret 생성
openssl rand -base64 64
```

### 3. 스키마 검증 실패

```
Schema-validation: wrong column type
```

**해결:**
```bash
# 1. Entity와 DB 스키마 일치 확인
# 2. SQL 파일 재실행
mysql -u root -p musinsa_dev < db/schema/V1__init.sql
```

### 4. Mapper를 찾을 수 없음

```
BindingException: Invalid bound statement
```

**해결:**
```yaml
# application.yml 확인
mybatis:
  mapper-locations: classpath:/mappers/**/*.xml  # 경로 확인
```

### 5. 커넥션 풀 고갈

```
HikariPool - Connection is not available
```

**해결:**
```yaml
# application-dev.yml
hikari:
  maximum-pool-size: 20  # 증가
  leak-detection-threshold: 60000  # 누수 감지
```

---

## 📊 성능 팁

### JPA 최적화

```java
// ✅ 좋은 예: 배치 처리
@Transactional
public void saveOrders(List<Order> orders) {
    for (int i = 0; i < orders.size(); i++) {
        orderRepository.save(orders.get(i));
        if (i % 100 == 0) {
            entityManager.flush();
            entityManager.clear();
        }
    }
}

// ❌ 나쁜 예: N+1 문제
// LAZY 로딩으로 인한 추가 쿼리
for (Order order : orders) {
    order.getOrderProducts().size();  // 각 주문마다 추가 쿼리!
}

// ✅ 해결: JOIN FETCH
@Query("SELECT o FROM Order o JOIN FETCH o.orderProducts")
List<Order> findAllWithProducts();
```

### MyBatis 최적화

```xml
<!-- ✅ 좋은 예: ResultMap 재사용 -->
<resultMap id="OrderMap" type="Order">
    <id property="orderId" column="order_id"/>
    ...
</resultMap>

<!-- ❌ 나쁜 예: SELECT * -->
SELECT * FROM `Order`  <!-- 불필요한 컬럼까지 조회 -->

<!-- ✅ 좋은 예: 필요한 컬럼만 -->
SELECT order_id, total_price, created_at FROM `Order`
```

### HikariCP 모니터링

```yaml
# Dev 환경에서 커넥션 풀 모니터링
logging.level:
  com.zaxxer.hikari: debug
  com.zaxxer.hikari.pool.HikariPool: debug
```

---

## 🎓 학습 경로

### 1단계: 기본 이해
- [ ] 프로필의 개념
- [ ] 환경 변수 사용법
- [ ] JPA vs MyBatis 차이

### 2단계: 실전 적용
- [ ] 로컬 환경 구축
- [ ] 스키마 관리
- [ ] API 개발 및 테스트

### 3단계: 최적화
- [ ] 쿼리 성능 튜닝
- [ ] 커넥션 풀 튜닝
- [ ] 캐싱 전략

### 4단계: 운영
- [ ] 프로덕션 배포
- [ ] 모니터링 및 로깅
- [ ] 장애 대응

---

## 📚 더 읽어보기

### 공식 문서
- [Spring Boot Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [MyBatis Configuration](https://mybatis.org/mybatis-3/configuration.html)

### 팀 문서
- [상세 환경 설정 가이드](SETUP.md)
- [프로필 비교표](PROFILE_COMPARISON.md)
- [MyBatis 사용 가이드](MYBATIS_GUIDE.md)

---

## 💬 질문이 있다면?

- **Slack**: #musinsa-backend
- **이슈**: GitHub Issues
- **문서 개선 제안**: Pull Request 환영!

---

**마지막 업데이트**: 2024-01-01  
**작성자**: Backend Team
