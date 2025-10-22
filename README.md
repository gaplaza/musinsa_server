# 무신사 클론 프로젝트 - 서버

> 무신사 쇼핑몰 클론 코딩 프로젝트의 백엔드 서버입니다.

## 🚀 빠른 시작

### 1. 환경 설정

```bash
# 1. 레포지토리 클론
git clone <repository-url>
cd server

# 2. 환경 변수 파일 생성
cp .env.dev.example .env.dev
# .env.dev 파일을 열어서 DB 정보 수정

# 3. MySQL 데이터베이스 생성
mysql -u root -p
> CREATE DATABASE musinsa_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
> CREATE USER 'musinsa_user'@'localhost' IDENTIFIED BY 'your_password';
> GRANT ALL PRIVILEGES ON musinsa_dev.* TO 'musinsa_user'@'localhost';
```

### 2. 스키마 적용

IntelliJ Database Tool에서 `db/schema/` 폴더의 SQL 파일들을 순서대로 실행

### 3. 애플리케이션 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

또는 IntelliJ에서 `ServerApplication` 실행 (프로필: dev)

### 4. API 문서 확인

서버 실행 후: http://localhost:8080/swagger-ui.html

---

## 📚 문서

- **[환경 설정 가이드](docs/SETUP.md)** - 상세한 환경 설정 방법
- **[API 명세서](docs/API.md)** - REST API 문서 (작성 예정)
- **[데이터베이스 스키마](db/schema/)** - DB 스키마 정의

---

## 🛠️ 기술 스택

- **Java 21**
- **Spring Boot 3.5.6**
- **Spring Data JPA**
- **Spring Security + JWT**
- **MySQL 8.0**
- **MyBatis 3.0.5**
- **Swagger (SpringDoc)**

---

## 📁 프로젝트 구조

```
server/
├── src/
│   ├── main/
│   │   ├── java/com/mudosa/musinsa/
│   │   │   ├── domain/          # 도메인 모델 (Entity, VO)
│   │   │   ├── repository/      # 데이터 접근 계층
│   │   │   ├── service/         # 비즈니스 로직
│   │   │   ├── controller/      # API 컨트롤러
│   │   │   ├── config/          # 설정 클래스
│   │   │   └── common/          # 공통 유틸, 예외 처리
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-test.yml
│   │       └── application-prod.yml
│   └── test/
├── db/
│   └── schema/                   # 데이터베이스 스키마
├── docs/                         # 프로젝트 문서
├── .env.dev.example              # 환경 변수 예시
└── build.gradle
```

---

## 👥 팀원 및 역할

| 이름 | 역할 | 담당 기능 |
|-----|------|----------|
| - | 주문/결제 | Order, Payment, PaymentLog |
| - | 상품 | Product, ProductOption |
| - | 회원 | User, Auth |
| - | 브랜드 | Brand |
| - | 쿠폰/이벤트 | Coupon, Event |
| - | 기타 | - |

---

## 🔄 개발 워크플로우

### 브랜치 전략
- `main`: 프로덕션 배포 브랜치
- `develop`: 개발 통합 브랜치
- `feature/*`: 기능 개발 브랜치
- `hotfix/*`: 긴급 수정 브랜치

### 스키마 변경 프로세스
1. `db/schema/Vx__description.sql` 파일 생성
2. 로컬에서 테스트
3. Git 커밋 & PR 생성
4. 리뷰 후 머지
5. 팀원들이 Pull 받아서 수동으로 SQL 실행

---

## 📝 커밋 컨벤션

```
feat: 새로운 기능 추가
fix: 버그 수정
docs: 문서 수정
style: 코드 포맷팅 (기능 변경 없음)
refactor: 코드 리팩토링
test: 테스트 코드 추가/수정
chore: 빌드 설정, 패키지 매니저 수정
```

---

## 🧪 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 테스트 클래스
./gradlew test --tests OrderServiceTest

# 테스트 커버리지
./gradlew test jacocoTestReport
```

---

## 📦 빌드 & 배포

```bash
# JAR 파일 생성
./gradlew build

# Docker 이미지 생성 (추후 추가 예정)
docker build -t musinsa-server:latest .

# 프로덕션 실행 (환경 변수 필요)
java -jar -Dspring.profiles.active=prod build/libs/server-0.0.1-SNAPSHOT.jar
```

---

## 🐛 이슈 트래킹

GitHub Issues를 사용하여 버그 및 개선사항 관리

---

## 📄 라이선스

MIT License (또는 프로젝트에 맞는 라이선스 명시)
