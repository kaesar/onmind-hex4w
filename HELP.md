# Hex4WebFlux (hex4w) - Plantilla de Arquitectura Hexagonal Reactiva

Una implementación completa de arquitectura hexagonal (patrón Ports and Adapters) utilizando Spring Boot WebFlux para programación reactiva.

## Características

- **Programación Reactiva**: Construido con Spring Boot WebFlux para operaciones no bloqueantes y reactivas
- **Arquitectura Hexagonal**: Separación clara de responsabilidades con capas de dominio, aplicación e infraestructura
- **Integración R2DBC**: Acceso reactivo a base de datos con H2 en memoria
- **Enrutamiento Funcional**: Enrutamiento basado en RouterFunction en lugar de controladores tradicionales
- **Testing Integral**: Testing reactivo con StepVerifier y WebTestClient
- **Integración MapStruct**: Mapeo automático entre DTOs y modelos de dominio
- **Manejo de Errores Reactivo**: Manejo centralizado de excepciones con GlobalErrorHandler
- **Logging Reactivo**: Aspectos de logging adaptados para programación reactiva

## Requisitos Previos

- **Java 21** o superior
- **Gradle 8.7** o superior
- **Git** para clonar el repositorio

## Arquitectura Hexagonal Reactiva

### Estructura de Paquetes

```
  __________________
./ co.onmind.hex4w /
│
├── domain/                  # Capa de Dominio (Core Business Logic)
│   ├── models/              # Entidades de dominio
│   ├── services/            # Servicios de dominio reactivos
│   └── exceptions/          # Excepciones de dominio
├── application/             # Capa de Aplicación (Use Cases)
│   ├── dto/
│   │   ├── in/              # DTOs de entrada
│   │   └── out/             # DTOs de salida
│   ├── mappers/             # Mappers reactivos entre DTOs y modelos
│   ├── usecases/            # Implementaciones de casos de uso reactivos
│   └── ports/
│       ├── in/              # Puertos de entrada reactivos (Use Cases)
│       └── out/             # Puertos de salida reactivos (Repositories)
├── infrastructure/          # Capa de Infraestructura (Adapters)
│   ├── configuration/       # Configuraciones de Spring WebFlux
│   ├── handlers/            # Handlers reactivos (en lugar de controllers)
│   ├── persistence/         # Implementaciones de persistencia R2DBC
│   │   ├── adapters/        # Adaptadores de repositorio
│   │   ├── entities/        # Entidades R2DBC
│   │   ├── mappers/         # Mappers de entidades
│   │   └── repositories/    # Repositorios R2DBC
│   └── webclients/          # Clientes web reactivos para servicios externos
└── transverse/              # Componentes Transversales
    ├── exceptions/          # Manejo global de errores reactivo (GlobalErrorHandler)
    └── logging/             # Aspectos de logging reactivos
```

### Diagrama de Arquitectura

```mermaid
graph TB
    subgraph "Infrastructure Layer (Reactive)"
        HANDLER[RoleHandler<br/>RouterFunction]
        R2DBC[R2DBC Repository<br/>ReactiveCrudRepository]
        CONFIG[WebFlux Configuration]
        WEBCLIENT[WebClient<br/>External Services]
    end
    
    subgraph "Application Layer (Reactive)"
        UC[RoleUseCaseImpl<br/>Reactive Use Cases]
        PIN[Input Ports<br/>Mono/Flux Interfaces]
        POUT[Output Ports<br/>Mono/Flux Interfaces]
        DTO[DTOs<br/>Request/Response]
        MAP[Reactive Mappers<br/>MapStruct]
    end
    
    subgraph "Domain Layer (Pure Business Logic)"
        MODEL[Role Model<br/>Domain Entity]
        SERVICE[RoleService<br/>Reactive Domain Logic]
        EXCEPTIONS[Domain Exceptions<br/>Business Rules]
    end
    
    HANDLER --> PIN
    PIN --> UC
    UC --> SERVICE
    UC --> POUT
    POUT --> R2DBC
    SERVICE --> MODEL
    SERVICE --> EXCEPTIONS
    UC --> MAP
    MAP --> DTO
    CONFIG --> HANDLER
    WEBCLIENT --> UC
    
    classDef domain fill:#e1f5fe
    classDef application fill:#f3e5f5
    classDef infrastructure fill:#e8f5e8
    
    class MODEL,SERVICE,EXCEPTIONS domain
    class UC,PIN,POUT,DTO,MAP application
    class HANDLER,R2DBC,CONFIG,WEBCLIENT infrastructure
```

## Flujo Reactivo Completo del Ejemplo Role

### 1. Flujo de Creación de Role

```mermaid
sequenceDiagram
    participant Client
    participant RoleHandler
    participant CreateRoleTrait
    participant RoleService
    participant RoleRepositoryPort
    participant R2dbcRepository
    participant Database
    
    Client->>RoleHandler: POST /api/v1/roles
    Note over RoleHandler: Mono<CreateRoleRequestDto>
    
    RoleHandler->>RoleHandler: validateRequest()
    RoleHandler->>CreateRoleTrait: createRole(request)
    Note over CreateRoleTrait: Mono<RoleResponseDto>
    
    CreateRoleTrait->>RoleService: createRole(name)
    Note over RoleService: Mono<Role>
    
    RoleService->>RoleService: validateBusinessRules()
    RoleService->>CreateRoleTrait: Mono<Role>
    
    CreateRoleTrait->>RoleRepositoryPort: save(role)
    Note over RoleRepositoryPort: Mono<Role>
    
    RoleRepositoryPort->>R2dbcRepository: save(roleEntity)
    R2dbcRepository->>Database: INSERT INTO roles
    Database-->>R2dbcRepository: RoleEntity
    R2dbcRepository-->>RoleRepositoryPort: Mono<RoleEntity>
    
    RoleRepositoryPort-->>CreateRoleTrait: Mono<Role>
    CreateRoleTrait-->>RoleHandler: Mono<RoleResponseDto>
    RoleHandler-->>Client: HTTP 201 + RoleResponseDto
```

### 2. Características Reactivas Clave

- **Mono/Flux**: Todos los métodos retornan tipos reactivos
- **Non-blocking**: Operaciones no bloqueantes en toda la aplicación
- **Backpressure**: Manejo automático de contrapresión
- **Error Handling**: Manejo reactivo de errores con `onErrorResume`
- **Composition**: Composición de operaciones reactivas con `flatMap`, `map`, etc.

### 3. Manejo de Errores (GlobalErrorHandler vs. Handlers)

La plantilla cuenta con **dos capas** de manejo de errores que conviven, y es
importante entender cuándo actúa cada una:

#### a. `GlobalErrorHandler` (centralizado)
Ubicado en `transverse/exceptions/GlobalErrorHandler.java`, implementa
`ErrorWebExceptionHandler` con `@Order(-2)`. Intercepta **cualquier excepción no
capturada** en el pipeline de WebFlux y la convierte en una respuesta JSON
estándar con 5 campos: `code`, `message`, `status`, `timestamp` y `path`.

Mapeo de excepciones a HTTP status:

| Excepción | HTTP Status | code |
|---|---|---|
| `DuplicateRoleException` | 409 CONFLICT | `DUPLICATE_ROLE` |
| `RoleNotFoundException` | 404 NOT_FOUND | `ROLE_NOT_FOUND` |
| `ScriptNotAllowedException` | 403 FORBIDDEN | `SCRIPT_NOT_ALLOWED` |
| `IllegalArgumentException` | 400 BAD_REQUEST | `INVALID_REQUEST` |
| `JsonProcessingException` | 400 BAD_REQUEST | `INVALID_JSON` |
| Cualquier otra | 500 INTERNAL_SERVER_ERROR | `INTERNAL_ERROR` |

#### b. `handleError` por Handler (frontera)
Cada handler en `infrastructure/handlers/` (p. ej. `RoleHandler`,
`ScriptingHandler`, `StoreHandler`) implementa su propio
`.onErrorResume(this::handleError)` y un `record ErrorResponse` local de **3
campos** (`code`, `message`, `status`). Esta capa captura el error **dentro del
flujo reactivo del handler**, antes de que llegue al `GlobalErrorHandler`.

#### ¿Por qué existen ambas y cuál predomina?
- En la **aplicación en ejecución**, la excepción se resuelve en el primer
  `onErrorResume` que la intercepte. Si el handler la maneja, se usa el formato
  de 3 campos; si no, la excepción sube y el `GlobalErrorHandler` la formatea
  con 5 campos.
- En los **tests aislados de handler** (`*WebFluxIntegrationTest`), el
  `WebTestClient` se construye manualmente solo con el handler y el
  `RouterFunction`, **sin** registrar el `GlobalErrorHandler` en el pipeline. Por
  lo tanto, en ese contexto el `GlobalErrorHandler` no interviene y el
  `handleError` del handler es la única fuente de mapeo de errores. Es por eso
  que los handlers mantienen su propio `handleError` y no delegan todo al
  centralizado.

> **Nota de diseño:** Si en el futuro se quiere unificar en el `GlobalErrorHandler`,
> habría que registrarlo también en los `WebTestClient` de los tests de handler
> (o migrar esos tests a `@WebFluxTest` con el contexto completo), y unificar el
> formato de `ErrorResponse` en un único record compartido.

## Inicio Rápido

### Instalación y Ejecución

1. **Clonar el repositorio**:
```bash
git clone https://github.com/kaesar/onmind-hex4w.git hex4w
cd hex4w
```

2. **Ejecutar la aplicación**:
```bash
./gradlew bootRun
```

La aplicación se iniciará en el puerto 8080.

3. **Verificar que la aplicación esté funcionando**:
```bash
curl http://localhost:8080/actuator/health
```

### Ejecutar Tests

```bash
# Ejecutar todos los tests
./gradlew test

# Ejecutar tests con reporte de cobertura
./gradlew test jacocoTestReport

# Ejecutar solo tests unitarios
./gradlew test --tests "*Test"

# Ejecutar solo tests de integración
./gradlew test --tests "*IntegrationTest"
```

## API Endpoints

### Endpoints Reactivos Disponibles

| Método | Endpoint | Descripción | Request Body | Response |
|--------|----------|-------------|--------------|----------|
| `POST` | `/api/v1/roles` | Crear un nuevo role | `CreateRoleRequestDto` | `RoleResponseDto` |
| `GET`  | `/api/v1/roles` | Obtener todos los roles | - | `Flux<RoleResponseDto>` |
| `GET`  | `/api/v1/roles/{id}` | Obtener role por ID | - | `RoleResponseDto` |
| `GET`  | `/api/v1/roles/search?name={pattern}` | Buscar roles por patrón de nombre | - | `Flux<RoleResponseDto>` |
| `POST` | `/api/v1/scripts/execute` | Ejecutar archivo `.js` whitelisteado (GraalJS) | `{ "script": "hello.js" }` | `ScriptResultResponseDto` |
| `GET`  | `/api/v1/store/items?bucket={name}` | Listar objetos de un bucket S3 | - | `Flux<StoreItemResponseDto>` |

### Ejemplos de Uso

#### Crear un Role
```bash
curl -X POST http://localhost:8080/api/v1/roles \
  -H "Content-Type: application/json" \
  -d '{"name": "ADMIN"}'
```

**Respuesta**:
```json
{
  "id": 1,
  "name": "ADMIN",
  "createdAt": "2024-01-15T10:30:00"
}
```

#### Obtener todos los Roles
```bash
curl http://localhost:8080/api/v1/roles
```

**Respuesta**:
```json
[
  {
    "id": 1,
    "name": "ADMIN",
    "createdAt": "2024-01-15T10:30:00"
  },
  {
    "id": 2,
    "name": "USER",
    "createdAt": "2024-01-15T10:31:00"
  }
]
```

#### Obtener Role por ID
```bash
curl http://localhost:8080/api/v1/roles/1
```

#### Buscar Roles por Nombre
```bash
curl "http://localhost:8080/api/v1/roles/search?name=ADM"
```

#### Ejecutar Script JavaScript (archivo whitelisteado)

Ruta: `POST /api/v1/scripts/execute`  
Body: `{ "script": "<nombre-archivo.js>" }` → `{ value, stdout, stderr }`

Solo se ejecutan archivos listados en el enum `AllowedScript` y presentes en
`src/main/resources/scripts/` (config: `app.scripts.location`).

| Archivo | Enum |
|---------|------|
| `hello.js` | `AllowedScript.HELLO` |
| `example.js` | `AllowedScript.EXAMPLE` |

```bash
curl -X POST http://localhost:8080/api/v1/scripts/execute \
  -H "Content-Type: application/json" \
  -d '{"script": "hello.js"}'
```

**Respuesta** (ejemplo):
```json
{
  "value": "Hello from hex4w scripts!",
  "stdout": "",
  "stderr": null
}
```

Flujo: nombre → whitelist (`AllowedScript`) → carga classpath → `GraalJsAdapter` (sandbox).  
Para ABCode: transpilar a `.js`, copiar a `scripts/` y registrar el nombre en el enum.  
Nombre no permitido → `403 SCRIPT_NOT_ALLOWED`.

#### Listar Objetos de un Bucket S3
```bash
curl "http://localhost:8080/api/v1/store/items?bucket=my-bucket"
```

**Respuesta**:
```json
[
  {
    "key": "documents/report.pdf",
    "size": 102400,
    "lastModified": "2024-01-15T10:30:00",
    "eTag": "\"d41d8cd98f00b204e9800998ecf8427e\""
  }
]
```

## Configuración

### Dependencias Reactivas Principales

El proyecto utiliza las siguientes dependencias clave para programación reactiva:

```gradle
dependencies {
    // Spring Boot WebFlux - Framework reactivo principal
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    
    // Spring Data R2DBC - Acceso reactivo a base de datos
    implementation 'org.springframework.boot:spring-boot-starter-data-r2dbc'
    
    // R2DBC H2 - Driver reactivo para H2
    runtimeOnly 'io.r2dbc:r2dbc-h2'
    runtimeOnly 'com.h2database:h2'
    
    // Validation - Validación de datos
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    
    // MapStruct - Mapeo de objetos
    implementation 'org.mapstruct:mapstruct:1.5.5.Final'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
    
    // Testing Reactivo
    testImplementation 'io.projectreactor:reactor-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

### Configuración de Base de Datos R2DBC

```yaml
spring:
  r2dbc:
    url: r2dbc:h2:mem:///hex4w;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password: password
```

### Configuración de WebFlux

```yaml
server:
  port: 8080

spring:
  webflux:
    base-path: /api/v1
```

## Testing Reactivo

### Estrategia de Testing

La aplicación implementa una pirámide de testing reactivo:

1. **Tests Unitarios Reactivos (70%)**
   - Servicios de dominio con `StepVerifier`
   - Casos de uso reactivos
   - Mappers y validaciones

2. **Tests de Integración Reactivos (20%)**
   - Handlers con `WebTestClient`
   - Repositorios R2DBC con `@DataR2dbcTest`

3. **Tests End-to-End Reactivos (10%)**
   - `@SpringBootTest` con `WebTestClient`
   - Flujos completos de API reactiva

### Ejemplo de Test Reactivo

```java
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {
    
    @Test
    void shouldCreateRoleReactively() {
        // Given
        String roleName = "ADMIN";
        Role expectedRole = new Role(roleName);
        
        // When
        Mono<Role> result = roleService.createRole(roleName);
        
        // Then
        StepVerifier.create(result)
            .expectNext(expectedRole)
            .verifyComplete();
    }
}
```

### Test de Handler con WebTestClient

```java
@WebFluxTest(RoleHandler.class)
class RoleHandlerTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    void shouldCreateRoleViaHandler() {
        // Given
        CreateRoleRequestDto request = new CreateRoleRequestDto("ADMIN");
        
        // When & Then
        webTestClient.post()
            .uri("/api/v1/roles")
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(RoleResponseDto.class)
            .value(response -> {
                assertThat(response.name()).isEqualTo("ADMIN");
                assertThat(response.id()).isNotNull();
            });
    }
}
```

## Desarrollo y Extensión

### Diferencias Clave con hex4j (Spring MVC)

| Aspecto | hex4j (Spring MVC) | hex4w (WebFlux) |
|---------|-------------------|-------------------------|
| **Framework** | Spring MVC | Spring WebFlux |
| **Modelo de Programación** | Imperativo/Bloqueante | Reactivo/No-bloqueante |
| **Controladores** | `@RestController` | `RouterFunction` + `Handler` |
| **Persistencia** | JPA/Hibernate | R2DBC |
| **Tipos de Retorno** | Objetos directos | `Mono<T>` / `Flux<T>` |
| **Testing** | MockMvc | WebTestClient + StepVerifier |
| **Base de Datos** | H2 con JDBC | H2 con R2DBC |

### Cómo Extender la Plantilla con Nuevas Entidades

#### 1. Crear el Modelo de Dominio

```java
// domain/models/User.java
public class User {
    private Long id;
    private String username;
    private String email;
    private LocalDateTime createdAt;
    
    // Constructor, getters, setters, business logic
}
```

#### 2. Crear el Servicio de Dominio Reactivo

```java
// domain/services/UserService.java
@Service
public class UserService {
    
    public Mono<User> createUser(String username, String email) {
        return Mono.fromCallable(() -> {
            // Validaciones de negocio
            validateUsername(username);
            validateEmail(email);
            return new User(username, email);
        });
    }
    
    public Mono<Void> validateBusinessRules(User user) {
        // Lógica de validación reactiva
        return Mono.empty();
    }
}
```

#### 3. Definir DTOs

```java
// application/dto/in/CreateUserRequestDto.java
public record CreateUserRequestDto(
    @NotBlank String username,
    @Email String email
) {}

// application/dto/out/UserResponseDto.java
public record UserResponseDto(
    Long id,
    String username,
    String email,
    LocalDateTime createdAt
) {}
```

#### 4. Crear Puertos Reactivos

```java
// application/ports/in/CreateUserTrait.java
public interface CreateUserTrait {
    Mono<UserResponseDto> createUser(CreateUserRequestDto request);
}

// application/ports/out/UserRepositoryPort.java
public interface UserRepositoryPort {
    Mono<User> save(User user);
    Mono<User> findById(Long id);
    Flux<User> findAll();
    Mono<Boolean> existsByUsername(String username);
}
```

#### 5. Caso de Uso Reactivo por Implementar

```java
// application/usecases/UserUseCase.java
@Component
public class UserUseCase implements CreateUserTrait, GetUserTrait {
    
    private final UserService userService;
    private final UserRepositoryPort userRepository;
    private final UserMapper userMapper;
    
    @Override
    public Mono<UserResponseDto> createUser(CreateUserRequestDto request) {
        return Mono.just(request)
            .map(userMapper::toEntity)
            .flatMap(user -> userService.createUser(user.getUsername(), user.getEmail()))
            .flatMap(userService::validateBusinessRules)
            .flatMap(userRepository::save)
            .map(userMapper::toResponseDto);
    }
}
```

#### 6. Crear Entidad R2DBC y Repositorio

```java
// infrastructure/persistence/entities/UserEntity.java
@Table("users")
public class UserEntity {
    @Id
    private Long id;
    
    @Column("username")
    private String username;
    
    @Column("email")
    private String email;
    
    @Column("created_at")
    private LocalDateTime createdAt;
}

// infrastructure/persistence/repositories/R2dbcUserRepository.java
public interface R2dbcUserRepository extends ReactiveCrudRepository<UserEntity, Long> {
    Mono<Boolean> existsByUsername(String username);
    Mono<UserEntity> findByEmail(String email);
}
```

#### 7. Implementar Adaptador de Persistencia

```java
// infrastructure/persistence/adapters/UserRepositoryAdapter.java
@Repository
public class UserRepositoryAdapter implements UserRepositoryPort {
    
    private final R2dbcUserRepository r2dbcRepository;
    private final UserEntityMapper entityMapper;
    
    @Override
    public Mono<User> save(User user) {
        return Mono.just(user)
            .map(entityMapper::toEntity)
            .flatMap(r2dbcRepository::save)
            .map(entityMapper::toDomain);
    }
}
```

#### 8. Crear Handler Reactivo

```java
// infrastructure/handlers/UserHandler.java
@Component
public class UserHandler {
    
    private final CreateUserTrait CreateUserTrait;
    private final GetUserTrait GetUserTrait;
    
    public Mono<ServerResponse> createUser(ServerRequest request) {
        return request.bodyToMono(CreateUserRequestDto.class)
            .flatMap(CreateUserTrait::createUser)
            .flatMap(user -> ServerResponse.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user))
            .onErrorResume(this::handleError);
    }
}
```

#### 9. Configurar Rutas

```java
// infrastructure/configuration/UserRouterConfiguration.java
@Configuration
public class UserRouterConfiguration {
    
    @Bean
    public RouterFunction<ServerResponse> userRoutes(UserHandler userHandler) {
        return RouterFunctions
            .route(POST("/api/v1/users"), userHandler::createUser)
            .andRoute(GET("/api/v1/users"), userHandler::getAllUsers)
            .andRoute(GET("/api/v1/users/{id}"), userHandler::getUserById);
    }
}
```

### Mejores Prácticas para Desarrollo Reactivo

1. **Evitar Bloqueos**: Nunca usar `.block()` en código de producción
2. **Composición**: Usar `flatMap`, `map`, `filter` para componer operaciones
3. **Manejo de Errores**: Usar `onErrorResume`, `onErrorReturn` para manejo reactivo de errores
4. **Testing**: Siempre usar `StepVerifier` para testing de streams reactivos
5. **Backpressure**: Considerar estrategias de backpressure para streams grandes
6. **Schedulers**: Usar schedulers apropiados para operaciones CPU-intensivas

## Monitoreo y Observabilidad

### Endpoints de Actuator

La aplicación incluye endpoints de monitoreo:

- `/actuator/health` - Estado de salud de la aplicación
- `/actuator/info` - Información de la aplicación
- `/actuator/metrics` - Métricas de la aplicación

### Logging Reactivo

El proyecto incluye logging reactivo configurado:

```yaml
logging:
  level:
    co.onmind.hex4w: DEBUG
    org.springframework.r2dbc: DEBUG
    reactor.netty: INFO
```
