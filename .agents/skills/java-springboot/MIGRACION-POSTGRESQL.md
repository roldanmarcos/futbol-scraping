# Migración H2 → PostgreSQL (tests)

Paso a paso para reemplazar H2 en memoria por Testcontainers con PostgreSQL real.  
**Requisito:** Docker corriendo. No se modifica ningún archivo de producción.

---

## Paso 1 — `pom.xml`

Sacar H2 y agregar las tres dependencias de Testcontainers:

```xml
<!-- SACAR -->
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <scope>test</scope>
</dependency>

<!-- AGREGAR -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-testcontainers</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>junit-jupiter</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
```

---

## Paso 2 — `src/test/resources/application-test.yml`

Reemplazar el bloque `datasource` de H2 por el de PostgreSQL:

```yaml
# SACAR
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;NON_KEYWORDS=VALUE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect

# AGREGAR
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/futbol_scraping
    driver-class-name: org.postgresql.Driver
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

El bloque `app:` (data-initializer, scheduling) no cambia.

---

## Paso 3 — Crear `TestcontainersConfig.java`

```
src/test/java/com/futbol/scraping/config/TestcontainersConfig.java
```

```java
@TestConfiguration
public class TestcontainersConfig {

  @Bean
  @ServiceConnection
  PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>("postgres:16-alpine");
  }
}
```

`@ServiceConnection` conecta el datasource al contenedor automáticamente, sin variables de entorno adicionales.

---

## Paso 4 — Actualizar `@FutbolJpaIT`

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers                          // +
@Import(TestcontainersConfig.class)      // +
public @interface FutbolJpaIT {}
```

---

## Paso 5 — Actualizar `@FutbolIT`

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers                          // +
@Import(TestcontainersConfig.class)      // +
public @interface FutbolIT {}
```

---

## Paso 6 — Verificar

El primer run descarga `postgres:16-alpine`; los siguientes la reutilizan desde caché de Docker.

```bash
./mvnw test -P unit
./mvnw test -P integration
./mvnw test
```

---

## Impacto por anotación

| Anotación | Cambio | Motivo |
|---|---|---|
| `@FutbolJpaIT` | `@Testcontainers` + `@Import` | Usa `@DataJpaTest` → necesita el contenedor |
| `@FutbolIT` | `@Testcontainers` + `@Import` | Contexto completo → necesita el contenedor |
| `@FutbolUnit` | Ninguno | No toca base de datos |
| `@FutbolWebMvcIT` | Ninguno | Slice web, DB mockeada |
| `@FutbolSecurityIT` | Ninguno | Slice security, DB mockeada |
