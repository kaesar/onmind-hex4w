package co.onmind.hex4w.domain.models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Role domain model representing a role entity in the system.
 * 
 * This class contains the core business logic and validation rules for roles.
 * It follows domain-driven design principles by encapsulating business rules
 * and maintaining data integrity.
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 */
public class Role {
    
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    
    public Role() {}
    
    public Role(String name) {
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }
    
    public Role(Long id, String name, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id) && 
               Objects.equals(name, role.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
    
    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}