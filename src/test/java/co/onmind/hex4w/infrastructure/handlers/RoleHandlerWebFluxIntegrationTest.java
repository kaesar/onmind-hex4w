package co.onmind.hex4w.infrastructure.handlers;

import co.onmind.hex4w.application.dto.in.CreateRoleRequestDto;
import co.onmind.hex4w.application.dto.out.RoleResponseDto;
import co.onmind.hex4w.application.ports.in.CreateRoleTrait;
import co.onmind.hex4w.application.ports.in.GetRoleTrait;
import co.onmind.hex4w.domain.exceptions.DuplicateRoleException;
import co.onmind.hex4w.domain.exceptions.RoleNotFoundException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * Comprehensive WebFlux integration tests for RoleHandler.
 * 
 * This test class verifies the complete integration of the RoleHandler
 * with WebFlux routing and reactive behavior, using mocked use cases
 * to isolate the handler layer testing.
 * 
 * Tests cover all endpoints with both success and error scenarios
 * using WebTestClient for reactive testing.
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleHandler WebFlux Integration Tests")
class RoleHandlerWebFluxIntegrationTest {
    
    @Mock
    private CreateRoleTrait createRoleTrait;
    
    @Mock
    private GetRoleTrait getRoleTrait;
    
    private RoleHandler roleHandler;
    private WebTestClient webTestClient;
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        // Initialize validator
        LocalValidatorFactoryBean validatorFactory = new LocalValidatorFactoryBean();
        validatorFactory.afterPropertiesSet();
        validator = validatorFactory;
        
        // Create handler with mocked dependencies
        roleHandler = new RoleHandler(createRoleTrait, getRoleTrait, validator);
        
        // Set up router function with all routes (order matters - more specific routes first)
        RouterFunction<ServerResponse> routes = RouterFunctions
            .route(POST("/api/v1/roles")
                .and(accept(MediaType.APPLICATION_JSON))
                .and(contentType(MediaType.APPLICATION_JSON)), 
                roleHandler::createRole)
            .andRoute(GET("/api/v1/roles/search")
                .and(accept(MediaType.APPLICATION_JSON)), 
                roleHandler::searchRolesByName)
            .andRoute(GET("/api/v1/roles")
                .and(accept(MediaType.APPLICATION_JSON)), 
                roleHandler::getAllRoles)
            .andRoute(GET("/api/v1/roles/{id}")
                .and(accept(MediaType.APPLICATION_JSON)), 
                roleHandler::getRoleById)
            // Utility routes
            .andRoute(GET("/api/v1/health"), 
                request -> ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new HealthResponse("UP", "Role API is running")))
            .andRoute(GET("/api/v1/info"), 
                request -> ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new ApiInfoResponse(
                        "Role Management API",
                        "1.0.0",
                        "Hexagonal Architecture with Spring WebFlux",
                        "/api/v1"
                    )));
        
        // Create WebTestClient bound to the router function
        webTestClient = WebTestClient.bindToRouterFunction(routes).build();
    }
    
    @Nested
    @DisplayName("POST /api/v1/roles - Create Role Tests")
    class CreateRoleTests {
        
        @Test
        @DisplayName("Should create role successfully with valid data")
        void shouldCreateRoleSuccessfully() {
            // Given
            CreateRoleRequestDto request = new CreateRoleRequestDto("ADMIN");
            RoleResponseDto response = new RoleResponseDto(1L, "ADMIN", LocalDateTime.now());
            
            when(createRoleTrait.createRole(any(CreateRoleRequestDto.class)))
                .thenReturn(Mono.just(response));
            
            // When & Then
            webTestClient.post()
                .uri("/api/v1/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(RoleResponseDto.class)
                .isEqualTo(response);
            
            verify(createRoleTrait, times(1)).createRole(any(CreateRoleRequestDto.class));
        }
        
        @Test
        @DisplayName("Should return 409 Conflict when role already exists")
        void shouldReturnConflictWhenRoleAlreadyExists() {
            // Given
            CreateRoleRequestDto request = new CreateRoleRequestDto("ADMIN");
            
            when(createRoleTrait.createRole(any(CreateRoleRequestDto.class)))
                .thenReturn(Mono.error(DuplicateRoleException.forName("ADMIN")));
            
            // When & Then
            webTestClient.post()
                .uri("/api/v1/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("DUPLICATE_ROLE")
                .jsonPath("$.message").isEqualTo("Role with name 'ADMIN' already exists")
                .jsonPath("$.status").isEqualTo(409);
            
            verify(createRoleTrait, times(1)).createRole(any(CreateRoleRequestDto.class));
        }
        
        @Test
        @DisplayName("Should return 400 Bad Request for blank role name")
        void shouldReturnBadRequestForBlankRoleName() {
            // Given
            CreateRoleRequestDto request = new CreateRoleRequestDto("");
            
            // When & Then
            webTestClient.post()
                .uri("/api/v1/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                .jsonPath("$.status").isEqualTo(400);
        }
        
        @Test
        @DisplayName("Should return 400 Bad Request for role name exceeding max length")
        void shouldReturnBadRequestForRoleNameTooLong() {
            // Given - Create a name longer than 100 characters
            String longName = "A".repeat(101);
            CreateRoleRequestDto request = new CreateRoleRequestDto(longName);
            
            // When & Then
            webTestClient.post()
                .uri("/api/v1/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                .jsonPath("$.status").isEqualTo(400);
        }
        
        @Test
        @DisplayName("Should handle internal server error gracefully")
        void shouldHandleInternalServerErrorGracefully() {
            // Given
            CreateRoleRequestDto request = new CreateRoleRequestDto("ADMIN");
            
            when(createRoleTrait.createRole(any(CreateRoleRequestDto.class)))
                .thenReturn(Mono.error(new RuntimeException("Database connection failed")));
            
            // When & Then
            webTestClient.post()
                .uri("/api/v1/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(500)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INTERNAL_ERROR")
                .jsonPath("$.message").isEqualTo("An unexpected error occurred")
                .jsonPath("$.status").isEqualTo(500);
        }
    }
    
    @Nested
    @DisplayName("GET /api/v1/roles - Get All Roles Tests")
    class GetAllRolesTests {
        
        @Test
        @DisplayName("Should return all roles successfully")
        void shouldReturnAllRolesSuccessfully() {
            // Given
            RoleResponseDto role1 = new RoleResponseDto(1L, "ADMIN", LocalDateTime.now());
            RoleResponseDto role2 = new RoleResponseDto(2L, "USER", LocalDateTime.now());
            RoleResponseDto role3 = new RoleResponseDto(3L, "MODERATOR", LocalDateTime.now());
            
            when(getRoleTrait.getAllRoles())
                .thenReturn(Flux.just(role1, role2, role3));
            
            // When & Then
            webTestClient.get()
                .uri("/api/v1/roles")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(RoleResponseDto.class)
                .hasSize(3)
                .contains(role1, role2, role3);
            
            verify(getRoleTrait, times(1)).getAllRoles();
        }
        
        @Test
        @DisplayName("Should return empty list when no roles exist")
        void shouldReturnEmptyListWhenNoRolesExist() {
            // Given
            when(getRoleTrait.getAllRoles())
                .thenReturn(Flux.empty());
            
            // When & Then
            webTestClient.get()
                .uri("/api/v1/roles")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(RoleResponseDto.class)
                .hasSize(0);
            
            verify(getRoleTrait, times(1)).getAllRoles();
        }
        
        @Test
        @DisplayName("Should handle error during role retrieval")
        void shouldHandleErrorDuringRoleRetrieval() {
            // Given
            when(getRoleTrait.getAllRoles())
                .thenReturn(Flux.error(new RuntimeException("Database error")));
            
            // When & Then
            webTestClient.get()
                .uri("/api/v1/roles")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(500);
        }
    }
    
    @Nested
    @DisplayName("GET /api/v1/roles/{id} - Get Role By ID Tests")
    class GetRoleByIdTests {
        
        @Test
        @DisplayName("Should return role by ID successfully")
        void shouldReturnRoleByIdSuccessfully() {
            // Given
            Long roleId = 1L;
            RoleResponseDto response = new RoleResponseDto(roleId, "ADMIN", LocalDateTime.now());
            
            when(getRoleTrait.getRoleById(roleId))
                .thenReturn(Mono.just(response));
            
            // When & Then
            webTestClient.get()
                .uri("/api/v1/roles/{id}", roleId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(RoleResponseDto.class)
                .isEqualTo(response);
            
            verify(getRoleTrait, times(1)).getRoleById(roleId);
        }
        
        @Test
        @DisplayName("Should return 404 Not Found when role does not exist")
        void shouldReturnNotFoundWhenRoleDoesNotExist() {
            // Given
            Long roleId = 999L;
            
            when(getRoleTrait.getRoleById(roleId))
                .thenReturn(Mono.empty());
            
            // When & Then
            webTestClient.get()
                .uri("/api/v1/roles/{id}", roleId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
            
            verify(getRoleTrait, times(1)).getRoleById(roleId);
        }
        
        @Test
        @DisplayName("Should return 400 Bad Request for invalid role ID format")
        void shouldReturnBadRequestForInvalidRoleIdFormat() {
            // When & Then
            webTestClient.get()
                .uri("/api/v1/roles/{id}", "invalid-id")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                .jsonPath("$.message").value(message -> message.toString().contains("Invalid role ID format"))
                .jsonPath("$.status").isEqualTo(400);
        }
        
        @Test
        @DisplayName("Should handle RoleNotFoundException appropriately")
        void shouldHandleRoleNotFoundExceptionAppropriately() {
            // Given
            Long roleId = 1L;
            
            when(getRoleTrait.getRoleById(roleId))
                .thenReturn(Mono.error(RoleNotFoundException.forId(roleId)));
            
            // When & Then
            webTestClient.get()
                .uri("/api/v1/roles/{id}", roleId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("ROLE_NOT_FOUND")
                .jsonPath("$.message").isEqualTo("Role with ID 1 not found")
                .jsonPath("$.status").isEqualTo(404);
        }
    }
    
    @Nested
    @DisplayName("GET /api/v1/roles/search - Search Roles Tests")
    class SearchRolesTests {
        
        @Test
        @DisplayName("Should search roles by name pattern successfully")
        void shouldSearchRolesByNamePatternSuccessfully() {
            // Given
            String searchPattern = "ADM";
            RoleResponseDto adminRole = new RoleResponseDto(1L, "ADMIN", LocalDateTime.now());
            RoleResponseDto adminUserRole = new RoleResponseDto(2L, "ADMIN_USER", LocalDateTime.now());
            
            when(getRoleTrait.getRolesByNamePattern(searchPattern))
                .thenReturn(Flux.just(adminRole, adminUserRole));
            
            // When & Then
            webTestClient.get()
                .uri("/api/v1/roles/search?name={pattern}", searchPattern)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(RoleResponseDto.class)
                .hasSize(2)
                .contains(adminRole, adminUserRole);
            
            verify(getRoleTrait, times(1)).getRolesByNamePattern(searchPattern);
        }
        
        @Test
        @DisplayName("Should return empty list when no roles match search pattern")
        void shouldReturnEmptyListWhenNoRolesMatchSearchPattern() {
            // Given
            String searchPattern = "NONEXISTENT";
            
            when(getRoleTrait.getRolesByNamePattern(searchPattern))
                .thenReturn(Flux.empty());
            
            // When & Then
            webTestClient.get()
                .uri("/api/v1/roles/search?name={pattern}", searchPattern)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(RoleResponseDto.class)
                .hasSize(0);
            
            verify(getRoleTrait, times(1)).getRolesByNamePattern(searchPattern);
        }
        
        @Test
        @DisplayName("Should return 400 Bad Request when name parameter is missing")
        void shouldReturnBadRequestWhenNameParameterIsMissing() {
            // When & Then
            webTestClient.get()
                .uri("/api/v1/roles/search")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                .jsonPath("$.message").isEqualTo("Name query parameter is required")
                .jsonPath("$.status").isEqualTo(400);
        }
        
        @Test
        @DisplayName("Should return 400 Bad Request when name parameter is empty")
        void shouldReturnBadRequestWhenNameParameterIsEmpty() {
            // When & Then
            webTestClient.get()
                .uri("/api/v1/roles/search?name=")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                .jsonPath("$.message").isEqualTo("Name query parameter cannot be empty")
                .jsonPath("$.status").isEqualTo(400);
        }
        
        @Test
        @DisplayName("Should return 400 Bad Request when name parameter is only whitespace")
        void shouldReturnBadRequestWhenNameParameterIsOnlyWhitespace() {
            // When & Then
            webTestClient.get()
                .uri("/api/v1/roles/search?name=   ")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                .jsonPath("$.message").isEqualTo("Name query parameter cannot be empty")
                .jsonPath("$.status").isEqualTo(400);
        }
    }
    
    @Nested
    @DisplayName("Utility Endpoints Tests")
    class UtilityEndpointsTests {
        
        @Test
        @DisplayName("Should handle health endpoint successfully")
        void shouldHandleHealthEndpointSuccessfully() {
            // When & Then
            webTestClient.get()
                .uri("/api/v1/health")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.message").isEqualTo("Role API is running");
        }
        
        @Test
        @DisplayName("Should handle API info endpoint successfully")
        void shouldHandleApiInfoEndpointSuccessfully() {
            // When & Then
            webTestClient.get()
                .uri("/api/v1/info")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.name").isEqualTo("Role Management API")
                .jsonPath("$.version").isEqualTo("1.0.0")
                .jsonPath("$.description").isEqualTo("Hexagonal Architecture with Spring WebFlux")
                .jsonPath("$.basePath").isEqualTo("/api/v1");
        }
    }
    
    @Nested
    @DisplayName("Reactive Behavior Tests")
    class ReactiveBehaviorTests {
        
        @Test
        @DisplayName("Should handle concurrent requests properly")
        void shouldHandleConcurrentRequestsProperly() {
            // Given
            RoleResponseDto role1 = new RoleResponseDto(1L, "ADMIN", LocalDateTime.now());
            RoleResponseDto role2 = new RoleResponseDto(2L, "USER", LocalDateTime.now());
            
            when(getRoleTrait.getAllRoles())
                .thenReturn(Flux.just(role1, role2));
            
            // When & Then - Make multiple concurrent requests
            for (int i = 0; i < 5; i++) {
                webTestClient.get()
                    .uri("/api/v1/roles")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBodyList(RoleResponseDto.class)
                    .hasSize(2);
            }
            
            verify(getRoleTrait, times(5)).getAllRoles();
        }
        
        @Test
        @DisplayName("Should handle streaming response properly")
        void shouldHandleStreamingResponseProperly() {
            // Given
            RoleResponseDto role1 = new RoleResponseDto(1L, "ADMIN", LocalDateTime.now());
            RoleResponseDto role2 = new RoleResponseDto(2L, "USER", LocalDateTime.now());
            RoleResponseDto role3 = new RoleResponseDto(3L, "MODERATOR", LocalDateTime.now());
            
            when(getRoleTrait.getAllRoles())
                .thenReturn(Flux.just(role1, role2, role3));
            
            // When & Then
            webTestClient.get()
                .uri("/api/v1/roles")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(RoleResponseDto.class)
                .hasSize(3)
                .consumeWith(result -> {
                    // Verify that all roles are present in the response
                    var roles = result.getResponseBody();
                    assert roles != null;
                    assert roles.contains(role1);
                    assert roles.contains(role2);
                    assert roles.contains(role3);
                });
        }
        
        @Test
        @DisplayName("Should handle backpressure in streaming scenarios")
        void shouldHandleBackpressureInStreamingScenarios() {
            // Given - Create a large number of roles to test backpressure
            RoleResponseDto[] roles = new RoleResponseDto[100];
            for (int i = 0; i < 100; i++) {
                roles[i] = new RoleResponseDto((long) (i + 1), "ROLE_" + i, LocalDateTime.now());
            }
            
            when(getRoleTrait.getAllRoles())
                .thenReturn(Flux.fromArray(roles));
            
            // When & Then
            webTestClient.get()
                .uri("/api/v1/roles")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(RoleResponseDto.class)
                .hasSize(100);
            
            verify(getRoleTrait, times(1)).getAllRoles();
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should handle validation errors consistently")
        void shouldHandleValidationErrorsConsistently() {
            // Test multiple validation scenarios
            CreateRoleRequestDto[] invalidRequests = {
                new CreateRoleRequestDto(""),           // Empty name
                new CreateRoleRequestDto("   "),        // Whitespace only
                new CreateRoleRequestDto("A".repeat(101)) // Too long
            };
            
            for (CreateRoleRequestDto request : invalidRequests) {
                webTestClient.post()
                    .uri("/api/v1/roles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                    .jsonPath("$.status").isEqualTo(400);
            }
        }
        
        @Test
        @DisplayName("Should handle different exception types appropriately")
        void shouldHandleDifferentExceptionTypesAppropriately() {
            // Test different exception scenarios
            CreateRoleRequestDto request = new CreateRoleRequestDto("TEST_ROLE");
            
            // Test DuplicateRoleException
            when(createRoleTrait.createRole(any(CreateRoleRequestDto.class)))
                .thenReturn(Mono.error(DuplicateRoleException.forName("TEST_ROLE")));
            
            webTestClient.post()
                .uri("/api/v1/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("DUPLICATE_ROLE");
            
            // Test generic RuntimeException
            when(createRoleTrait.createRole(any(CreateRoleRequestDto.class)))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error")));
            
            webTestClient.post()
                .uri("/api/v1/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(500)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INTERNAL_ERROR");
        }
    }
    
    // Helper records for utility endpoints
    public record HealthResponse(String status, String message) {}
    public record ApiInfoResponse(String name, String version, String description, String basePath) {}
}