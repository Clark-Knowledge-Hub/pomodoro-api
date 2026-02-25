package com.pomodoro.dto;

import com.pomodoro.entity.Category;
import com.pomodoro.entity.Period;
import com.pomodoro.entity.Session;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public record SessionDetailDTO(
        String id,
        LocalDate date,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        Period period,
        Category category,
        int targetCycles,
        int completedCycles,
        boolean success,
        int totalFocusMinutes,
        int totalBreakMinutes
) {
    public static SessionDetailDTO fromEntity(Session session) {
        return new SessionDetailDTO(
                session.getId(),
                session.getDate(),
                session.getDayOfWeek(),
                session.getStartTime(),
                session.getPeriod(),
                session.getCategory(),
                session.getTargetCycles(),
                session.getCompletedCycles(),
                session.isSuccess(),
                session.getTotalFocusMinutes(),
                session.getTotalBreakMinutes()
        );
    }
}

