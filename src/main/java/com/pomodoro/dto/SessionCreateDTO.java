package com.pomodoro.dto;

import com.pomodoro.entity.Category;
import com.pomodoro.entity.Period;
import com.pomodoro.entity.Session;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public record SessionCreateDTO(

        @NotNull(message = "date is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date,

        @NotNull(message = "dayOfWeek is required")
        DayOfWeek dayOfWeek,

        @NotNull(message = "startTime is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
        LocalTime startTime,

        @Min(value = 1, message = "targetCycles must be at least 1")
        int targetCycles,

        @Min(value = 0, message = "completedCycles must be at least 0")
        int completedCycles,

        @Min(value = 0, message = "totalFocusMinutes must be at least 0")
        int totalFocusMinutes,

        @Min(value = 0, message = "totalBreakMinutes must be at least 0")
        int totalBreakMinutes,

        @NotNull(message = "success is required")
        Boolean success,

        @NotNull(message = "period is required")
        Period period,

        @NotNull(message = "category is required")
        Category category
) {
    public Session toEntity() {
        return Session.builder()
                .date(date)
                .dayOfWeek(dayOfWeek)
                .startTime(startTime)
                .targetCycles(targetCycles)
                .completedCycles(completedCycles)
                .totalFocusMinutes(totalFocusMinutes)
                .totalBreakMinutes(totalBreakMinutes)
                .success(success)
                .period(period)
                .category(category)
                .build();
    }
}

