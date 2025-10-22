# MyBatis 설정 및 사용 가이드

## 📋 목차
1. [MyBatis 설정 개요](#mybatis-설정-개요)
2. [프로필별 설정](#프로필별-설정)
3. [Mapper 작성 가이드](#mapper-작성-가이드)
4. [JPA vs MyBatis 사용 기준](#jpa-vs-mybatis-사용-기준)
5. [Best Practices](#best-practices)

---

## 🛠️ MyBatis 설정 개요

### 공통 설정 (`application.yml`)

```yaml
mybatis:
  # Mapper XML 위치
  mapper-locations: classpath:/mappers/**/*.xml
  
  # Type Alias 패키지 (도메인별)
  type-aliases-package: >
    com.mudosa.musinsa.domain.order.mapper,
    com.mudosa.musinsa.domain.payment.mapper,
    ...
  
  configuration:
    map-underscore-to-camel-case: true    # snake_case → camelCase
    use-actual-param-name: true           # Java 8+ 파라미터 이름 사용
    jdbc-type-for-null: NULL              # NULL 값 처리
    lazy-loading-enabled: true            # 지연 로딩
    cache-enabled: true                   # 캐시 활성화
    default-executor-type: REUSE          # PreparedStatement 재사용
```

### 주요 설정 항목 설명

| 설정 | 값 | 설명 |
|------|-----|------|
| `map-underscore-to-camel-case` | true | DB의 `order_id` → Java의 `orderId` 자동 변환 |
| `use-actual-param-name` | true | `#{userId}` 형태로 파라미터 사용 가능 |
| `jdbc-type-for-null` | NULL | NULL 값 INSERT/UPDATE 시 명시적 타입 지정 불필요 |
| `lazy-loading-enabled` | true | N+1 문제 방지를 위한 지연 로딩 |
| `cache-enabled` | true | 2차 캐시 활성화 (성능 향상) |
| `default-executor-type` | REUSE | PreparedStatement 재사용으로 성능 최적화 |

---

## 🔧 프로필별 설정

### Dev (로컬 개발)

```yaml
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl  # SLF4J 로깅
    local-cache-scope: STATEMENT                         # 캐시 비활성화

logging:
  level:
    com.mudosa.musinsa.domain.*.mapper: trace  # 모든 SQL 및 파라미터 출력
```

**특징:**
- ✅ 모든 SQL 쿼리와 바인딩 파라미터 상세 로깅
- ✅ 로컬 캐시 비활성화로 변경 사항 즉시 반영
- ✅ 디버깅에 최적화

### Test (테스트)

```yaml
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
    cache-enabled: false
    local-cache-scope: STATEMENT
```

**특징:**
- ✅ 로깅 최소화 (테스트 속도 향상)
- ✅ 캐시 비활성화 (격리된 테스트)
- ✅ 빠른 테스트 실행

### Prod (프로덕션)

```yaml
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
    cache-enabled: true
    local-cache-scope: SESSION
    default-fetch-size: 200
    default-statement-timeout: 60
```

**특징:**
- ✅ 로깅 완전 비활성화 (성능 최우선)
- ✅ 캐시 최대 활용
- ✅ 최적화된 페치 사이즈 및 타임아웃

---

## 📝 Mapper 작성 가이드

### 1. 디렉토리 구조

```
src/main/resources/mappers/
├── order/
│   └── OrderMapper.xml
├── payment/
│   ├── PaymentMapper.xml
│   └── PaymentLogMapper.xml
├── user/
│   └── UserMapper.xml
└── product/
    ├── ProductMapper.xml
    └── ProductOptionMapper.xml
```

### 2. Mapper Interface

```java
package com.mudosa.musinsa.domain.order.mapper;

import com.mudosa.musinsa.domain.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    
    // 단일 조회
    Order findById(@Param("orderId") Long orderId);
    
    // 목록 조회 (페이징)
    List<Order> findByUserId(
        @Param("userId") Integer userId,
        @Param("offset") int offset,
        @Param("limit") int limit
    );
    
    // 통계 쿼리
    List<Map<String, Object>> countByStatus(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // 복잡한 조인 쿼리
    Map<String, Object> findOrderWithDetails(@Param("orderId") Long orderId);
}
```

### 3. Mapper XML 작성 규칙

#### ✅ ResultMap 활용

```xml
<resultMap id="OrderResultMap" type="com.mudosa.musinsa.domain.order.entity.Order">
    <id property="orderId" column="order_id"/>
    <result property="userId" column="user_id"/>
    <!-- map-underscore-to-camel-case=true면 생략 가능하지만 명시적으로 작성 권장 -->
</resultMap>
```

#### ✅ 파라미터 바인딩

```xml
<!-- 단일 파라미터 -->
<select id="findById" parameterType="long" resultMap="OrderResultMap">
    SELECT * FROM `Order` WHERE order_id = #{orderId}
</select>

<!-- 여러 파라미터 -->
<select id="findByCondition" resultMap="OrderResultMap">
    SELECT * FROM `Order`
    WHERE user_id = #{userId}
      AND created_at BETWEEN #{startDate} AND #{endDate}
</select>
```

#### ✅ 동적 SQL

```xml
<select id="searchOrders" resultMap="OrderResultMap">
    SELECT * FROM `Order`
    WHERE 1=1
    <if test="userId != null">
        AND user_id = #{userId}
    </if>
    <if test="orderStatus != null">
        AND order_status = #{orderStatus}
    </if>
    <if test="startDate != null and endDate != null">
        AND created_at BETWEEN #{startDate} AND #{endDate}
    </if>
    ORDER BY created_at DESC
    <if test="limit != null">
        LIMIT #{offset}, #{limit}
    </if>
</select>
```

#### ✅ 복잡한 조인 쿼리

```xml
<select id="findOrderWithDetails" resultType="map">
    SELECT 
        o.order_id,
        o.order_no,
        o.total_price,
        sc.status_code AS status_name,
        COUNT(op.order_product_id) AS product_count,
        SUM(op.product_quantity) AS total_quantity
    FROM `Order` o
    LEFT JOIN status_codes sc ON o.order_status = sc.status_code_id
    LEFT JOIN OrderProduct op ON o.order_id = op.order_id
    WHERE o.order_id = #{orderId}
    GROUP BY o.order_id, o.order_no, o.total_price, sc.status_code
</select>
```

---

## 🎯 JPA vs MyBatis 사용 기준

### JPA 사용 (권장 상황)

✅ **CRUD 위주의 단순 쿼리**
```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Integer userId);
    Optional<Order> findByOrderNo(String orderNo);
}
```

✅ **도메인 로직이 중심**
- Entity 간 연관관계 관리
- 영속성 컨텍스트 활용
- 객체지향적 설계

✅ **트랜잭션 내 여러 작업**
```java
@Transactional
public void createOrder(OrderRequest request) {
    Order order = orderRepository.save(createOrderEntity(request));
    orderProducts.forEach(orderProductRepository::save);
    paymentService.processPayment(order);
}
```

### MyBatis 사용 (권장 상황)

✅ **복잡한 조인 및 집계 쿼리**
```xml
<!-- 3개 이상 테이블 조인 + GROUP BY + HAVING -->
<select id="getOrderStatistics" resultType="map">
    SELECT 
        DATE(o.created_at) AS order_date,
        COUNT(DISTINCT o.order_id) AS order_count,
        COUNT(DISTINCT o.user_id) AS unique_users,
        SUM(o.final_payment_amount) AS total_revenue,
        AVG(op.product_quantity) AS avg_quantity
    FROM `Order` o
    JOIN OrderProduct op ON o.order_id = op.order_id
    JOIN Payment p ON o.order_id = p.order_id
    JOIN status_codes sc ON p.payment_status = sc.status_code_id
    WHERE sc.status_code = 'APPROVED'
      AND o.created_at BETWEEN #{startDate} AND #{endDate}
    GROUP BY DATE(o.created_at)
    HAVING total_revenue > 1000000
    ORDER BY order_date DESC
</select>
```

✅ **대량 데이터 조회 (읽기 전용)**
```xml
<!-- 페이징 처리된 대량 조회 -->
<select id="findOrdersForExport" resultType="map">
    SELECT 
        o.*,
        u.username,
        b.brand_name
    FROM `Order` o
    JOIN User u ON o.user_id = u.user_id
    JOIN Brand b ON o.brand_id = b.brand_id
    LIMIT #{offset}, #{limit}
</select>
```

✅ **통계 및 리포팅 쿼리**
```xml
<select id="getMonthlyReport" resultType="map">
    SELECT 
        YEAR(created_at) AS year,
        MONTH(created_at) AS month,
        COUNT(*) AS order_count,
        SUM(final_payment_amount) AS revenue
    FROM `Order`
    WHERE created_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH)
    GROUP BY YEAR(created_at), MONTH(created_at)
    ORDER BY year DESC, month DESC
</select>
```

✅ **동적 쿼리 (조건이 많을 때)**
```xml
<select id="searchWithFilters" resultMap="OrderResultMap">
    SELECT * FROM `Order`
    WHERE 1=1
    <if test="userId != null">AND user_id = #{userId}</if>
    <if test="brandId != null">AND brand_id = #{brandId}</if>
    <if test="minPrice != null">AND total_price >= #{minPrice}</if>
    <if test="maxPrice != null">AND total_price &lt;= #{maxPrice}</if>
    <if test="orderStatus != null">AND order_status = #{orderStatus}</if>
</select>
```

---

## 💡 Best Practices

### 1. Mapper Interface 네이밍 규칙

```java
// ✅ 좋은 예
OrderMapper.findById()
OrderMapper.findByUserId()
OrderMapper.searchWithFilters()
OrderMapper.countByStatus()

// ❌ 나쁜 예
OrderMapper.getOrder()      // get보다 find 사용
OrderMapper.selectAll()     // 구체적인 이름 사용
OrderMapper.query1()        // 의미 없는 이름
```

### 2. ResultMap vs ResultType

```xml
<!-- ResultMap: 복잡한 매핑, 재사용 가능 -->
<resultMap id="OrderResultMap" type="Order">
    <id property="orderId" column="order_id"/>
    <result property="totalPrice" column="total_price"/>
</resultMap>

<!-- ResultType: 단순 매핑 (map-underscore-to-camel-case가 처리) -->
<select id="findSimple" resultType="Order">
    SELECT * FROM `Order` WHERE order_id = #{orderId}
</select>

<!-- Map 타입: 통계 쿼리, DTO 불필요 -->
<select id="getStats" resultType="map">
    SELECT COUNT(*) as count, SUM(amount) as total FROM Payment
</select>
```

### 3. 파라미터 전달

```java
// ✅ @Param 사용 (명시적, 권장)
List<Order> findByCondition(
    @Param("userId") Integer userId,
    @Param("status") Integer status
);

// ⚠️ Map 사용 (가독성 떨어짐, 지양)
List<Order> findByCondition(Map<String, Object> params);

// ⚠️ 단일 파라미터는 @Param 생략 가능하지만 명시 권장
Order findById(Long orderId);  // XML: #{orderId}
```

### 4. SQL 인젝션 방지

```xml
<!-- ✅ 안전: #{} 사용 (PreparedStatement) -->
<select id="findById">
    SELECT * FROM `Order` WHERE order_id = #{orderId}
</select>

<!-- ⚠️ 위험: ${} 사용 (문자열 치환) -->
<select id="findByColumn">
    SELECT * FROM `Order` WHERE ${columnName} = #{value}
    <!-- columnName에 "1=1 OR" 같은 값이 들어올 수 있음! -->
</select>

<!-- ✅ 동적 컬럼은 코드에서 검증 후 사용 -->
```

### 5. 페이징 처리

```xml
<!-- MySQL -->
<select id="findWithPaging" resultMap="OrderResultMap">
    SELECT * FROM `Order`
    WHERE user_id = #{userId}
    ORDER BY created_at DESC
    LIMIT #{offset}, #{limit}
</select>
```

```java
// Service Layer
public Page<Order> getOrders(Integer userId, int page, int size) {
    int offset = page * size;
    List<Order> orders = orderMapper.findWithPaging(userId, offset, size);
    int total = orderMapper.countByUserId(userId);
    return new PageImpl<>(orders, PageRequest.of(page, size), total);
}
```

### 6. 로깅 활용 (개발 환경)

```yaml
# application-dev.yml
logging:
  level:
    # SQL + 파라미터 + 결과 모두 출력
    com.mudosa.musinsa.domain.order.mapper: trace
```

**출력 예시:**
```
==>  Preparing: SELECT * FROM `Order` WHERE order_id = ?
==> Parameters: 123(Long)
<==      Total: 1
<==    Columns: order_id, user_id, total_price, ...
<==        Row: 123, 456, 50000.00, ...
```

---

## 🔍 트러블슈팅

### 1. Mapper를 찾을 수 없음
```
org.apache.ibatis.binding.BindingException: Invalid bound statement (not found)
```

**해결책:**
- Mapper XML의 namespace가 Interface 패키지명과 일치하는지 확인
- `mapper-locations` 설정 확인
- XML 파일이 `resources/mappers/` 하위에 있는지 확인

### 2. 파라미터 매핑 오류
```
BindingException: Parameter '...' not found
```

**해결책:**
- `@Param` 어노테이션 추가
- XML에서 `#{paramName}` 형태로 정확히 사용

### 3. ResultMap 타입 오류
```
TypeException: Could not set property '...' 
```

**해결책:**
- Entity 필드명과 `property` 속성 일치 확인
- DB 컬럼명과 `column` 속성 일치 확인
- `map-underscore-to-camel-case=true` 설정 확인

---

## 📚 참고 자료

- [MyBatis 공식 문서](https://mybatis.org/mybatis-3/)
- [Spring Boot MyBatis 스타터](https://mybatis.org/spring-boot-starter/)
- [MyBatis Dynamic SQL](https://mybatis.org/mybatis-dynamic-sql/)

---

## 🤝 기여 및 문의

MyBatis 관련 이슈나 개선 사항은 팀 슬랙 채널에 공유해주세요!
