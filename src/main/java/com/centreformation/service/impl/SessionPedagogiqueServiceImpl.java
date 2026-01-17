package com.centreformation.service.impl;

import com.centreformation.entity.SessionPedagogique;
import com.centreformation.repository.SessionPedagogiqueRepository;
import com.centreformation.service.SessionPedagogiqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionPedagogiqueServiceImpl implements SessionPedagogiqueService {

    private final SessionPedagogiqueRepository sessionRepository;

    @Override
    public List<SessionPedagogique> findAll() {
        return sessionRepository.findAll();
    }

    @Override
    public Page<SessionPedagogique> findAll(Pageable pageable) {
        return sessionRepository.findAll(pageable);
    }

    @Override
    public SessionPedagogique findById(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session pédagogique non trouvée avec l'ID: " + id));
    }

    @Override
    public Page<SessionPedagogique> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll(pageable);
        }
        return sessionRepository.searchSessions(keyword, pageable);
    }

    @Override
    public SessionPedagogique save(SessionPedagogique session) {
        // Validation des dates
        if (session.getDateDebut() != null && session.getDateFin() != null && 
            session.getDateFin().isBefore(session.getDateDebut())) {
            throw new RuntimeException("La date de fin doit être postérieure à la date de début");
        }
        
        return sessionRepository.save(session);
    }

    @Override
    public SessionPedagogique update(Long id, SessionPedagogique session) {
        SessionPedagogique existing = findById(id);
        
        // Validation des dates
        if (session.getDateDebut() != null && session.getDateFin() != null && 
            session.getDateFin().isBefore(session.getDateDebut())) {
            throw new RuntimeException("La date de fin doit être postérieure à la date de début");
        }
        
        existing.setAnneeScolaire(session.getAnneeScolaire());
        existing.setSemestre(session.getSemestre());
        existing.setDateDebut(session.getDateDebut());
        existing.setDateFin(session.getDateFin());
        
        return sessionRepository.save(existing);
    }

    @Override
    public void deleteById(Long id) {
        if (!sessionRepository.existsById(id)) {
            throw new RuntimeException("Session pédagogique non trouvée avec l'ID: " + id);
        }
        sessionRepository.deleteById(id);
    }

    @Override
    public long count() {
        return sessionRepository.count();
    }

    @Override
    public SessionPedagogique findCurrentSession() {
        LocalDate today = LocalDate.now();
        return sessionRepository.findAll().stream()
                .filter(s -> s.getDateDebut() != null && s.getDateFin() != null)
                .filter(s -> !today.isBefore(s.getDateDebut()) && !today.isAfter(s.getDateFin()))
                .findFirst()
                .orElse(null);
    }
}
