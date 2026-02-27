package com.pomodoro.controller;

import com.pomodoro.dto.dashboard.*;
import com.pomodoro.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Resumo geral: total de sessões, taxa de sucesso, streak, médias.
     */
    @GetMapping("/summary")
    public DashboardSummaryDTO getSummary() {
        return dashboardService.getSummary();
    }

    /**
     * Dados de overview por período (gráfico de linha/barra).
     * @param period week, month ou year
     */
    @GetMapping("/overview")
    public OverviewDTO getOverview(@RequestParam(defaultValue = "week") String period) {
        return dashboardService.getOverview(period);
    }

    /**
     * Distribuição por categoria (gráfico de pizza/donut).
     * @param period week, month ou year
     */
    @GetMapping("/by-category")
    public List<CategoryStatsDTO> getByCategory(@RequestParam(defaultValue = "month") String period) {
        return dashboardService.getByCategory(period);
    }

    /**
     * Heatmap de atividade estilo GitHub.
     * @param year ano desejado
     */
    @GetMapping("/heatmap")
    public List<HeatmapEntryDTO> getHeatmap(@RequestParam(defaultValue = "2026") int year) {
        return dashboardService.getHeatmap(year);
    }

    /**
     * Estatísticas por período do dia (manhã, tarde, noite).
     */
    @GetMapping("/by-period")
    public List<PeriodStatsDTO> getByPeriod() {
        return dashboardService.getByPeriod();
    }

    /**
     * Estatísticas por dia da semana.
     */
    @GetMapping("/by-weekday")
    public List<WeekdayStatsDTO> getByWeekday() {
        return dashboardService.getByWeekday();
    }

    /**
     * Metas e progresso no período.
     * @param period week, month ou year
     */
    @GetMapping("/goals")
    public GoalsDTO getGoals(@RequestParam(defaultValue = "week") String period) {
        return dashboardService.getGoals(period);
    }
}

