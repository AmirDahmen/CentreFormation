-- ===================================================================
-- Script de création de la base de données centre_formation
-- Pour MySQL 8.0+
-- Date: 2025-12-08
-- ===================================================================

-- Création de la base de données
DROP DATABASE IF EXISTS centre_formation;
CREATE DATABASE centre_formation CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE centre_formation;

-- ===================================================================
-- Tables de sécurité
-- ===================================================================

-- Table: roles
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    CONSTRAINT chk_role_nom CHECK (nom IN ('ADMIN', 'FORMATEUR', 'ETUDIANT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: utilisateurs
CREATE TABLE utilisateurs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table de jointure: utilisateurs_roles
CREATE TABLE utilisateurs_roles (
    utilisateur_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (utilisateur_id, role_id),
    CONSTRAINT fk_ur_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- Tables de gestion académique
-- ===================================================================

-- Table: specialites
CREATE TABLE specialites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    INDEX idx_specialite_nom (nom)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: sessions_pedagogiques
CREATE TABLE sessions_pedagogiques (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    annee_scolaire VARCHAR(20) NOT NULL,
    semestre VARCHAR(10),
    date_debut DATE,
    date_fin DATE,
    INDEX idx_session_annee (annee_scolaire),
    CONSTRAINT chk_dates_session CHECK (date_fin IS NULL OR date_debut IS NULL OR date_fin >= date_debut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: groupes
CREATE TABLE groupes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    code VARCHAR(20) UNIQUE,
    capacite INT DEFAULT 30,
    session_pedagogique_id BIGINT,
    specialite_id BIGINT,
    CONSTRAINT fk_groupe_session FOREIGN KEY (session_pedagogique_id) REFERENCES sessions_pedagogiques(id) ON DELETE SET NULL,
    CONSTRAINT fk_groupe_specialite FOREIGN KEY (specialite_id) REFERENCES specialites(id) ON DELETE SET NULL,
    INDEX idx_groupe_nom (nom),
    INDEX idx_groupe_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- Tables des acteurs
-- ===================================================================

-- Table: etudiants
CREATE TABLE etudiants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    matricule VARCHAR(50) NOT NULL UNIQUE,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    date_inscription DATE,
    specialite_id BIGINT,
    utilisateur_id BIGINT UNIQUE,
    CONSTRAINT fk_etudiant_specialite FOREIGN KEY (specialite_id) REFERENCES specialites(id) ON DELETE SET NULL,
    CONSTRAINT fk_etudiant_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id) ON DELETE SET NULL,
    INDEX idx_etudiant_matricule (matricule),
    INDEX idx_etudiant_email (email),
    INDEX idx_etudiant_nom (nom, prenom)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: formateurs
CREATE TABLE formateurs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    telephone VARCHAR(20),
    specialite_id BIGINT,
    utilisateur_id BIGINT UNIQUE,
    CONSTRAINT fk_formateur_specialite FOREIGN KEY (specialite_id) REFERENCES specialites(id) ON DELETE SET NULL,
    CONSTRAINT fk_formateur_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id) ON DELETE SET NULL,
    INDEX idx_formateur_email (email),
    INDEX idx_formateur_nom (nom, prenom)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- Tables pédagogiques
-- ===================================================================

-- Table: cours
CREATE TABLE cours (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    titre VARCHAR(200) NOT NULL,
    description TEXT,
    nombre_heures INT,
    formateur_id BIGINT,
    CONSTRAINT fk_cours_formateur FOREIGN KEY (formateur_id) REFERENCES formateurs(id) ON DELETE SET NULL,
    INDEX idx_cours_code (code),
    INDEX idx_cours_titre (titre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: inscriptions (avec workflow de validation)
CREATE TABLE inscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    date_inscription DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_validation DATETIME NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    etudiant_id BIGINT NOT NULL,
    cours_id BIGINT NOT NULL,
    groupe_id BIGINT NULL,
    CONSTRAINT fk_inscription_etudiant FOREIGN KEY (etudiant_id) REFERENCES etudiants(id) ON DELETE CASCADE,
    CONSTRAINT fk_inscription_cours FOREIGN KEY (cours_id) REFERENCES cours(id) ON DELETE CASCADE,
    CONSTRAINT fk_inscription_groupe FOREIGN KEY (groupe_id) REFERENCES groupes(id) ON DELETE SET NULL,
    CONSTRAINT uk_inscription_etudiant_cours UNIQUE (etudiant_id, cours_id),
    CONSTRAINT chk_inscription_statut CHECK (statut IN ('EN_ATTENTE', 'VALIDEE', 'REFUSEE', 'ANNULEE')),
    INDEX idx_inscription_date (date_inscription),
    INDEX idx_inscription_statut (statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: notes
CREATE TABLE notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    valeur DOUBLE NOT NULL,
    date_saisie DATETIME DEFAULT CURRENT_TIMESTAMP,
    commentaire TEXT,
    etudiant_id BIGINT NOT NULL,
    cours_id BIGINT NOT NULL,
    formateur_id BIGINT,
    CONSTRAINT fk_note_etudiant FOREIGN KEY (etudiant_id) REFERENCES etudiants(id) ON DELETE CASCADE,
    CONSTRAINT fk_note_cours FOREIGN KEY (cours_id) REFERENCES cours(id) ON DELETE CASCADE,
    CONSTRAINT fk_note_formateur FOREIGN KEY (formateur_id) REFERENCES formateurs(id) ON DELETE SET NULL,
    CONSTRAINT uk_note_etudiant_cours UNIQUE (etudiant_id, cours_id),
    CONSTRAINT chk_note_valeur CHECK (valeur >= 0 AND valeur <= 20),
    INDEX idx_note_date (date_saisie)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: seances_cours
CREATE TABLE seances_cours (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL,
    heure_debut TIME NOT NULL,
    heure_fin TIME NOT NULL,
    salle VARCHAR(100),
    contenu TEXT,
    cours_id BIGINT NOT NULL,
    groupe_id BIGINT,
    formateur_id BIGINT,
    CONSTRAINT fk_seance_cours FOREIGN KEY (cours_id) REFERENCES cours(id) ON DELETE CASCADE,
    CONSTRAINT fk_seance_groupe FOREIGN KEY (groupe_id) REFERENCES groupes(id) ON DELETE SET NULL,
    CONSTRAINT fk_seance_formateur FOREIGN KEY (formateur_id) REFERENCES formateurs(id) ON DELETE SET NULL,
    CONSTRAINT chk_seance_heures CHECK (heure_fin > heure_debut),
    INDEX idx_seance_date (date),
    INDEX idx_seance_formateur_date (formateur_id, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- Tables de jointure ManyToMany
-- ===================================================================

-- Table de jointure: etudiants_groupes
CREATE TABLE etudiants_groupes (
    etudiant_id BIGINT NOT NULL,
    groupe_id BIGINT NOT NULL,
    PRIMARY KEY (etudiant_id, groupe_id),
    CONSTRAINT fk_eg_etudiant FOREIGN KEY (etudiant_id) REFERENCES etudiants(id) ON DELETE CASCADE,
    CONSTRAINT fk_eg_groupe FOREIGN KEY (groupe_id) REFERENCES groupes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table de jointure: cours_groupes
CREATE TABLE cours_groupes (
    cours_id BIGINT NOT NULL,
    groupe_id BIGINT NOT NULL,
    PRIMARY KEY (cours_id, groupe_id),
    CONSTRAINT fk_cg_cours FOREIGN KEY (cours_id) REFERENCES cours(id) ON DELETE CASCADE,
    CONSTRAINT fk_cg_groupe FOREIGN KEY (groupe_id) REFERENCES groupes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================
-- Données de test initiales
-- ===================================================================

-- Insertion des rôles par défaut
INSERT INTO roles (nom) VALUES 
    ('ADMIN'),
    ('FORMATEUR'),
    ('ETUDIANT');

-- Insertion de spécialités
INSERT INTO specialites (nom, description) VALUES 
    ('Informatique', 'Développement et systèmes informatiques'),
    ('Réseaux', 'Administration réseaux et télécommunications'),
    ('Intelligence Artificielle', 'Machine Learning et Data Science'),
    ('Cybersécurité', 'Sécurité des systèmes d\'information');

-- Insertion d'une session pédagogique
INSERT INTO sessions_pedagogiques (annee_scolaire, semestre, date_debut, date_fin) VALUES 
    ('2025-2026', 'S1', '2025-09-01', '2026-01-31'),
    ('2025-2026', 'S2', '2026-02-01', '2026-06-30');

-- ===================================================================
-- Fin du script
-- ===================================================================
