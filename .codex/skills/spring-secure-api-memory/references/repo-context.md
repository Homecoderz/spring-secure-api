# Repo Context

## Stack

- Java 21
- Spring Boot 4.0.6
- Spring Security
- Spring Data JPA
- Spring Web MVC
- Spring Validation
- MySQL
- JWT via `io.jsonwebtoken`
- MapStruct
- Lombok

## Runtime

- App name: `spring-secure-api`
- Default port: `8080`
- Database URL targets MySQL on `localhost:3308`
- Hibernate schema mode: `update`

## Main Functional Areas

- `controllers/user`: registration, login, user lookup
- `controllers/student`: student CRUD-style read/create endpoints
- `controllers/teacher`: teacher read/create endpoints
- `services/authentication`: login flow and JWT generation or validation
- `configuration/security` and `configuration/filters`: Spring Security chain and JWT request filter
- `repositories`: JPA access for users, students, teachers
- `entities`, `entities/dto`, `entities/mapper`: persistence models, DTOs, MapStruct mapping

## Authentication Flow

1. `POST /user/register` stays public.
2. `POST /user/login` stays public.
3. `AuthService` authenticates through `AuthenticationManager`.
4. `JwtService` generates a signed JWT with user id, email, and role claims.
5. `JwtAuthenticationFilter` reads `Authorization: Bearer <token>`, validates the token, and populates the Spring Security context.
6. All remaining endpoints require authentication.

## Known Current Details

- `SecurityConfiguration` disables CSRF and `httpBasic`.
- `JwtService` builds the signing key from `application.security.jwt.secret`.
- `UserController` currently validates login requests with `getUsername()` and registration requests with basic non-empty checks.
- `README.md` describes env-driven secrets, but `application.properties` currently contains direct datasource and JWT secret values. Treat configuration handling as sensitive and verify it before relying on docs alone.

## API Surface

- Public:
  - `POST /user/register`
  - `POST /user/login`
- Protected:
  - `GET /user`
  - `GET /user/{user_id}`
  - `GET /user/retrieve/{username}`
  - `GET /student`
  - `GET /student/{student_id}`
  - `POST /student/add`
  - `GET /student/populate`
  - `GET /teacher`
  - `GET /teacher/{teacher_id}`
  - `POST /teacher`
