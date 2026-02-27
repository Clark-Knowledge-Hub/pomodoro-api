package com.pomodoro.dto.dashboard;

public record DashboardSummaryDTO(
        long totalSessions,
        long successfulSessions,
        double successRate,
        long totalFocusMinutes,
        double averageCompletedCycles,
        double averageFocusMinutes,
        int currentStreak
) {}

