# Centre de Formation - Application de Gestion

## Description
Application web de gestion d'un centre de formation développée avec Spring Boot 3.x et Java 17.

## Stack Technique
- **Backend**: Java 17, Spring Boot 3.2.0
- **Frameworks**: Spring MVC, Spring Data JPA, Spring Security
- **Base de données**: MySQL 8.0+
- **Template Engine**: Thymeleaf + Bootstrap
- **API**: REST avec JWT
- **Build**: Maven
- **Sécurité**: Spring Security + JWT

## Fonctionnalités par Rôle

### ADMIN
- Gestion complète des étudiants, formateurs, cours
- Gestion des inscriptions, sessions pédagogiques, groupes
- Gestion des spécialités et du planning
- Reporting et paramètres techniques

### FORMATEUR
- Gestion de ses cours
- Consultation du planning
- Saisie et modification des notes de ses étudiants

### ETUDIANT
- Consultation de ses cours, notes et emploi du temps
- Inscription/désinscription aux cours

## Structure du Projet

```
centre-formation/
├── src/
│   ├── main/
│   │   ├── java/com/centreformation/
│   │   │   ├── entity/           # Entités JPA
│   │   │   │   ├── Utilisateur.java
│   │   │   │   ├── Role.java
│   │   │   │   ├── Etudiant.java
│   │   │   │   ├── Formateur.java
│   │   │   │   ├── Cours.java
│   │   │   │   ├── Inscription.java
│   │   │   │   ├── Note.java
│   │   │   │   ├── Specialite.java
│   │   │   │   ├── Groupe.java
│   │   │   │   ├── SessionPedagogique.java
│   │   │   │   └── SeanceCours.java
│   │   │   ├── repository/       # Repositories Spring Data
│   │   │   ├── service/          # Logique métier
│   │   │   ├── controller/       # Controllers MVC + REST
│   │   │   ├── config/           # Configuration Spring
│   │   │   └── CentreFormationApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       ├── sql/
│   │       │   └── create_database_centre_formation.sql
│   │       └── templates/        # Templates Thymeleaf
│   └── test/
└── pom.xml
```

## Modèle de Données

### Entités Principales
- **Utilisateur**: Compte utilisateur avec authentification
- **Role**: ADMIN, FORMATEUR, ETUDIANT
- **Etudiant**: Données étudiant avec matricule unique
- **Formateur**: Données formateur
- **Cours**: Cours avec code unique
- **Inscription**: Relation étudiant-cours
- **Note**: Notes avec validation (0-20)
- **Specialite**: Filières de formation
- **Groupe**: Groupes d'étudiants
- **SessionPedagogique**: Sessions/années scolaires
- **SeanceCours**: Séances de cours planifiées

### Relations
- Utilisateur ↔ Role (ManyToMany)
- Utilisateur ↔ Etudiant/Formateur (OneToOne)
- Etudiant ↔ Groupe (ManyToMany)
- Cours ↔ Groupe (ManyToMany)
- Etudiant → Inscription ← Cours
- Etudiant → Note ← Cours

## Configuration

### Base de Données MySQL

#### 1. Créer la base de données
```bash
mysql -u root -p < src/main/resources/sql/create_database_centre_formation.sql
```

#### 2. Configuration (application-dev.properties)
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/centre_formation
spring.datasource.username=root
spring.datasource.password=root
```

### Profils
- **dev**: Développement avec MySQL local
- **prod**: Production avec variables d'environnement

## Installation et Démarrage

### Prérequis
- Java 17+
- Maven 3.8+
- MySQL 8.0+

### Étapes

1. **Cloner le projet**
```bash
cd c:\Dev\projet
```

2. **Créer la base de données**
```bash
mysql -u root -p < src/main/resources/sql/create_database_centre_formation.sql
```

3. **Configurer application-dev.properties**
Adapter les identifiants MySQL si nécessaire.

4. **Compiler le projet**
```bash
mvn clean install
```

5. **Lancer l'application**
```bash
mvn spring-boot:run
```

L'application sera accessible sur `http://localhost:8080`

## Dépendances Principales
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-security
- spring-boot-starter-thymeleaf
- spring-boot-starter-validation
- spring-boot-starter-mail
- mysql-connector-j
- jjwt (JWT)
- lombok

## Sécurité
- Authentification par Spring Security
- JWT pour l'API REST
- Mots de passe hashés (BCrypt)
- Contrôle d'accès basé sur les rôles

## À Venir (Modules suivants)
- Repositories Spring Data
- Services métier
- Controllers MVC (Thymeleaf)
- Controllers REST (API)
- Configuration Spring Security
- Interface utilisateur Bootstrap
- Tests unitaires et d'intégration

## Auteur
Centre de Formation - 2025
