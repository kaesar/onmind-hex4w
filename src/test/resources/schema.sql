-- Schema for R2DBC tests
-- This file is automatically executed by Spring Boot during test startup
-- NOTE: identifiers are double-quoted lowercase (see src/main/resources/schema.sql):
-- Spring Data R2DBC quotes H2 identifiers and H2 matches them case-sensitively.

CREATE TABLE IF NOT EXISTS "roles" (
    "id" BIGINT AUTO_INCREMENT PRIMARY KEY,
    "name" VARCHAR(255) NOT NULL UNIQUE,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
