package com.pomodoro.dto;

import com.pomodoro.entity.Category;
import com.pomodoro.entity.Period;
import com.pomodoro.entity.Session;

import java.time.LocalDate;

public record SessionSummaryDTO(
        String id,
        LocalDate date,
        Period period,
        Category category,
        int completedCycles,
        int targetCycles,
        boolean success,
        int totalFocusMinutes
) {
    public static SessionSummaryDTO fromEntity(Session session) {
        return new SessionSummaryDTO(
                session.getId(),
                session.getDate(),
                session.getPeriod(),
                session.getCategory(),
                session.getCompletedCycles(),
                session.getTargetCycles(),
                session.isSuccess(),
                session.getTotalFocusMinutes()
        );
    }
}

