# Touche Manager - Backend

API backend en Spring Boot con módulos por dominio y soporte de almacenamiento en MinIO.

## Stack
- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- MinIO (S3 compatible)

## Estructura
- `src/main/java/com/touchemanager`
- `src/main/java/com/touchemanager/auth` (auth: controller, dto, entity, repository, service)
- `src/main/java/com/touchemanager/athlete` (athlete: controller, dto, entity, repository, service)
- `src/main/java/com/touchemanager/tournament` (tournament: controller, dto, entity, repository, service)
- `src/main/java/com/touchemanager/bout` (bout: controller, dto, entity, repository, service)
- `src/main/java/com/touchemanager/payment` (payment: controller, dto, entity, repository, service)
- `src/main/java/com/touchemanager/shared` (exception, response)
- `src/main/resources/application.yml`

# Entorno local

Este entorno levanta PostgreSQL y MinIO para desarrollo local.

## Requisitos
- Docker Desktop con Docker Compose v2

## Pasos
1. Crear el archivo `.env` a partir del ejemplo:
   - Copiar `.env.example` a `.env` y ajustar valores si hace falta.
2. Levantar los servicios:
   - `docker compose up -d`

## Servicios
- PostgreSQL: `localhost:5432`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`
