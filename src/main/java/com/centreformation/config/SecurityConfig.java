package com.centreformation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Ressources publiques
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/login", "/register", "/error").permitAll()
                
                // Accès admin uniquement
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // Accès formateur
                .requestMatchers("/formateur/**").hasAnyRole("ADMIN", "FORMATEUR")
                
                // Accès étudiant
                .requestMatchers("/etudiant/**").hasAnyRole("ADMIN", "FORMATEUR", "ETUDIANT")
                
                // Tout le reste nécessite une authentification
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/admin", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/error/403")
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        // Utilisateurs en mémoire pour les tests (à remplacer par la base de données)
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder.encode("admin123"))
            .roles("ADMIN")
            .build();

        UserDetails formateur = User.builder()
            .username("formateur")
            .password(passwordEncoder.encode("form123"))
            .roles("FORMATEUR")
            .build();

        UserDetails etudiant = User.builder()
            .username("etudiant")
            .password(passwordEncoder.encode("etud123"))
            .roles("ETUDIANT")
            .build();

        return new InMemoryUserDetailsManager(admin, formateur, etudiant);
    }
}
