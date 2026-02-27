package com.pomodoro.dto.dashboard;

import com.pomodoro.entity.Period;

public record PeriodStatsDTO(
        Period period,
        long totalFocusMinutes,
        long sessions,
        double successRate,
        double averageFocusMinutes
) {}

