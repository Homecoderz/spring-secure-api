# Solutions d'implémentation — `spring-secure-api`

> Document de mise en oeuvre basé sur `AMELIORATIONS.md`.
>
> **Règle suivie :** aucun fichier source du projet n'est modifié ici. Les changements ci-dessous sont des solutions prêtes à appliquer, structurées dans le même ordre que le rapport d'améliorations.

---

## Tableau de lecture

| Niveau | Objectif | Approche |
|:---:|---|---|
| 🔴 | Sécurité critique | Supprimer les fuites de données et fermer les chemins d'accès dangereux |
| 🟠 | Stabilité haute | Normaliser les erreurs, éviter les `null`, structurer les réponses |
| 🟡 | Qualité moyenne | Renforcer les contrats API, JPA et Java |
| 🟢 | Qualité basse | Améliorer tests, configuration, observabilité et documentation |

---

# 🔴 Priorité 1 — CRITIQUE

## C-1 · Séparer le DTO d'inscription et le DTO de réponse utilisateur

### Objectif

Empêcher l'exposition du hash BCrypt dans les réponses REST. Le mot de passe ne doit exister que dans le DTO d'entrée utilisé par `POST /user/register`.

### Fichiers à créer

- `entities/dto/UserRegistrationDTO.java`
- `entities/dto/UserResponseDTO.java`

### Exemple d'implémentation

```java
package ci.homecoderz.springsecureapi.entities.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegistrationDTO {
    @NotBlank
    private String username;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank
    @Email
    private String email;

    private String firstname;
    private String lastname;
}
```

```java
package ci.homecoderz.springsecureapi.entities.dto;

import lombok.Data;

@Data
public class UserResponseDTO {
    private Integer id;
    private String username;
    private String email;
    private String firstname;
    private String lastname;
}
```

### Mapper recommandé

```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(UserRegistrationDTO dto);

    UserResponseDTO toResponseDTO(User user);
}
```

### Controller recommandé

```java
@GetMapping("/{userId}")
public ResponseEntity<UserResponseDTO> retrieveUserById(@PathVariable int userId) {
    return userService.findById(userId)
            .map(userMapper::toResponseDTO)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
}
```

---

## C-2 · Bloquer la modification de `role` et `enabled` par le client

### Objectif

Empêcher un utilisateur de s'auto-attribuer le rôle `ADMIN` ou de forcer son activation.

### Solution

Ne jamais accepter `role` et `enabled` depuis le body d'inscription. Ces valeurs doivent être fixées côté serveur dans `UserService`.

```java
public User store(UserRegistrationDTO dto) {
    User user = userMapper.toEntity(dto);
    user.setPassword(passwordEncoder.encode(dto.getPassword()));
    user.setRole("USER");
    user.setEnabled(true);

    return userRepository.save(user);
}
```

### Variante avec validation e-mail

```java
user.setEnabled(false);
```

Dans ce cas, un endpoint séparé d'activation devra confirmer l'adresse e-mail avant de passer `enabled` à `true`.

---

## C-3 · Désactiver `httpBasic()` dans la configuration JWT

### Objectif

Forcer un seul mode d'authentification : `Bearer JWT`.

### Fichier concerné

- `configuration/security/SecurityConfiguration.java`

### Exemple d'implémentation

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                    .requestMatchers(HttpMethod.POST, "/user/login", "/user/register").permitAll()
                    .anyRequest().authenticated())
            .httpBasic(AbstractHttpConfigurer::disable)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

---

## C-4 · Transformer les erreurs JWT en HTTP 401

### Objectif

Un token expiré, malformé ou signé avec une mauvaise clé doit produire `401 Unauthorized`, pas `500 Internal Server Error`.

### Fichier concerné

- `configuration/filters/JwtAuthenticationFilter.java`

### Exemple d'implémentation

```java
import io.jsonwebtoken.JwtException;

@Override
protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
) throws ServletException, IOException {
    final String authorizationHeader = request.getHeader("Authorization");

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);
        return;
    }

    try {
        final String jwtToken = authorizationHeader.substring(7);
        final String username = jwtService.extractUsername(jwtToken);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (username != null && authentication == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(jwtToken, userDetails)) {
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        filterChain.doFilter(request, response);
    } catch (JwtException | IllegalArgumentException exception) {
        SecurityContextHolder.clearContext();
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token invalide ou expiré");
    }
}
```

---

# 🟠 Priorité 2 — HAUTE

## H-1 · Remplacer le `null` de `StudentService.retrieveById()` par `Optional`

### Objectif

Eviter la `NullPointerException` dans `StudentController`.

### Service

```java
public Optional<Student> retrieveById(int id) {
    return studentRepository.findById(id);
}
```

### Controller

```java
@GetMapping("/{studentId}")
public ResponseEntity<StudentDTO> getStudentById(@PathVariable int studentId) {
    return studentService.retrieveById(studentId)
            .map(studentMapper::toDTO)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
}
```

---

## H-2 · Remplacer `GET /student/populate` par un `POST` protégé

### Objectif

Un endpoint `GET` ne doit jamais modifier la base de données. Le seed doit être volontaire, idempotent et réservé à un administrateur.

### Option recommandée : supprimer l'endpoint

Créer un script SQL, Flyway ou une commande de seed dédiée.

### Option acceptable : endpoint `POST` admin

```java
@PostMapping("/populate")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> populateStudentDB() {
    if (studentService.existsByMatricule("12345")) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    List<Student> students = List.of(
            Student.builder().matricule("12345").firstname("John").lastname("Doe").age(23).present(true).build(),
            Student.builder().matricule("786786").firstname("Alicia").lastname("Keys").age(34).present(true).build()
    );

    studentService.storeAll(students);
    return ResponseEntity.status(HttpStatus.CREATED).build();
}
```

### Méthodes service utiles

```java
public boolean existsByMatricule(String matricule) {
    return studentRepository.existsByMatricule(matricule);
}

public List<Student> storeAll(List<Student> students) {
    return studentRepository.saveAll(students);
}
```

---

## H-3 · Ajouter un gestionnaire global d'exceptions

### Objectif

Uniformiser les réponses d'erreur et éviter les réponses techniques exposant des détails internes.

### DTO d'erreur

```java
package ci.homecoderz.springsecureapi.entities.dto;

import java.time.Instant;

public record ErrorDTO(
        String message,
        int status,
        Instant timestamp
) {
    public static ErrorDTO of(String message, int status) {
        return new ErrorDTO(message, status, Instant.now());
    }
}
```

### Handler global

```java
package ci.homecoderz.springsecureapi.configuration.exceptions;

import ci.homecoderz.springsecureapi.entities.dto.ErrorDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorDTO> handleNotFound(EntityNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorDTO.of(exception.getMessage(), 404));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorDTO> handleAuthentication(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorDTO.of("Authentification échouée", 401));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> handleValidation(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest()
                .body(ErrorDTO.of("Requête invalide", 400));
    }
}
```

---

## H-4 · Faire retourner `Optional<User>` à `findByUsername()`

### Objectif

Rendre l'absence d'utilisateur explicite.

### Repository

```java
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
}
```

### CustomUserDetailsService

```java
@Override
public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return new CustomUserDetails(user);
}
```

### UserService

```java
public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
}
```

---

## H-5 · Retourner un `AuthResponseDTO` au lieu d'une `String`

### Objectif

Rendre la réponse de login stable et claire.

### DTO

```java
package ci.homecoderz.springsecureapi.entities.dto;

public record AuthResponseDTO(
        String token,
        String tokenType
) {
    public static AuthResponseDTO bearer(String token) {
        return new AuthResponseDTO(token, "Bearer");
    }
}
```

### Service

```java
public AuthResponseDTO authenticate(CredentialDTO credentialDTO) {
    Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                    credentialDTO.getPrincipal().trim(),
                    credentialDTO.getPassword().trim()
            )
    );

    User authenticatedUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return AuthResponseDTO.bearer(jwtService.generateToken(authenticatedUser));
}
```

### Controller

```java
@PostMapping("/login")
public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody CredentialDTO credentialDTO) {
    return ResponseEntity.ok(authService.authenticate(credentialDTO));
}
```

---

## H-6 · Réduire les claims JWT au strict nécessaire

### Objectif

Eviter de stocker des données personnelles dans un token lisible par le client.

### Exemple d'implémentation

```java
public String generateToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("role", user.getRole());

    return Jwts.builder()
            .claims(claims)
            .subject(user.getUsername())
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + 3600_000))
            .signWith(getKey())
            .compact();
}
```

### Règle pratique

Le JWT peut contenir :

- `sub` : identifiant stable de l'utilisateur, ici `username`
- `role` : autorisation minimale
- `iat` et `exp` : dates techniques

Il ne doit pas contenir :

- `email`
- `firstname`
- `lastname`
- données métier sensibles

---

# 🟡 Priorité 3 — MOYENNE

## M-1 · Ajouter `@Valid` et les annotations de validation

### Objectif

Centraliser la validation dans les DTOs et supprimer les validations manuelles incomplètes.

### CredentialDTO

```java
@Data
@NoArgsConstructor
public class CredentialDTO {
    @NotBlank
    private String principal;

    @NotBlank
    private String password;
}
```

### StudentDTO

```java
@Data
public class StudentDTO {
    private Integer id;

    @NotBlank
    private String firstname;

    @NotBlank
    private String lastname;

    @NotBlank
    private String matricule;

    @Min(1)
    private int age;

    private boolean present;
}
```

### Utilisation dans les controllers

```java
@PostMapping("/register")
public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRegistrationDTO userDTO) {
    UserResponseDTO createdUser = userMapper.toResponseDTO(userService.store(userDTO));
    URI location = URI.create("/user/" + createdUser.getId());
    return ResponseEntity.created(location).body(createdUser);
}
```

---

## M-2 · Utiliser `TeacherDTO` dans `TeacherController`

### Objectif

Ne plus exposer l'entité JPA `Teacher` directement dans l'API.

### Controller recommandé

```java
@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;
    private final TeacherMapper teacherMapper;

    @GetMapping
    public List<TeacherDTO> getTeachers() {
        return teacherService.retrieveAll().stream()
                .map(teacherMapper::toDTO)
                .toList();
    }

    @PostMapping
    public ResponseEntity<TeacherDTO> createTeacher(@Valid @RequestBody TeacherDTO teacherDTO) {
        Teacher savedTeacher = teacherService.store(teacherMapper.toEntity(teacherDTO));
        return ResponseEntity.status(HttpStatus.CREATED).body(teacherMapper.toDTO(savedTeacher));
    }
}
```

---

## M-3 · Ne pas retourner `Optional<Teacher>` dans la réponse REST

### Objectif

Retourner soit un enseignant, soit `404 Not Found`.

### Exemple d'implémentation

```java
@GetMapping("/{teacherId}")
public ResponseEntity<TeacherDTO> getTeacher(@PathVariable int teacherId) {
    return teacherService.retrieve(teacherId)
            .map(teacherMapper::toDTO)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
}
```

---

## M-4 · Appliquer les conventions de nommage Java

### Objectif

Utiliser le `camelCase` Java partout dans les variables, paramètres et champs.

### Exemples de renommage

```java
@GetMapping("/{userId}")
public ResponseEntity<UserResponseDTO> retrieveUserById(@PathVariable int userId) {
    return userService.findById(userId)
            .map(userMapper::toResponseDTO)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
}
```

```java
User authenticatedUser = userRepository.findByUsername(authentication.getName())
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
```

```java
private boolean present;
```

### Note MapStruct

Si la colonne existante s'appelle encore `is_present`, gérer la transition progressivement :

```java
@Column(name = "is_present")
private boolean present;
```

---

## M-5 · Ne plus muter le DTO dans `UserService.store()`

### Objectif

Eviter l'effet de bord qui remplace le mot de passe clair du DTO par un hash.

### Mauvaise pratique actuelle

```java
userDTO.setPassword(passwordEncoder.encode(userDTO.getPassword()));
```

### Solution recommandée

```java
public User store(UserRegistrationDTO dto) {
    User user = userMapper.toEntity(dto);
    user.setPassword(passwordEncoder.encode(dto.getPassword()));
    user.setRole("USER");
    user.setEnabled(true);

    return userRepository.save(user);
}
```

---

## M-6 · Ajouter les contraintes JPA manquantes

### Objectif

Faire respecter les invariants critiques au niveau base de données.

### User

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    private String firstname;
    private String lastname;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private boolean enabled;
}
```

### Student

```java
@Column(nullable = false, unique = true)
private String matricule;
```

### Teacher

```java
@Column(nullable = false)
private String firstname;

@Column(nullable = false)
private String lastname;

@Column(unique = true)
private String email;
```

---

## M-7 · Remplacer `String role` par une enum `Role`

### Objectif

Empêcher les rôles invalides comme `"GODMODE"`, `""` ou `"admin"`.

### Enum

```java
package ci.homecoderz.springsecureapi.entities.user;

public enum Role {
    USER,
    ADMIN,
    TEACHER
}
```

### Entité User

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private Role role;
```

### Initialisation à l'inscription

```java
user.setRole(Role.USER);
```

### Authorities

Dans `CustomUserDetails`, préférer produire une autorité Spring explicite :

```java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
}
```

---

## M-8 · Configurer explicitement la session en `STATELESS`

### Objectif

Une API JWT ne doit pas créer de session HTTP côté serveur.

### Exemple d'implémentation

```java
import org.springframework.security.config.http.SessionCreationPolicy;

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                    .requestMatchers(HttpMethod.POST, "/user/login", "/user/register").permitAll()
                    .anyRequest().authenticated())
            .httpBasic(AbstractHttpConfigurer::disable)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

---

# 🟢 Priorité 4 — BASSE

## B-1 · Ajouter une vraie couverture de tests

### Objectif

Tester les chemins critiques : JWT, authentification, inscription et accès protégé.

### Exemple de test `JwtService`

```java
@SpringBootTest
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void generateTokenShouldUseUsernameAsSubject() {
        User user = new User();
        user.setUsername("alice");
        user.setRole("USER");

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }
}
```

### Exemple de test controller avec MockMvc

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerShouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
```

---

## B-2 · Déplacer `show-sql=true` dans un profil dev

### Objectif

Eviter les logs SQL permanents en production.

### `application.properties`

```properties
spring.application.name=spring-secure-api
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3308/${spring.application.name}?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=${MYSQL_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.mvc.format.date=dd/MM/yyyy
application.security.jwt.secret=${JWT_SECRET}
```

### `application-dev.properties`

```properties
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE
```

### Lancement en dev

```powershell
.\mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## B-3 · Remplacer `ddl-auto=update` par Flyway et `validate`

### Objectif

Rendre les changements de schéma versionnés et maîtrisés.

### Dépendance Maven

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

### Configuration production

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

### Exemple de migration

Créer :

```text
src/main/resources/db/migration/V1__create_users_table.sql
```

```sql
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    firstname VARCHAR(255),
    lastname VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL
);
```

---

## B-4 · Ajouter la pagination sur les endpoints de liste

### Objectif

Eviter de charger toute la base en mémoire.

### UserController

```java
@GetMapping
public Page<UserResponseDTO> findAllUsers(Pageable pageable) {
    return userService.findAll(pageable).map(userMapper::toResponseDTO);
}
```

### UserService

```java
public Page<User> findAll(Pageable pageable) {
    return userRepository.findAll(pageable);
}
```

### Exemple d'appel

```http
GET /user?page=0&size=20&sort=username,asc
```

---

## B-5 · Ajouter OpenAPI / Swagger UI

### Objectif

Générer une documentation interactive des endpoints REST.

### Dépendance Maven

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.6</version>
</dependency>
```

### Configuration optionnelle

```java
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI springSecureApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("spring-secure-api")
                        .version("1.0.0")
                        .description("API sécurisée avec JWT"));
    }
}
```

### URL

```text
http://localhost:8080/swagger-ui.html
```

---

## B-6 · Ajouter du logging applicatif

### Objectif

Tracer les événements importants sans exposer de données sensibles.

### Exemple dans `AuthService`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    public AuthResponseDTO authenticate(CredentialDTO credentialDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        credentialDTO.getPrincipal().trim(),
                        credentialDTO.getPassword().trim()
                )
        );

        log.info("Authentification réussie pour username={}", authentication.getName());

        User authenticatedUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return AuthResponseDTO.bearer(jwtService.generateToken(authenticatedUser));
    }
}
```

### Exemple dans le filtre JWT

```java
catch (JwtException | IllegalArgumentException exception) {
    log.warn("Token JWT rejeté: {}", exception.getMessage());
    SecurityContextHolder.clearContext();
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token invalide ou expiré");
}
```

### Règle

Ne jamais logger :

- le mot de passe
- le token JWT complet
- le hash BCrypt
- les données personnelles inutiles

---

## B-7 · Renommer `CredentialDTO.principal` en `username`

### Objectif

Rendre le contrat REST plus intuitif pour le client.

### DTO recommandé

```java
@Data
@NoArgsConstructor
public class CredentialDTO {
    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
```

### AuthService

```java
Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
                credentialDTO.getUsername().trim(),
                credentialDTO.getPassword().trim()
        )
);
```

### Exemple de requête

```json
{
  "username": "alice",
  "password": "password-secret"
}
```

---

# Feuille de route d'application

## Sprint 1 — Sécurité critique

1. Créer `UserRegistrationDTO` et `UserResponseDTO`.
2. Adapter `UserMapper`, `UserService` et `UserController`.
3. Forcer `role = USER` et `enabled = true` côté serveur.
4. Désactiver `httpBasic()`.
5. Gérer les exceptions JWT dans `JwtAuthenticationFilter`.

## Sprint 2 — Stabilité

1. Remplacer les retours `null` par `Optional`.
2. Supprimer ou sécuriser `POST /student/populate`.
3. Ajouter `GlobalExceptionHandler`.
4. Passer `UserRepository.findByUsername()` en `Optional<User>`.
5. Introduire `AuthResponseDTO`.
6. Réduire les claims JWT.

## Sprint 3 — Qualité et cohérence

1. Ajouter `@Valid` dans les controllers.
2. Ajouter les contraintes Bean Validation dans les DTOs.
3. Utiliser `TeacherDTO` dans `TeacherController`.
4. Corriger les noms en `camelCase`.
5. Ajouter les contraintes JPA.
6. Remplacer le rôle `String` par une enum.
7. Configurer `SessionCreationPolicy.STATELESS`.

## Sprint 4 — Outillage

1. Ajouter les tests unitaires et tests web.
2. Séparer la configuration `dev` et `prod`.
3. Ajouter Flyway.
4. Ajouter la pagination.
5. Ajouter Swagger UI.
6. Ajouter le logging applicatif.
7. Renommer `principal` en `username`.

---

# Priorité d'exécution recommandée

```text
1. C-1 + C-2 : fermer la fuite du password et l'escalade de privilèges
2. C-3 + M-8 : verrouiller le mode d'authentification JWT stateless
3. C-4 + H-3 : rendre les erreurs de sécurité propres et contrôlées
4. H-4 + H-5 + H-6 : stabiliser l'authentification
5. H-1 + M-2 + M-3 : corriger les réponses REST incohérentes
6. M-1 + M-6 + M-7 : renforcer les contrats API et base de données
7. B-1 à B-7 : améliorer maintenance, documentation et exploitation
```

---

## Notes finales

- Les exemples sont alignés sur le package réel du projet : `ci.homecoderz.springsecureapi`.
- Les changements de DTOs peuvent casser le contrat JSON actuel : prévoir une mise à jour du README et des clients API.
- Les changements JPA doivent être accompagnés d'une migration SQL si une base existe déjà.
- Après application progressive, lancer au minimum :

```powershell
.\mvnw test
.\mvnw spring-boot:run
```

