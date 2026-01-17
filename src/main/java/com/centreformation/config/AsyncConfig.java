package com.centreformation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration pour activer l'exécution asynchrone.
 * Permet d'envoyer les emails en arrière-plan sans bloquer les requêtes.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // La configuration par défaut de Spring est utilisée
    // Les méthodes annotées avec @Async s'exécuteront dans un thread pool séparé
}
