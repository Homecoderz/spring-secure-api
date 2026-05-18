# spring-secure-api

API REST Spring Boot securisee avec Spring Security, JWT, Spring Data JPA et MySQL.

## Prerequis

- JDK 21
- MySQL accessible sur `localhost:3308`
- Maven Wrapper fourni par le projet (`mvnw.cmd` sous Windows)

## Configuration

La configuration principale se trouve dans `src/main/resources/application.properties`.

Variables d'environnement obligatoires :

| Variable | Description |
| --- | --- |
| `MYSQL_PASSWORD` | Mot de passe du compte MySQL configure dans `spring.datasource.username` |
| `JWT_SECRET` | Cle secrete Base64 utilisee pour signer et verifier les JWT |

La base utilisee est `spring-secure-api`. Elle est creee automatiquement si elle n'existe pas grace a `createDatabaseIfNotExist=true`.

Exemple PowerShell :

```powershell
$env:MYSQL_PASSWORD="votre_mot_de_passe_mysql"

$bytes = New-Object byte[] 64
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$env:JWT_SECRET=[Convert]::ToBase64String($bytes)
```

Pour conserver une configuration locale sans l'exposer, vous pouvez aussi creer un fichier ignore par Git comme `src/main/resources/application-local.properties`, puis lancer l'application avec le profil `local`.

## Lancement local

Verifier la version Java active :

```powershell
java -version
.\mvnw.cmd -version
```

Compiler :

```powershell
.\mvnw.cmd -DskipTests compile
```

Lancer l'API :

```powershell
.\mvnw.cmd spring-boot:run
```

Avec un profil local :

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Par defaut, l'API ecoute sur :

```text
http://localhost:8080
```

## Authentification JWT

Les endpoints publics sont :

- `POST /user/register`
- `POST /user/login`

Tous les autres endpoints demandent un JWT dans l'en-tete HTTP :

```http
Authorization: Bearer <token>
```

Exemple d'inscription :

```powershell
curl.exe -i -X POST http://localhost:8080/user/register `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"admin\",\"password\":\"admin123\",\"email\":\"admin@example.com\",\"firstname\":\"Admin\",\"lastname\":\"User\",\"role\":\"ADMIN\",\"enabled\":true}"
```

Exemple de connexion :

```powershell
$token = curl.exe -s -X POST http://localhost:8080/user/login `
  -H "Content-Type: application/json" `
  -d "{\"principal\":\"admin\",\"password\":\"admin123\"}"
```

Exemple d'appel protege :

```powershell
curl.exe -i http://localhost:8080/user `
  -H "Authorization: Bearer $token"
```

## Endpoints

### Utilisateurs

| Methode | Chemin | Auth | Reponse attendue |
| --- | --- | --- | --- |
| `POST` | `/user/register` | Public | `201 Created` si cree, `400 Bad Request` si le payload est incomplet |
| `POST` | `/user/login` | Public | `200 OK` avec le token JWT, `400 Bad Request` si le payload est incomplet, `401 Unauthorized` si les identifiants sont invalides |
| `GET` | `/user` | JWT | Liste des utilisateurs |
| `GET` | `/user/{user_id}` | JWT | Utilisateur, ou `404 Not Found` si absent |
| `GET` | `/user/retrieve/{username}` | JWT | Utilisateur, ou `404 Not Found` si absent |

Payload `UserDTO` :

```json
{
  "username": "admin",
  "password": "admin123",
  "email": "admin@example.com",
  "firstname": "Admin",
  "lastname": "User",
  "role": "ADMIN",
  "enabled": true
}
```

Payload de connexion :

```json
{
  "principal": "admin",
  "password": "admin123"
}
```

### Etudiants

| Methode | Chemin | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/student` | JWT | Liste les etudiants |
| `GET` | `/student/{student_id}` | JWT | Recupere un etudiant par id |
| `POST` | `/student/add` | JWT | Cree un etudiant |
| `GET` | `/student/populate` | JWT | Insere un jeu de donnees de demo |

Payload `StudentDTO` :

```json
{
  "firstname": "John",
  "lastname": "Doe",
  "matricule": "12345",
  "age": 23,
  "is_present": true
}
```

### Enseignants

| Methode | Chemin | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/teacher` | JWT | Liste les enseignants |
| `GET` | `/teacher/{teacher_id}` | JWT | Recupere un enseignant par id |
| `POST` | `/teacher` | JWT | Cree un enseignant |

Payload `Teacher` :

```json
{
  "firstname": "Marie",
  "lastname": "Kouassi",
  "email": "marie.kouassi@example.com",
  "discipline": "Mathematiques",
  "grade": "Professeur"
}
```

## Notes techniques

- Les mots de passe utilisateurs sont hashes avec BCrypt.
- Le token JWT est genere par `JwtService` et expire apres 1 heure.
- La configuration actuelle conserve aussi `httpBasic`; les appels API proteges doivent cependant utiliser le JWT Bearer.
- `spring.jpa.hibernate.ddl-auto=update` met a jour le schema automatiquement en local.
