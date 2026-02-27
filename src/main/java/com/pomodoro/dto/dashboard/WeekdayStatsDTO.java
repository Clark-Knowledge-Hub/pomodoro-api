package com.pomodoro.dto.dashboard;

import java.time.DayOfWeek;

public record WeekdayStatsDTO(
        DayOfWeek dayOfWeek,
        long totalFocusMinutes,
        long sessions,
        double successRate,
        double averageFocusMinutes
) {}

