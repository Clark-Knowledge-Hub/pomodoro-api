package com.pomodoro.dto.dashboard;

public record OverviewEntryDTO(
        String label,
        long totalFocusMinutes,
        long sessions,
        double successRate
) {}

