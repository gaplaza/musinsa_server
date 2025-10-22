# 무신사 클론 프로젝트 환경 설정 가이드

## 📋 목차
1. [로컬 개발 환경 설정](#로컬-개발-환경-설정)
2. [데이터베이스 스키마 적용](#데이터베이스-스키마-적용)
3. [환경별 프로필 설정](#환경별-프로필-설정)
4. [IntelliJ 실행 설정](#intellij-실행-설정)

---

## 🛠️ 로컬 개발 환경 설정

### 1단계: 로컬 MySQL 데이터베이스 생성

```sql
-- MySQL에 접속 후 실행
CREATE DATABASE musinsa_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'musinsa_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON musinsa_dev.* TO 'musinsa_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2단계: 환경 변수 파일 생성

프로젝트 루트에 `.env.dev` 파일 생성 (이미 `.env.dev` 파일이 있다면 복사해서 사용)

```bash
# .env.dev 파일 복사
cp .env.dev.example .env.dev

# 또는 직접 생성
cat > .env.dev << 'EOF'
DB_HOST=localhost
DB_PORT=3306
DB_NAME=musinsa_dev
DB_USERNAME=musinsa_user
DB_PASSWORD=your_password

JWT_SECRET=dev-musinsa-secret-key-for-development-environment-minimum-256-bits-required
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000
EOF
```

⚠️ **주의**: `.env.dev` 파일은 Git에 커밋되지 않습니다! 각자 로컬에서만 관리하세요.

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

### Dev (로컬 개발)
```yaml
spring.profiles.active=dev
```
- SQL 로그 출력: ✅
- DDL Auto: validate (스키마 검증만)
- 커넥션 풀: 10개

### Test (테스트)
```yaml
spring.profiles.active=test
```
- SQL 로그 출력: ✅
- DDL Auto: create-drop (테스트 시작/종료 시 자동 생성/삭제)
- 커넥션 풀: 5개

### Prod (프로덕션)
```yaml
spring.profiles.active=prod
```
- SQL 로그 출력: ❌
- DDL Auto: none
- 커넥션 풀: 20개
- Swagger: 비활성화

---

## 🚀 IntelliJ 실행 설정

### 방법 1: Run Configuration 설정

1. **Run → Edit Configurations**
2. **+** 버튼 → **Spring Boot** 선택
3. 설정:
   - **Name**: `Musinsa Dev Server`
   - **Main class**: `com.mudosa.musinsa.ServerApplication`
   - **Active profiles**: `dev`
   - **Environment variables**: 
     ```
     DB_HOST=localhost;DB_PORT=3306;DB_NAME=musinsa_dev;DB_USERNAME=musinsa_user;DB_PASSWORD=your_password;JWT_SECRET=dev-musinsa-secret-key-for-development-environment-minimum-256-bits-required
     ```
   - **VM options** (선택사항):
     ```
     -Xms512m -Xmx1024m
     ```

### 방법 2: EnvFile 플러그인 사용 (권장)

1. **Preferences → Plugins → EnvFile** 설치
2. **Run → Edit Configurations**
3. **EnvFile** 탭 활성화
4. **+** 버튼 → `.env.dev` 파일 추가

---

## 📝 주의사항

### 🔒 보안
- ✅ `.env` 파일은 절대 Git에 커밋하지 않습니다
- ✅ `application-*.yml`에는 민감한 정보를 하드코딩하지 않습니다
- ✅ JWT Secret은 프로덕션에서 256bit 이상의 강력한 키 사용

### 🗄️ 데이터베이스
- ✅ 로컬 개발 시 `ddl-auto: validate` 사용 (자동 생성 안 함)
- ✅ 스키마 변경은 SQL 파일로 관리하고 팀원과 공유
- ✅ 프로덕션에서는 절대 `ddl-auto` 사용하지 않음

### 👥 협업
- ✅ 스키마 변경 시 `db/schema/` 폴더에 버전별 SQL 파일 생성
- ✅ 변경 사항을 Git으로 공유
- ✅ 팀원은 새로운 SQL 파일을 pull 받아 수동으로 실행

---

## 🆘 문제 해결

### 데이터베이스 연결 실패
```
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
```
→ MySQL 서비스가 실행 중인지 확인: `brew services list` (Mac) 또는 `systemctl status mysql` (Linux)

### JWT Secret 길이 오류
```
WeakKeyException: The signing key's size is ... bit(s) which is not secure enough
```
→ `.env.dev`의 `JWT_SECRET`을 256bit 이상으로 변경

### 스키마 검증 실패
```
Schema-validation: wrong column type encountered
```
→ Entity와 실제 DB 스키마 불일치. SQL 파일을 다시 확인하고 실행

---

## 📞 문의

문제가 있으면 팀 슬랙 채널 또는 이슈 트래커에 등록해주세요!
