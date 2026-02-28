package com.pomodoro.controller;

import com.pomodoro.dto.dashboard.*;
import com.pomodoro.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
     * @param period week, month, year ou all
     * @param startDate data início customizada (opcional, formato yyyy-MM-dd)
     * @param endDate data fim customizada (opcional, formato yyyy-MM-dd)
     */
    @GetMapping("/overview")
    public OverviewDTO getOverview(
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return dashboardService.getOverview(period, startDate, endDate);
    }

    /**
     * Distribuição por categoria (gráfico de pizza/donut).
     * @param period week, month, year ou all
     * @param startDate data início customizada (opcional, formato yyyy-MM-dd)
     * @param endDate data fim customizada (opcional, formato yyyy-MM-dd)
     */
    @GetMapping("/by-category")
    public List<CategoryStatsDTO> getByCategory(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return dashboardService.getByCategory(period, startDate, endDate);
    }

    /**
     * Heatmap de atividade estilo GitHub.
     * @param year ano desejado
     */
    @GetMapping("/heatmap")
    public List<HeatmapEntryDTO> getHeatmap(@RequestParam(required = false) Integer year) {
        int resolvedYear = (year != null) ? year : LocalDate.now().getYear();
        return dashboardService.getHeatmap(resolvedYear);
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
     * @param period week, month, year ou all
     * @param startDate data início customizada (opcional, formato yyyy-MM-dd)
     * @param endDate data fim customizada (opcional, formato yyyy-MM-dd)
     */
    @GetMapping("/goals")
    public GoalsDTO getGoals(
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return dashboardService.getGoals(period, startDate, endDate);
    }
}

