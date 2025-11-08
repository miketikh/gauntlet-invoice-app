# Authentication Implementation Technical Notes

## Version Compatibility Reference

This document captures the correct syntax and configuration for the specific versions we're using in the InvoiceMe project.

### Library Versions
- **Spring Boot**: 3.2.0
- **Spring Security**: 6.x (included with Spring Boot 3.2)
- **JJWT**: 0.12.3
- **Lombok**: 1.18.36 (updated for Java 21+ compatibility)
- **Java**: 17 (compile target), but runs on newer JVMs

## JJWT 0.12.3 Correct Syntax

### Building JWT Tokens
```java
// Correct syntax for JJWT 0.12.3
String token = Jwts.builder()
    .subject(username)
    .issuedAt(new Date())
    .expiration(expiryDate)  // Note: not setExpiration
    .signWith(getSigningKey())  // Note: algorithm auto-detected from key
    .compact();
```

### Parsing JWT Tokens
```java
// Correct parser syntax for JJWT 0.12.3
Claims claims = Jwts.parser()
    .verifyWith(secretKey)  // Note: verifyWith, not setSigningKey
    .build()
    .parseSignedClaims(token)  // Note: parseSignedClaims, not parseClaimsJws
    .getPayload();  // Note: getPayload(), not getBody()
```

### Getting Signing Key
```java
private SecretKey getSigningKey() {
    byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
    return Keys.hmacShaKeyFor(keyBytes);
}
```

## Spring Security 6 Configuration (Spring Boot 3.2)

### SecurityFilterChain with Lambda Syntax
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .anyRequest().authenticated())
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

### Key Changes from Earlier Versions
- Use lambda expressions instead of method chaining with `.and()`
- Use `requestMatchers()` instead of deprecated `antMatchers()` or `mvcMatchers()`
- Return `http.build()` directly
- Each configuration method takes a `Customizer` lambda

## Lombok Configuration for Java 21+

### Maven Configuration
```xml
<!-- Properties -->
<properties>
    <lombok.version>1.18.36</lombok.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>

<!-- Dependency -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>${lombok.version}</version>
    <optional>true</optional>
</dependency>

<!-- Compiler Plugin Configuration -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <source>17</source>
        <target>17</target>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

### Important Notes
- Lombok requires explicit annotation processor configuration for Java 21+
- The `annotationProcessorPaths` configuration is mandatory
- Use Lombok version 1.18.36 or later for Java 21 compatibility

## Common Pitfalls and Solutions

### 1. JJWT API Changes
- **Old**: `Jwts.parserBuilder().setSigningKey().build().parseClaimsJws()`
- **New**: `Jwts.parser().verifyWith().build().parseSignedClaims()`

### 2. Spring Security Deprecations
- **Old**: `.cors().and().csrf().disable()`
- **New**: `.cors(cors -> cors...).csrf(csrf -> csrf.disable())`

### 3. Lombok Not Generating Code
- **Solution**: Ensure `annotationProcessorPaths` is configured in maven-compiler-plugin
- **IDE**: Enable annotation processing in IDE settings

## References
- [JJWT GitHub](https://github.com/jwtk/jjwt)
- [Spring Security 6 Migration Guide](https://docs.spring.io/spring-security/reference/migration/index.html)
- [Lombok Maven Setup](https://projectlombok.org/setup/maven)