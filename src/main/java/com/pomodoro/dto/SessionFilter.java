package com.pomodoro.dto;

import com.pomodoro.entity.Category;
import com.pomodoro.entity.Period;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record SessionFilter(

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,

        Period period,
        Category category,
        Boolean success
) {}

