package com.centreformation.service;

import com.centreformation.entity.SessionPedagogique;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SessionPedagogiqueService {

    List<SessionPedagogique> findAll();

    Page<SessionPedagogique> findAll(Pageable pageable);

    SessionPedagogique findById(Long id);

    Page<SessionPedagogique> search(String keyword, Pageable pageable);

    SessionPedagogique save(SessionPedagogique session);

    SessionPedagogique update(Long id, SessionPedagogique session);

    void deleteById(Long id);

    long count();

    /**
     * Récupère la session pédagogique en cours
     */
    SessionPedagogique findCurrentSession();
}
