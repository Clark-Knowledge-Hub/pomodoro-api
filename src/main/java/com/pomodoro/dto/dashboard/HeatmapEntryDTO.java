package com.pomodoro.dto.dashboard;

import java.time.LocalDate;

public record HeatmapEntryDTO(
        LocalDate date,
        long totalMinutes,
        long sessions
) {}

