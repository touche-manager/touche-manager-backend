# Touché Manager — Backend 🤺

Sistema de gestión integral para torneos de esgrima, desarrollado como Trabajo Final Integrador de la Tecnicatura Universitaria en Programación (UTN FRC).

## 📖 Acerca del Proyecto

La esgrima argentina carece de una plataforma unificada para gestionar competencias. **Touché Manager** nace para resolver este problema, ofreciendo un ecosistema digital integrado que abarca:
- Portal del Atleta (gestión de perfil y documentación médica).
- Gestión y motor de Torneos (armado de poules asistido por IA, cálculo de eliminatorias).
- Arbitraje en tiempo real desde dispositivos móviles.

Este repositorio contiene la **API REST** del sistema, construida bajo una arquitectura en capas por módulo (Domain-Driven Design simplificado).

## 🛠️ Stack Tecnológico

- **Lenguaje:** Java 17+
- **Framework Core:** Spring Boot 3.3.x
- **Persistencia:** Spring Data JPA + Hibernate
- **Base de Datos:** PostgreSQL 16
- **Almacenamiento de Archivos:** MinIO (S3 Compatible)
- **Seguridad:** Spring Security + JWT
- **Herramientas de Desarrollo:** Lombok, MapStruct, Jakarta Validation
- **Documentación de API:** SpringDoc OpenAPI (Swagger)

## 📂 Estructura del Proyecto

El código está organizado modularmente bajo el paquete `com.touchemanager`:
- `auth`: Registro, autenticación JWT y gestión de roles.
- `athlete`: Perfil del esgrimista y validación de documentos médicos.
- `tournament`: Gestión de inscripciones y lógica deportiva.
- `bout`: Control de asaltos y arbitraje.
- `shared`: Excepciones globales, configuraciones de seguridad y DTOs genéricos.

## 🚀 Guía de Instalación y Ejecución

### Prerrequisitos
- Docker y Docker Compose instalados.
- Java 17 (JDK) instalado.

### 1. Levantar la Infraestructura (Base de Datos y MinIO)
El proyecto incluye un archivo `docker-compose.yml` preconfigurado.
```bash
docker compose up -d
```
Esto levantará:
- PostgreSQL en `localhost:5432`
- MinIO (API) en `localhost:9000` y su Consola Web en `localhost:9001`

### 2. Variables de Entorno
Asegúrese de contar con un archivo `.env` en la raíz del proyecto con las credenciales necesarias (ver `.env.example`).

### 3. Ejecutar la Aplicación
Puede utilizar el wrapper de Maven incluido para levantar el servidor:
```bash
./mvnw spring-boot:run
```
La API estará disponible en `http://localhost:8080`.

### 4. Documentación de la API
Una vez que el servidor esté corriendo, puede explorar e interactuar con los endpoints a través de Swagger UI:
👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

---
*Desarrollado para la Universidad Tecnológica Nacional - Facultad Regional Córdoba (2026).*
