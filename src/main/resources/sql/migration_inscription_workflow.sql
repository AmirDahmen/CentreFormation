-- ===================================================================
-- Script de migration pour le nouveau workflow d'inscription
-- Date: 2025
-- Description: Ajoute les colonnes nécessaires pour le workflow de validation
-- ===================================================================

USE centre_formation;

-- ===================================================================
-- 1. Modification de la table 'groupes' - ajout de la capacité
-- ===================================================================
ALTER TABLE groupes 
ADD COLUMN capacite INT DEFAULT 30 AFTER code;

-- ===================================================================
-- 2. Modification de la table 'inscriptions' - nouveau workflow
-- ===================================================================

-- Ajouter la colonne groupe_id (nullable car assigné lors de la validation)
ALTER TABLE inscriptions 
ADD COLUMN groupe_id BIGINT NULL AFTER cours_id;

-- Ajouter la contrainte de clé étrangère
ALTER TABLE inscriptions
ADD CONSTRAINT fk_inscription_groupe FOREIGN KEY (groupe_id) REFERENCES groupes(id) ON DELETE SET NULL;

-- Ajouter la colonne date_validation
ALTER TABLE inscriptions 
ADD COLUMN date_validation DATETIME NULL AFTER date_inscription;

-- Modifier la contrainte de statut pour les nouveaux statuts
ALTER TABLE inscriptions 
DROP CHECK chk_inscription_statut;

ALTER TABLE inscriptions 
ADD CONSTRAINT chk_inscription_statut CHECK (statut IN ('EN_ATTENTE', 'VALIDEE', 'REFUSEE', 'ANNULEE'));

-- Mettre à jour les inscriptions existantes (ACTIVE devient VALIDEE)
UPDATE inscriptions SET statut = 'VALIDEE' WHERE statut = 'ACTIVE';
UPDATE inscriptions SET statut = 'ANNULEE' WHERE statut = 'TERMINEE';

-- Modifier le default
ALTER TABLE inscriptions 
ALTER COLUMN statut SET DEFAULT 'EN_ATTENTE';

-- ===================================================================
-- 3. Mettre à jour les capacités des groupes existants
-- ===================================================================
UPDATE groupes SET capacite = 30 WHERE capacite IS NULL;

-- ===================================================================
-- Message de fin
-- ===================================================================
SELECT 'Migration completed successfully!' AS message;
