package co.onmind.hex4w.infrastructure.handlers;

import co.onmind.hex4w.application.dto.in.CreateRoleRequestDto;
import co.onmind.hex4w.application.dto.out.RoleResponseDto;
import co.onmind.hex4w.application.ports.in.CreateRoleTrait;
import co.onmind.hex4w.application.ports.in.GetRoleTrait;
import co.onmind.hex4w.domain.exceptions.DuplicateRoleException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * Unit tests for RoleHandler.
 * 
 * This test class verifies the behavior of the RoleHandler in isolation,
 * using mocked dependencies to test the handler logic without involving
 * the actual use case implementations or persistence layer.
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class RoleHandlerTest {
    
    @Mock
    private CreateRoleTrait createRoleTrait;
    
    @Mock
    private GetRoleTrait getRoleTrait;
    
    private RoleHandler roleHandler;
    private WebTestClient webTestClient;
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        // Initialize validator properly
        LocalValidatorFactoryBean validatorFactory = new LocalValidatorFactoryBean();
        validatorFactory.afterPropertiesSet();
        validator = validatorFactory;
        
        roleHandler = new RoleHandler(createRoleTrait, getRoleTrait, validator);
        
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
                roleHandler::getRoleById);
        
        webTestClient = WebTestClient.bindToRouterFunction(routes).build();
    }
    
    @Test
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
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(RoleResponseDto.class)
            .isEqualTo(response);
    }
    
    @Test
    void shouldReturnConflictWhenRoleAlreadyExists() {
        // Given
        CreateRoleRequestDto request = new CreateRoleRequestDto("ADMIN");
        
        when(createRoleTrait.createRole(any(CreateRoleRequestDto.class)))
            .thenReturn(Mono.error(DuplicateRoleException.forName("ADMIN")));
        
        // When & Then
        webTestClient.post()
            .uri("/api/v1/roles")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("DUPLICATE_ROLE")
            .jsonPath("$.message").isEqualTo("Role with name 'ADMIN' already exists");
    }
    
    @Test
    void shouldReturnBadRequestForInvalidRoleData() {
        // Given
        CreateRoleRequestDto request = new CreateRoleRequestDto(""); // Invalid empty name
        
        // When & Then
        webTestClient.post()
            .uri("/api/v1/roles")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("INVALID_REQUEST");
    }
    
    @Test
    void shouldGetAllRolesSuccessfully() {
        // Given
        RoleResponseDto role1 = new RoleResponseDto(1L, "ADMIN", LocalDateTime.now());
        RoleResponseDto role2 = new RoleResponseDto(2L, "USER", LocalDateTime.now());
        
        when(getRoleTrait.getAllRoles())
            .thenReturn(Flux.just(role1, role2));
        
        // When & Then
        webTestClient.get()
            .uri("/api/v1/roles")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBodyList(RoleResponseDto.class)
            .hasSize(2)
            .contains(role1, role2);
    }
    
    @Test
    void shouldGetRoleByIdSuccessfully() {
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
    }
    
    @Test
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
    }
    
    @Test
    void shouldReturnBadRequestForInvalidRoleId() {
        // When & Then
        webTestClient.get()
            .uri("/api/v1/roles/{id}", "invalid-id")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("INVALID_REQUEST");
    }
    
    @Test
    void shouldSearchRolesByNameSuccessfully() {
        // Given
        String searchPattern = "ADM";
        RoleResponseDto response = new RoleResponseDto(1L, "ADMIN", LocalDateTime.now());
        
        when(getRoleTrait.getRolesByNamePattern(searchPattern))
            .thenReturn(Flux.just(response));
        
        // When & Then
        webTestClient.get()
            .uri("/api/v1/roles/search?name={pattern}", searchPattern)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBodyList(RoleResponseDto.class)
            .hasSize(1)
            .contains(response);
    }
    
    @Test
    void shouldReturnBadRequestWhenSearchPatternIsMissing() {
        // When & Then
        webTestClient.get()
            .uri("/api/v1/roles/search")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("INVALID_REQUEST");
    }
}