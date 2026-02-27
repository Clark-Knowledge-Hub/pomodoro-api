package com.pomodoro.dto.dashboard;

public record GoalsDTO(
        String period,
        long totalFocusMinutes,
        long totalSessions,
        long successfulSessions,
        double successRate,
        double averageFocusMinutesPerDay,
        int activeDays,
        int totalDaysInPeriod
) {}

