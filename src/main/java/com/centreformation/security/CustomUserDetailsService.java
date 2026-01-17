package com.centreformation.security;

import com.centreformation.entity.Utilisateur;
import com.centreformation.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouvé avec le nom: " + username));

        return new User(
                utilisateur.getUsername(),
                utilisateur.getPassword(),
                utilisateur.getActif(),
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                utilisateur.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getNom()))
                        .collect(Collectors.toList())
        );
    }
}
