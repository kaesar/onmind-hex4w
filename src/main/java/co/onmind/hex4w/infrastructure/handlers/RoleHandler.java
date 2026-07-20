package co.onmind.hex4w.infrastructure.handlers;

import co.onmind.hex4w.application.dto.in.CreateRoleRequestDto;
import co.onmind.hex4w.application.dto.out.RoleResponseDto;
import co.onmind.hex4w.application.ports.in.CreateRoleTrait;
import co.onmind.hex4w.application.ports.in.GetRoleTrait;
import co.onmind.hex4w.domain.exceptions.DuplicateRoleException;
import co.onmind.hex4w.domain.exceptions.RoleNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reactive handler for role-related HTTP operations.
 * 
 * This handler implements the functional reactive approach using Spring WebFlux's
 * RouterFunction pattern. It processes HTTP requests for role operations including
 * creation, retrieval, and listing of roles in a non-blocking, reactive manner.
 * 
 * <p>The handler follows the hexagonal architecture pattern by depending only on
 * input ports (use cases) and not on specific infrastructure details. It handles
 * request validation, response formatting, and error handling in a reactive way.</p>
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class RoleHandler {
    
    private final CreateRoleTrait createRoleTrait;
    private final GetRoleTrait getRoleTrait;
    private final Validator validator;
    
    /**
     * Constructs a new RoleHandler with the required dependencies.
     * 
     * @param createRoleTrait the use case for creating roles
     * @param getRoleTrait the use case for retrieving roles
     * @param validator the validator for input validation
     */
    public RoleHandler(CreateRoleTrait createRoleTrait,
                      GetRoleTrait getRoleTrait,
                      Validator validator) {
        this.createRoleTrait = createRoleTrait;
        this.getRoleTrait = getRoleTrait;
        this.validator = validator;
    }
    
    /**
     * Handles HTTP POST requests to create a new role.
     * 
     * This method processes role creation requests by:
     * <ul>
     *   <li>Extracting and validating the request body</li>
     *   <li>Delegating to the create role use case</li>
     *   <li>Returning the created role with HTTP 201 status</li>
     *   <li>Handling validation and business logic errors appropriately</li>
     * </ul>
     * 
     * @param request the server request containing the role creation data
     * @return a Mono that emits the server response with the created role or error information
     */
    public Mono<ServerResponse> createRole(ServerRequest request) {
        return request.bodyToMono(CreateRoleRequestDto.class)
            .flatMap(this::validateRequest)
            .flatMap(createRoleTrait::createRole)
            .flatMap(role -> ServerResponse.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(role))
            .onErrorResume(this::handleError);
    }
    
    /**
     * Handles HTTP GET requests to retrieve all roles.
     * 
     * This method processes requests to list all roles by:
     * <ul>
     *   <li>Delegating to the get role use case</li>
     *   <li>Streaming the results as JSON array</li>
     *   <li>Returning HTTP 200 status with the role list</li>
     * </ul>
     * 
     * @param request the server request (not used for this operation)
     * @return a Mono that emits the server response with all roles
     */
    public Mono<ServerResponse> getAllRoles(ServerRequest request) {
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(getRoleTrait.getAllRoles(), RoleResponseDto.class)
            .onErrorResume(this::handleError);
    }
    
    /**
     * Handles HTTP GET requests to retrieve a specific role by ID.
     * 
     * This method processes requests to get a role by its ID by:
     * <ul>
     *   <li>Extracting the role ID from the path variable</li>
     *   <li>Delegating to the get role use case</li>
     *   <li>Returning the role with HTTP 200 status if found</li>
     *   <li>Returning HTTP 404 status if not found</li>
     * </ul>
     * 
     * @param request the server request containing the role ID in the path
     * @return a Mono that emits the server response with the role or not found status
     */
    public Mono<ServerResponse> getRoleById(ServerRequest request) {
        return Mono.fromCallable(() -> {
                String idParam = request.pathVariable("id");
                try {
                    return Long.valueOf(idParam);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid role ID format: " + idParam);
                }
            })
            .flatMap(getRoleTrait::getRoleById)
            .flatMap(role -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(role))
            .switchIfEmpty(ServerResponse.notFound().build())
            .onErrorResume(this::handleError);
    }
    
    /**
     * Handles HTTP GET requests to search roles by name pattern.
     * 
     * This method processes requests to search roles by name pattern by:
     * <ul>
     *   <li>Extracting the search pattern from query parameters</li>
     *   <li>Delegating to the get role use case</li>
     *   <li>Streaming the matching results as JSON array</li>
     *   <li>Returning HTTP 200 status with the matching roles</li>
     * </ul>
     * 
     * @param request the server request containing the search pattern in query parameters
     * @return a Mono that emits the server response with matching roles
     */
    public Mono<ServerResponse> searchRolesByName(ServerRequest request) {
        return Mono.fromCallable(() -> {
                String namePattern = request.queryParam("name")
                    .orElseThrow(() -> new IllegalArgumentException("Name query parameter is required"));
                if (namePattern.trim().isEmpty()) {
                    throw new IllegalArgumentException("Name query parameter cannot be empty");
                }
                return namePattern.trim();
            })
            .flatMap(namePattern -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(getRoleTrait.getRolesByNamePattern(namePattern), RoleResponseDto.class))
            .onErrorResume(this::handleError);
    }
    
    /**
     * Validates the incoming request DTO using Bean Validation.
     * 
     * @param request the request DTO to validate
     * @return a Mono that emits the validated request or an error if validation fails
     */
    private Mono<CreateRoleRequestDto> validateRequest(CreateRoleRequestDto request) {
        Set<ConstraintViolation<CreateRoleRequestDto>> violations = validator.validate(request);
        
        if (violations.isEmpty()) {
            return Mono.just(request);
        }
        
        String errorMessage = violations.stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining(", "));
            
        return Mono.error(new IllegalArgumentException("Validation failed: " + errorMessage));
    }
    
    /**
     * Handles errors that occur during request processing.
     * 
     * This method provides centralized error handling for the handler methods,
     * converting domain exceptions and validation errors into appropriate HTTP responses.
     * 
     * @param throwable the error that occurred
     * @return a Mono that emits an appropriate error response
     */
    private Mono<ServerResponse> handleError(Throwable throwable) {
        if (throwable instanceof DuplicateRoleException) {
            return ServerResponse.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ErrorResponse(
                    "DUPLICATE_ROLE",
                    throwable.getMessage(),
                    HttpStatus.CONFLICT.value()
                ));
        }
        
        if (throwable instanceof RoleNotFoundException) {
            return ServerResponse.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ErrorResponse(
                    "ROLE_NOT_FOUND",
                    throwable.getMessage(),
                    HttpStatus.NOT_FOUND.value()
                ));
        }
        
        if (throwable instanceof IllegalArgumentException) {
            return ServerResponse.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ErrorResponse(
                    "INVALID_REQUEST",
                    throwable.getMessage(),
                    HttpStatus.BAD_REQUEST.value()
                ));
        }
        
        // Generic error handling
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value()
            ));
    }
    
    /**
     * Record representing an error response structure.
     * 
     * @param code the error code
     * @param message the error message
     * @param status the HTTP status code
     */
    public record ErrorResponse(
        String code,
        String message,
        int status
    ) {}
}