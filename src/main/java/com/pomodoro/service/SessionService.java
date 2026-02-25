package com.pomodoro.service;

import com.pomodoro.dto.SessionDetailDTO;
import com.pomodoro.dto.SessionFilter;
import com.pomodoro.dto.SessionSummaryDTO;
import com.pomodoro.entity.Session;
import com.pomodoro.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final MongoTemplate mongoTemplate;

    public Session save(Session session) {
        return sessionRepository.save(session);
    }

    public Page<SessionSummaryDTO> findAll(SessionFilter filter, Pageable pageable) {
        Query query = buildQuery(filter);

        long total = mongoTemplate.count(query, Session.class);

        query.with(pageable);

        List<SessionSummaryDTO> content = mongoTemplate
                .find(query, Session.class)
                .stream()
                .map(SessionSummaryDTO::fromEntity)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    public SessionDetailDTO findById(String id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found: " + id));
        return SessionDetailDTO.fromEntity(session);
    }

    private Query buildQuery(SessionFilter filter) {
        Query query = new Query();

        if (filter == null) return query;

        Criteria criteria = new Criteria();

        if (filter.startDate() != null && filter.endDate() != null) {
            criteria.and("date").gte(filter.startDate()).lte(filter.endDate());
        } else if (filter.startDate() != null) {
            criteria.and("date").gte(filter.startDate());
        } else if (filter.endDate() != null) {
            criteria.and("date").lte(filter.endDate());
        }

        if (filter.period() != null) {
            criteria.and("period").is(filter.period());
        }

        if (filter.category() != null) {
            criteria.and("category").is(filter.category());
        }

        if (filter.success() != null) {
            criteria.and("success").is(filter.success());
        }

        query.addCriteria(criteria);
        return query;
    }
}
