package co.onmind.hex4w;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Main Spring Boot WebFlux application class for the Hexagonal Architecture Reactive Template.
 * 
 * This application demonstrates a complete implementation of hexagonal architecture
 * (Ports and Adapters pattern) using Spring Boot WebFlux for reactive programming.
 * 
 * The application includes:
 * - Domain layer with reactive business logic
 * - Application layer with reactive use cases and ports
 * - Infrastructure layer with reactive adapters
 * - Complete reactive CRUD operations for Role entity
 * - H2 R2DBC reactive database
 * - Comprehensive reactive testing strategy
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 */
@SpringBootApplication
@EnableR2dbcRepositories(basePackages = "co.onmind.hex4jwebflux.infrastructure.persistence.repositories")
public class MicroHexApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroHexApplication.class, args);
    }
}