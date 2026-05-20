# Touché Manager — Backend

Sistema de gestión de torneos de esgrima. API REST construida con Spring Boot.

## Stack
- Java 17
- Spring Boot 3.x
- Spring Security (JWT)
- Spring Data JPA
- PostgreSQL 16
- MinIO (almacenamiento de archivos, compatible S3)
- Lombok
- MapStruct
- Jakarta Validation
- SpringDoc OpenAPI (Swagger en /swagger-ui.html)

## Arquitectura
Arquitectura en capas por módulo (Controller → Service → Repository).
Cada módulo tiene las carpetas: `controller`, `service`, `service/impl`, `repository`, `entity`, `dto`.

Módulos:
- `auth` → registro, login, JWT, roles
- `athlete` → perfil del atleta, documentos
- `tournament` → torneos, inscripciones, poules, eliminatorias
- `bout` → arbitraje, asaltos, marcador
- `payment` → integración Mercado Pago
- `shared` → excepción global (GlobalExceptionHandler), respuesta estándar (ApiResponse<T>)

Estructura de paquetes base: `com.touchemanager`

## Convenciones de código
- **TODO EL CÓDIGO Y ESTRUCTURA DEBE ESTAR EN INGLÉS**: nombres de clases, métodos, variables, comentarios en el código, mensajes de excepciones internas, descripciones de Swagger, mensajes de validación y registros de log. El usuario final no interactúa con esta capa. Excepción: documentación en README.md puede estar en español.
- Usar `@RequiredArgsConstructor` de Lombok para inyección por constructor (nunca @Autowired)
- Servicios siempre con interfaz + implementación en `service/impl`
- DTOs como Java Records cuando son solo de lectura; clases normales con Lombok si necesitan validaciones @Valid
- Todas las respuestas de la API usar ApiResponse<T> del shared
- Validaciones con Jakarta Validation (@NotBlank, @Email, @NotNull, etc.)
- Excepciones de negocio: lanzar excepciones personalizadas que extienden RuntimeException, capturadas en GlobalExceptionHandler

## Modelo de datos (Sprint 1)

### Usuario
- id (Long)
- email (String, único)
- password (String, hasheada con BCrypt)
- activo (boolean)
- fechaCreacion (LocalDateTime)
- roles (ManyToMany → Rol)

### Rol
- id (Long)
- nombre (Enum: ATLETA, ARBITRO, ORGANIZADOR, ADMIN)

### Atleta
- id (Long)
- usuario (OneToOne → Usuario)
- nombre, apellido, dni (String)
- fechaNacimiento (LocalDate)
- sexo (Enum: MASCULINO, FEMENINO)
- manoHabil (Enum: DIESTRO, ZURDO)
- club, provincia (String)

### Documento
- id (Long)
- atleta (ManyToOne → Atleta)
- tipo (Enum: APTO_MEDICO, COMPROBANTE_AFILIACION)
- urlArchivo (String, path en MinIO)
- fechaSubida (LocalDateTime)
- fechaVigencia (LocalDate, ingresada por el atleta)
- activo (boolean, solo uno activo por tipo por atleta)

## Seguridad
- Autenticación con JWT (biblioteca jjwt)
- El token incluye: userId, email, rolActivo, expiración 24hs
- Si el usuario tiene un solo rol → el JWT se genera directamente con ese rol
- Si tiene múltiples roles → el login devuelve la lista de roles y el frontend llama a POST /api/auth/select-role con el rol elegido
- Rutas públicas: /api/auth/**, /swagger-ui.html, /v3/api-docs/**
- Rutas protegidas: todo lo demás requiere JWT válido

## Endpoints definidos (Sprint 1)
- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/select-role
- GET  /api/athletes/profile
- POST /api/athletes/profile
- PUT  /api/athletes/profile
- POST /api/athletes/documents  (multipart/form-data)
- GET  /api/athletes/documents

## Entorno local
Levantar infraestructura con Docker Compose:
```bash
docker compose up -d
```
Servicios:
- PostgreSQL: localhost:5432
- MinIO API: http://localhost:9000
- MinIO Console: http://localhost:9001

Correr la API:
```bash
./mvnw spring-boot:run
```
API disponible en http://localhost:8080

## Reglas importantes para Claude Code
- No hardcodear credenciales, siempre leer desde application.yml con variables de entorno
- No usar @Autowired, siempre inyección por constructor con @RequiredArgsConstructor
- Antes de crear un archivo nuevo, revisar si ya existe la estructura del módulo
- Al crear un endpoint nuevo, agregar también el DTO correspondiente y la entrada en Swagger
- Los archivos subidos a MinIO se validan: solo PDF, JPG o PNG, máximo 5MB
