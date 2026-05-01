<task>
# Module 00 – Infrastructure Setup
Đây là bước foundation bắt buộc. Chỉ tập trung vào cấu hình nền tảng, KHÔNG implement nghiệp vụ auth/booking ở phase này.
Sau khi hoàn thành, toàn bộ hệ thống sẽ có sẵn: BaseEntity, Error Handling, Security skeleton, Redis, OpenAPI, Correlation ID, Health Checks.

## Yêu Cầu Kỹ Thuật
### 1. BaseEntity & JPA Auditing
- Tạo `com.tomzxy.busozy.common.BaseEntity`:
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    
    @CreatedDate @Column(updatable = false) private OffsetDateTime createdAt;
    @LastModifiedDate private OffsetDateTime updatedAt;
    @Column(name = "deleted_at") private OffsetDateTime deletedAt;
    
    public boolean isDeleted() { return deletedAt != null; }
}

### 2. ApiResponse Wrapper
Tạo `com.tomzxy.busozy.common.ApiResponse<T>` (Java record):
```java
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    String error_code,
    int code,
    OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data) { 
        return new ApiResponse<>(true, data, "Thành công", null, 200, OffsetDateTime.now()); 
    }
    public static <T> ApiResponse<T> error(String error_code, String message, int status) {
        return new ApiResponse<>(false, null, message, error_code, status, OffsetDateTime.now());
    }
}
```

### 3. GlobalExceptionHandler
- @RestControllerAdvice tại com.tomzxy.busozy.exception.GlobalExceptionHandler
- Catch & map:
 -ResourceNotFoundException → 404
 - BusinessException → 400
 - ConflictException → 409
 - MethodArgumentNotValidException → 400 (trích xuất fieldErrors → list field + message)
 - AccessDeniedException → 403
 - AuthenticationException → 401
 - Exception (fallback) → 500
- Tất cả trả về ApiResponse<T> với error_code tương ứng

### 4. Correlation ID & Structured Logging
- Filter com.tomzxy.busozy.config.CorrelationIdFilter:
  - Đọc header X-Correlation-Id hoặc sinh UUID mới
  - Put vào MDC.put("correlationId", id)
  - Thêm response header X-Correlation-Id
- logback-spring.xml: JSON format, include %X{correlationId}

### 5. Redis Configuration
`com.tomzxy.busozy.config.RedisConfig`:
- @Bean RedisTemplate<String, Object> dùng Jackson2JsonRedisSerializer
- Key prefix: busozy:{env}: (lấy từ @Value("${app.env:dev}"))
- Enable key expiry events nếu cần: spring.data.redis.timeout=2000ms

### 6. OpenAPI / Swagger Config
- Dependency: springdoc-openapi-starter-webmvc-ui (v2.6.0+ compatible với SB 3.3)
- com.tomzxy.busozy.config.OpenApiConfig: @OpenAPIDefinition với info, servers, security schemes (BearerAuth)
- Swagger UI: /swagger-ui/index.html

### 7. Spring Security Skeleton
- com.tomzxy.busozy.config.SecurityConfig:
  .authorizeHttpRequests(auth -> 
  auth.requestMatchers("/api/v1/auth/**", "/swagger-ui/**", 
  "/v3/api-docs/**", 
  "/actuator/health/**").permitAll().anyRequest().authenticated())
  .httpBasic(Customizer.withDefaults()) + .csrf(CsrfConfigurer::disable)
  - CORS config cho @CrossOrigin
- com.tomzxy.busozy.security.JwtAuthFilter extends OncePerRequestFilter
- com.tomzxy.busozy.security.JwtService: generateToken(), extractUsername(), isTokenValid(), extractExpiration()
- @Bean PasswordEncoder → BCryptPasswordEncoder
- @Bean AuthenticationManager
### 8.  Actuator & Health
- Dependency: spring-boot-starter-actuator, micrometer-registry-prometheus
- application.yml expose: /actuator/health, /actuator/prometheus, /actuator/info
- Custom HealthIndicator cho DB & Redis (nếu cần)

### 9. pom.xml
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
</parent>

<dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-redis</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
    
    <dependency><groupId>io.hypersistence</groupId><artifactId>hypersistence-utils-hibernate-63</artifactId><version>3.9.0</version></dependency>
    <dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId><version>2.6.0</version></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>0.12.6</version></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>0.12.6</version><scope>runtime</scope></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>0.12.6</version><scope>runtime</scope></dependency>
    <dependency><groupId>org.mapstruct</groupId><artifactId>mapstruct</artifactId><version>1.6.3</version></dependency>
    <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
    <dependency><groupId>io.micrometer</groupId><artifactId>micrometer-registry-prometheus</artifactId></dependency>
</dependencies>

<build>
    <plugins>
        <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><configuration><annotationProcessorPaths><path><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><version>1.18.34</version></path><path><groupId>org.mapstruct</groupId><artifactId>mapstruct-processor</artifactId><version>1.6.3</version></path></annotationProcessorPaths></configuration></plugin>
    </plugins>
</build>

```

### 10. Application Properties Template (application.yml)
```yml
app:
  env: dev
  jwt:
    secret: ${JWT_SECRET:change-me-in-prod}
    access-ttl: 900000  # 15m
    refresh-ttl: 604800000 # 7d

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:busozy}
    username: ${DB_USER:postgres}
    password: ${DB_PASS:postgres}
  jpa:
    open-in-view: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          time_zone: UTC
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 2000ms
  flyway:
    enabled: true
    locations: classpath:db/migration

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: always

```
## Cấu Trúc File Cần Tạo
```structure
src/main/java/com/tomzxy/busozy/
├── common/
│   ├── BaseEntity.java
│   ├── ApiResponse.java
│   └── enums/ErrorCode.java
├── config/
│   ├── JpaConfig.java
│   ├── RedisConfig.java
│   ├── SecurityConfig.java
│   ├── OpenApiConfig.java
│   └── CorrelationIdFilter.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── BusinessException.java
│   ├── ConflictException.java
│   └── UnauthorizedException.java
└── security/
    ├── JwtAuthFilter.java
    └── JwtService.java
```

## Additional Requirements
1. `UserDetailsServiceImpl` implements `UserDetailsService`
2. `AuthController` dùng `@RestController`, `@RequestMapping("/api/v1/auth")`
3. `UserMapper` dùng MapStruct với `@Mapper(componentModel = "spring")`
4. Update `lastLoginAt` và `lastLoginIp` khi login thành công
5. `SecurityConfig` cần expose `PasswordEncoder` và `AuthenticationManager` beans

## Standards
- Tuân thủ tuyệt đối quy tắc trong `global_standards.md`.
</task>
