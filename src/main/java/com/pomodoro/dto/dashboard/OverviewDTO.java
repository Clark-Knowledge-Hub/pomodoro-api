package com.pomodoro.dto.dashboard;

import java.util.List;

public record OverviewDTO(
        String period,
        long totalFocusMinutes,
        long totalSessions,
        double successRate,
        List<OverviewEntryDTO> data
) {}

