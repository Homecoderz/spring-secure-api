# Rapport d'améliorations — `spring-secure-api`

> **Portée de l'analyse :** sécurité · architecture · qualité de code · base de données · tests · configuration  
> **Date :** 2026-05-15  
> **Statut :** aucun fichier source n'a été modifié — document consultatif uniquement

---

## Tableau de bord

| Priorité | Libellé | Nombre |
|:---:|---|:---:|
| 🔴 | **CRITIQUE** — à corriger immédiatement | 4 |
| 🟠 | **HAUTE** — à corriger dans la semaine | 6 |
| 🟡 | **MOYENNE** — à planifier | 8 |
| 🟢 | **BASSE** — qualité & confort | 7 |
| **Total** | | **25** |

---

## Légende

```
🔴 CRITIQUE  →  Faille de sécurité exploitable ou crash garanti en production
🟠 HAUTE     →  Comportement incorrect ou risque sécurité indirect
🟡 MOYENNE   →  Mauvaise pratique nuisant à la maintenabilité ou à la cohérence
🟢 BASSE     →  Amélioration de confort, lisibilité, outillage
```

---

## 🔴 Priorité 1 — CRITIQUE

### C-1 · Le `UserDTO` expose le hash du mot de passe

**Fichier :** `entities/dto/UserDTO.java` · `controllers/user/UserController.java`

Le champ `password` est présent dans `UserDTO`. Puisque ce DTO est utilisé à la fois en **entrée** (inscription) et en **sortie** (réponses GET), chaque appel à `GET /user` ou `GET /user/{id}` retourne le hash BCrypt du mot de passe à n'importe quel utilisateur authentifié.

```
// Réponse actuelle de GET /user/{id}
{
  "id": 1,
  "username": "alice",
  "password": "$2a$10$...",   ← hash exposé publiquement
  "email": "alice@example.com",
  "role": "USER",
  "enabled": true
}
```

**Correction recommandée :** Séparer en deux DTO distincts :
- `UserRegistrationDTO` — utilisé en entrée (contient le password en clair)
- `UserResponseDTO` — utilisé en sortie (sans password, sans role, sans enabled)

---

### C-2 · Les champs `role` et `enabled` sont modifiables par le client

**Fichier :** `entities/dto/UserDTO.java` · `controllers/user/UserController.java` · `entities/mapper/UserMapper.java`

Lors de l'inscription (`POST /user/register`), le client peut envoyer :

```json
{
  "username": "pirate",
  "password": "secret",
  "email": "pirate@evil.com",
  "role": "ADMIN",
  "enabled": true
}
```

Le mapper copie ces valeurs directement vers l'entité `User`, permettant à n'importe qui de s'auto-promouvoir administrateur.

**Correction recommandée :** Ignorer `role` et `enabled` à l'inscription ; forcer `role = "USER"` et `enabled = true` (ou `false` si validation e-mail souhaitée) dans `UserService.store()`.

---

### C-3 · `httpBasic()` activé en parallèle du JWT

**Fichier :** `configuration/security/SecurityConfiguration.java` · `README.md` (mentionné)

```java
http.csrf(AbstractHttpConfigurer::disable)
    .authorizeHttpRequests(...)
    .httpBasic(Customizer.withDefaults())  // ← ouvre un vecteur d'attaque supplémentaire
    .addFilterBefore(jwtAuthenticationFilter, ...);
```

L'authentification HTTP Basic est active, ce qui ouvre un second chemin d'accès non intentionnel. Tout client qui envoie un header `Authorization: Basic <base64(user:pass)>` contourne le flux JWT.

**Correction recommandée :**
```java
.httpBasic(AbstractHttpConfigurer::disable)
```

---

### C-4 · Le filtre JWT ne gère pas les exceptions → HTTP 500 en production

**Fichier :** `configuration/filters/JwtAuthenticationFilter.java`

Si le token JWT est malformé, expiré ou a une signature invalide, `jwtService.extractUsername()` lance une exception non catchée (`JwtException`). Spring retourne alors une `HTTP 500 Internal Server Error` au lieu d'une `HTTP 401 Unauthorized`, ce qui :
- révèle une stack trace potentiellement sensible
- ne communique pas correctement au client la cause du rejet

**Correction recommandée :** Entourer le bloc d'extraction du token d'un `try/catch` renvoyant proprement une `401` :

```java
try {
    final String username = jwtService.extractUsername(jwtToken);
    // ...
} catch (JwtException e) {
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token invalide ou expiré");
    return;
}
```

---

## 🟠 Priorité 2 — HAUTE

### H-1 · `StudentService.retrieveById()` retourne `null` → NullPointerException

**Fichier :** `services/student/StudentService.java` · `controllers/student/StudentController.java`

```java
// Service
public Student retrieveById(int id) {
    return studentRepository.findById(id).orElse(null);  // ← retourne null
}

// Controller
public StudentDTO getStudentById(@PathVariable int student_id) {
    return studentMapper.toDTO(studentService.retrieveById(student_id));  // ← NPE si null
}
```

Si l'ID n'existe pas, le controller plante avec une `NullPointerException` non gérée.

**Correction recommandée :** Retourner `Optional<Student>` depuis le service et renvoyer `404` dans le controller, comme le fait déjà `TeacherController`.

---

### H-2 · `GET /student/populate` modifie la base de données

**Fichier :** `controllers/student/StudentController.java`

```java
@GetMapping("/populate")      // ← GET ne doit JAMAIS modifier l'état
public void populateStudentDB() {
    // insère 5 lignes en base
}
```

Un endpoint GET qui écrit en base viole le principe d'idempotence REST. Il suffit à quelqu'un (navigateur, bot de crawl, test de monitoring) d'appeler ce GET pour dupliquer les données indéfiniment.

**Correction recommandée :** Soit supprimer cet endpoint (remplacer par des scripts SQL de seed), soit le convertir en `POST` protégé par un rôle `ADMIN` et un guard "already populated".

---

### H-3 · Aucun gestionnaire global d'exceptions (`@ControllerAdvice`)

**Impact :** tous les controllers

En l'absence d'un `@ControllerAdvice`, toute exception non gérée remonte à Spring Boot qui retourne des réponses génériques (parfois avec stack traces). Le comportement d'erreur est incohérent selon le controller.

**Correction recommandée :** Créer une classe `GlobalExceptionHandler` :

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorDTO> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(404).body(new ErrorDTO(e.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorDTO> handleAuth(AuthenticationException e) {
        return ResponseEntity.status(401).body(new ErrorDTO("Authentification échouée"));
    }
}
```

---

### H-4 · `UserRepository.findByUsername()` retourne `null` au lieu d'`Optional`

**Fichier :** `repositories/UserRepository.java` · `services/user/CustomUserDetailsService.java`

```java
User findByUsername(String username);  // ← null si absent
```

Chaque appelant doit penser à vérifier `null` manuellement. `CustomUserDetailsService` le fait, mais `AuthService` ne le fait pas (il appelle `jwtService.generateToken(null)` si l'utilisateur disparaît entre l'authentification et la recherche).

**Correction recommandée :**
```java
Optional<User> findByUsername(String username);
```

---

### H-5 · `AuthService` retourne une `String` brute au lieu d'un objet structuré

**Fichier :** `services/authentication/AuthService.java`

```java
public String authenticate(CredentialDTO credentialDTO) {
    // ...
    return jwtService.generateToken(authenticated_user);  // ← token brut
    // ou : return "Aucun utilisateur trouvé";            // ← message d'erreur dans le body d'un 200
}
```

La méthode mélange deux cas : un token JWT valide ou un message d'erreur, les deux dans une `String`. Le controller ne peut pas distinguer les deux sans analyser le contenu.

**Correction recommandée :** Créer un `AuthResponseDTO` avec le token, et lancer une exception dédiée en cas d'échec.

---

### H-6 · Le JWT embarque trop de données personnelles dans les claims

**Fichier :** `services/authentication/JwtService.java`

```java
claims.put("id", user.getId());
claims.put("username", user.getUsername());
claims.put("email", user.getEmail());          // ← PII
claims.put("firstname", user.getFirstname());  // ← PII
claims.put("lastname", user.getLastname());    // ← PII
claims.put("role", user.getRole());
```

Le payload JWT est encodé en Base64 (non chiffré). N'importe qui peut décoder le token côté client et lire toutes ces informations. En cas de fuite d'un token, toutes les données personnelles sont compromises.

**Correction recommandée :** Ne garder que le strict nécessaire :
```java
claims.put("role", user.getRole());
// subject = username (déjà positionné via .subject())
```

---

## 🟡 Priorité 3 — MOYENNE

### M-1 · Aucune annotation `@Valid` sur les `@RequestBody`

**Fichier :** `controllers/user/UserController.java` · `controllers/student/StudentController.java` · `controllers/teacher/TeacherController.java`

La dépendance `spring-boot-starter-validation` est incluse dans le `pom.xml`, mais aucun des controllers ne l'utilise. La validation est faite manuellement (ex. `isValidRegistrationRequest()`) et de façon incomplète.

**Correction recommandée :** Ajouter des annotations de validation sur les DTOs (`@NotBlank`, `@Email`, `@Size`) et `@Valid` sur les paramètres des controllers. Cela simplifie les méthodes helper et garantit une validation systématique.

---

### M-2 · `TeacherController` expose l'entité JPA directement (pas de DTO)

**Fichier :** `controllers/teacher/TeacherController.java`

```java
@GetMapping
public List<Teacher> getTeachers() { ... }   // ← entité JPA exposée

@PostMapping
public Teacher createTeacher(@RequestBody Teacher teacher) { ... }  // ← idem
```

Exposer l'entité JPA directement lie le contrat API au schéma de base de données. Toute modification du modèle brise potentiellement les clients. `TeacherMapper` et `TeacherDTO` existent mais ne sont pas utilisés ici.

**Correction recommandée :** Utiliser `TeacherDTO` + `TeacherMapper` comme dans `UserController` et `StudentController`.

---

### M-3 · `TeacherController.getTeacher()` retourne `Optional<Teacher>` comme réponse REST

**Fichier :** `controllers/teacher/TeacherController.java`

```java
@GetMapping("/{teacher_id}")
public Optional<Teacher> getTeacher(@PathVariable int teacher_id) {
    return teacherService.retrieve(teacher_id);  // ← sérialise {"present": true, "value": {...}}
}
```

Spring sérialise `Optional` en JSON comme un objet `{"present": true, "value": {...}}` ou `{}`, ce qui est inattendu pour un client REST.

**Correction recommandée :** Retourner `ResponseEntity<TeacherDTO>` avec `.map()` et `.orElse(notFound())`.

---

### M-4 · Conventions de nommage Java violées

**Fichiers concernés :** plusieurs

| Localisation | Code actuel | Convention Java attendue |
|---|---|---|
| `StudentService.java` | param `teacher_id` | `teacherId` (camelCase) |
| `AuthService.java` | var `authenticated_user` | `authenticatedUser` |
| `Student.java` | champ `is_present` | `present` (Lombok génère `isPresent()`) |
| `StudentDTO.java` | champ `is_present` | `present` |
| `UserController.java` | `@PathVariable int user_id` | `userId` |
| `StudentController.java` | `@PathVariable int student_id` | `studentId` |

---

### M-5 · `UserService.store()` mute le DTO reçu en paramètre

**Fichier :** `services/user/UserService.java`

```java
public User store(UserDTO userDTO) {
    userDTO.setPassword(passwordEncoder.encode(userDTO.getPassword()));  // ← mute l'objet appelant
    return userRepository.save(userMapper.toEntity(userDTO));
}
```

Modifier un objet reçu en paramètre est un effet de bord. Si le même `UserDTO` est réutilisé après l'appel, il contiendra le hash BCrypt à la place du mot de passe original, ce qui peut provoquer des comportements imprévisibles.

**Correction recommandée :** Encoder le password lors de la construction de l'entité, pas sur le DTO.

---

### M-6 · Aucune contrainte de base de données sur les champs critiques

**Fichiers :** `entities/user/User.java` · `entities/student/Student.java` · `entities/teacher/Teacher.java`

Les contraintes de validité doivent exister au niveau de la base de données, indépendamment des validations applicatives. Plusieurs colonnes critiques manquent de contraintes JPA :

| Entité | Champ | Contrainte manquante |
|---|---|---|
| `User` | `username`, `password`, `email` | `nullable = false` |
| `Student` | `matricule` | `unique = true`, `nullable = false` |
| `Teacher` | `email` | `unique = true` |
| `Teacher` | `firstname`, `lastname` | `nullable = false` |

---

### M-7 · Le rôle utilisateur est une `String` sans enum

**Fichier :** `entities/user/User.java` · `entities/dto/UserDTO.java`

```java
private String role;  // ← rien n'empêche "GODMODE" ou "" d'être stocké
```

Sans typage fort, n'importe quelle chaîne peut être stockée comme rôle. Les comparaisons de rôles dans `getAuthorities()` deviennent fragiles.

**Correction recommandée :**
```java
public enum Role { USER, ADMIN, TEACHER }
```
Avec `@Enumerated(EnumType.STRING)` sur le champ JPA.

---

### M-8 · La session n'est pas configurée comme `STATELESS` explicitement

**Fichier :** `configuration/security/SecurityConfiguration.java`

Une API JWT pure ne doit jamais créer de session HTTP. Sans configuration explicite, Spring Security peut créer des sessions `JSESSIONID`, ce qui est inutile et constitue une surface d'attaque supplémentaire.

**Correction recommandée :**
```java
.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

---

## 🟢 Priorité 4 — BASSE

### B-1 · Couverture de tests quasi nulle

**Fichier :** `src/test/`

Le projet ne contient qu'un seul test `contextLoads()` qui vérifie uniquement que Spring démarre. Aucun test ne couvre les services, la logique JWT, les controllers ou la sécurité.

**Recommandations de départ :**
- Tests unitaires : `JwtService` (génération, validation, expiration), `AuthService`, `UserService`
- Tests d'intégration : `POST /user/register`, `POST /user/login`, accès protégé sans token
- Utiliser `@WebMvcTest` pour les controllers et `MockMvc` pour simuler les requêtes HTTP

---

### B-2 · `show-sql=true` actif en permanence

**Fichier :** `src/main/resources/application.properties`

```properties
spring.jpa.show-sql=true   # ← toutes les requêtes SQL sont loggées en production
```

En production, logger toutes les requêtes SQL est coûteux en I/O et peut exposer des données sensibles dans les logs.

**Correction recommandée :** Déplacer dans un profil `application-dev.properties` et utiliser un logger SQL dédié (ex. `p6spy` ou `logging.level.org.hibernate.SQL=DEBUG`) en développement uniquement.

---

### B-3 · `ddl-auto=update` risqué hors développement

**Fichier :** `src/main/resources/application.properties`

```properties
spring.jpa.hibernate.ddl-auto=update
```

En mode `update`, Hibernate tente d'altérer automatiquement le schéma à chaque démarrage. Cela peut provoquer des pertes de données ou des migrations partielles silencieuses en production.

**Correction recommandée :**
- `update` → uniquement en développement
- Production : utiliser un outil de migration dédié comme **Flyway** ou **Liquibase**, et passer `ddl-auto=validate`

---

### B-4 · Aucune pagination sur les endpoints de liste

**Fichier :** `controllers/user/UserController.java` · `controllers/student/StudentController.java` · `controllers/teacher/TeacherController.java`

```java
@GetMapping
public List<UserDTO> findAllUsers() {   // ← charge TOUT en mémoire
    return userService.findAll()...
}
```

Sans pagination, ces endpoints chargent tous les enregistrements en mémoire, ce qui peut provoquer des `OutOfMemoryError` à mesure que les données grandissent.

**Correction recommandée :** Utiliser `Pageable` de Spring Data :
```java
@GetMapping
public Page<UserDTO> findAllUsers(Pageable pageable) {
    return userRepository.findAll(pageable).map(userMapper::toDTO);
}
```

---

### B-5 · Aucune documentation OpenAPI / Swagger

Le projet n'a aucune documentation d'API auto-générée. Les développeurs doivent lire le README pour connaître les endpoints.

**Correction recommandée :** Ajouter `springdoc-openapi-starter-webmvc-ui` au `pom.xml`. La documentation interactive sera alors disponible à `http://localhost:8080/swagger-ui.html` sans configuration supplémentaire.

---

### B-6 · Aucun logging applicatif

Les services ne produisent aucun log. En cas d'anomalie, il est impossible de tracer le comportement de l'application sans debugger.

**Correction recommandée :** Ajouter `@Slf4j` (Lombok) sur les classes clés et logger les événements importants :
- Authentification réussie / échouée
- Création d'un utilisateur
- Token JWT rejeté

---

### B-7 · `CredentialDTO.principal` — nommage peu intuitif

**Fichier :** `entities/dto/CredentialDTO.java`

```java
private String principal;   // ← "principal" est un terme de sécurité interne, pas du domaine métier
```

Le champ s'appelle `principal` dans le DTO, mais les clients REST s'attendraient à `username` ou `usernameOrEmail`. C'est une confusion inutile pour les consommateurs de l'API.

**Correction recommandée :** Renommer en `username` (ou `login` si l'authentification par email est prévue).

---

## Feuille de route recommandée

```
Sprint 1 — Sécurité critique
  ✦ C-1  Séparer UserDTO en entrée/sortie (supprimer password des réponses)
  ✦ C-2  Bloquer l'auto-attribution de role/enabled
  ✦ C-3  Désactiver httpBasic()
  ✦ C-4  Gérer les exceptions JWT dans le filtre

Sprint 2 — Stabilité
  ✦ H-1  Corriger le risque de NPE dans StudentService/Controller
  ✦ H-2  Supprimer ou sécuriser GET /student/populate
  ✦ H-3  Ajouter @ControllerAdvice global
  ✦ H-4  Passer findByUsername() en Optional
  ✦ H-5  Retourner AuthResponseDTO depuis AuthService
  ✦ H-6  Alléger les claims JWT

Sprint 3 — Qualité & cohérence
  ✦ M-1  Ajouter @Valid + annotations de validation sur les DTOs
  ✦ M-2  Utiliser TeacherDTO dans TeacherController
  ✦ M-3  Corriger le retour Optional dans TeacherController
  ✦ M-4  Corriger les conventions de nommage (camelCase)
  ✦ M-5  Corriger la mutation du DTO dans UserService
  ✦ M-6  Ajouter les contraintes JPA manquantes
  ✦ M-7  Remplacer String role par une enum Role
  ✦ M-8  Configurer SessionCreationPolicy.STATELESS

Sprint 4 — Qualité & outillage
  ✦ B-1  Ajouter des tests unitaires et d'intégration
  ✦ B-2  Déplacer show-sql dans le profil dev
  ✦ B-3  Passer à Flyway et ddl-auto=validate
  ✦ B-4  Ajouter la pagination sur les listes
  ✦ B-5  Intégrer Springdoc OpenAPI / Swagger UI
  ✦ B-6  Ajouter du logging avec @Slf4j
  ✦ B-7  Renommer CredentialDTO.principal → username
```

---

*Document généré par analyse statique du code source — spring-secure-api · 2026-05-15*