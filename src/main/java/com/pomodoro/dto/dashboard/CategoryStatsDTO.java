package com.pomodoro.dto.dashboard;

import com.pomodoro.entity.Category;

public record CategoryStatsDTO(
        Category category,
        long totalFocusMinutes,
        long sessions,
        double successRate,
        double percentage
) {}

