package com.pomodoro.service;

import com.pomodoro.dto.dashboard.*;
import com.pomodoro.entity.Category;
import com.pomodoro.entity.Period;
import com.pomodoro.entity.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MongoTemplate mongoTemplate;

    // ==================== 1. SUMMARY ====================

    public DashboardSummaryDTO getSummary() {
        List<Session> sessions = mongoTemplate.findAll(Session.class);

        if (sessions.isEmpty()) {
            return new DashboardSummaryDTO(0, 0, 0.0, 0, 0.0, 0.0, 0);
        }

        long total = sessions.size();
        long successful = sessions.stream().filter(Session::isSuccess).count();
        double successRate = round((double) successful / total * 100);
        long totalFocusMinutes = sessions.stream().mapToLong(Session::getTotalFocusMinutes).sum();
        double avgCycles = round(sessions.stream().mapToInt(Session::getCompletedCycles).average().orElse(0));
        double avgFocus = round(sessions.stream().mapToInt(Session::getTotalFocusMinutes).average().orElse(0));
        int streak = calculateStreak(sessions);

        return new DashboardSummaryDTO(total, successful, successRate, totalFocusMinutes, avgCycles, avgFocus, streak);
    }

    // ==================== 2. OVERVIEW ====================

    public OverviewDTO getOverview(String period, LocalDate customStart, LocalDate customEnd) {
        LocalDate now = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate;
        String groupBy;

        if (customStart != null && customEnd != null) {
            startDate = customStart;
            endDate = customEnd;
            long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
            groupBy = daysBetween > 60 ? "week" : "day";
        } else {
            endDate = now;
            switch (period.toLowerCase()) {
                case "week" -> {
                    startDate = now.minusDays(6);
                    groupBy = "day";
                }
                case "month" -> {
                    startDate = now.withDayOfMonth(1);
                    groupBy = "day";
                }
                case "year" -> {
                    startDate = now.withDayOfYear(1);
                    groupBy = "week";
                }
                case "all" -> {
                    List<Session> allSessions = mongoTemplate.findAll(Session.class);
                    if (allSessions.isEmpty()) {
                        return new OverviewDTO("all", 0, 0, 0.0, List.of());
                    }
                    LocalDate earliest = allSessions.stream()
                            .map(Session::getDate).min(LocalDate::compareTo).orElse(now);
                    startDate = earliest;
                    long days = ChronoUnit.DAYS.between(startDate, now);
                    groupBy = days > 60 ? "week" : "day";
                }
                default -> throw new IllegalArgumentException(
                        "Invalid period: " + period + ". Use: week, month, year, all");
            }
        }

        List<Session> sessions = findByDateRange(startDate, endDate);

        List<OverviewEntryDTO> data;
        if ("day".equals(groupBy)) {
            data = groupByDay(sessions, startDate, endDate);
        } else {
            data = groupByWeek(sessions, startDate, endDate);
        }

        long totalFocus = sessions.stream().mapToLong(Session::getTotalFocusMinutes).sum();
        long totalSessions = sessions.size();
        double successRate = totalSessions > 0
                ? round((double) sessions.stream().filter(Session::isSuccess).count() / totalSessions * 100)
                : 0.0;

        return new OverviewDTO(period, totalFocus, totalSessions, successRate, data);
    }

    // ==================== 3. BY CATEGORY ====================

    public List<CategoryStatsDTO> getByCategory(String period, LocalDate customStart, LocalDate customEnd) {
        List<Session> sessions;

        if (customStart != null && customEnd != null) {
            sessions = findByDateRange(customStart, customEnd);
        } else if ("all".equalsIgnoreCase(period)) {
            sessions = mongoTemplate.findAll(Session.class);
        } else {
            LocalDate now = LocalDate.now();
            LocalDate startDate = resolveStartDate(period, now);
            sessions = findByDateRange(startDate, now);
        }

        long grandTotalMinutes = sessions.stream().mapToLong(Session::getTotalFocusMinutes).sum();

        Map<Category, List<Session>> grouped = sessions.stream()
                .collect(Collectors.groupingBy(Session::getCategory));

        return Arrays.stream(Category.values())
                .filter(grouped::containsKey)
                .map(category -> {
                    List<Session> catSessions = grouped.get(category);
                    long focusMinutes = catSessions.stream().mapToLong(Session::getTotalFocusMinutes).sum();
                    long count = catSessions.size();
                    long success = catSessions.stream().filter(Session::isSuccess).count();
                    double successRate = count > 0 ? round((double) success / count * 100) : 0.0;
                    double percentage = grandTotalMinutes > 0 ? round((double) focusMinutes / grandTotalMinutes * 100) : 0.0;
                    return new CategoryStatsDTO(category, focusMinutes, count, successRate, percentage);
                })
                .sorted(Comparator.comparingLong(CategoryStatsDTO::totalFocusMinutes).reversed())
                .toList();
    }

    // ==================== 4. HEATMAP ====================

    public List<HeatmapEntryDTO> getHeatmap(int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<Session> sessions = findByDateRange(startDate, endDate);

        Map<LocalDate, List<Session>> grouped = sessions.stream()
                .collect(Collectors.groupingBy(Session::getDate));

        List<HeatmapEntryDTO> result = new ArrayList<>();
        LocalDate current = startDate;
        LocalDate today = LocalDate.now();
        LocalDate limit = endDate.isAfter(today) ? today : endDate;

        while (!current.isAfter(limit)) {
            List<Session> daySessions = grouped.getOrDefault(current, List.of());
            long totalMinutes = daySessions.stream().mapToLong(Session::getTotalFocusMinutes).sum();
            result.add(new HeatmapEntryDTO(current, totalMinutes, daySessions.size()));
            current = current.plusDays(1);
        }

        return result;
    }

    // ==================== 5. BY PERIOD (MORNING/AFTERNOON/NIGHT) ====================

    public List<PeriodStatsDTO> getByPeriod() {
        List<Session> sessions = mongoTemplate.findAll(Session.class);

        Map<Period, List<Session>> grouped = sessions.stream()
                .collect(Collectors.groupingBy(Session::getPeriod));

        return Arrays.stream(Period.values())
                .map(period -> {
                    List<Session> periodSessions = grouped.getOrDefault(period, List.of());
                    long focusMinutes = periodSessions.stream().mapToLong(Session::getTotalFocusMinutes).sum();
                    long count = periodSessions.size();
                    long success = periodSessions.stream().filter(Session::isSuccess).count();
                    double successRate = count > 0 ? round((double) success / count * 100) : 0.0;
                    double avgFocus = count > 0 ? round((double) focusMinutes / count) : 0.0;
                    return new PeriodStatsDTO(period, focusMinutes, count, successRate, avgFocus);
                })
                .toList();
    }

    // ==================== 6. BY WEEKDAY ====================

    public List<WeekdayStatsDTO> getByWeekday() {
        List<Session> sessions = mongoTemplate.findAll(Session.class);

        Map<DayOfWeek, List<Session>> grouped = sessions.stream()
                .collect(Collectors.groupingBy(Session::getDayOfWeek));

        return Arrays.stream(DayOfWeek.values())
                .map(day -> {
                    List<Session> daySessions = grouped.getOrDefault(day, List.of());
                    long focusMinutes = daySessions.stream().mapToLong(Session::getTotalFocusMinutes).sum();
                    long count = daySessions.size();
                    long success = daySessions.stream().filter(Session::isSuccess).count();
                    double successRate = count > 0 ? round((double) success / count * 100) : 0.0;
                    double avgFocus = count > 0 ? round((double) focusMinutes / count) : 0.0;
                    return new WeekdayStatsDTO(day, focusMinutes, count, successRate, avgFocus);
                })
                .toList();
    }

    // ==================== 7. GOALS ====================

    public GoalsDTO getGoals(String period, LocalDate customStart, LocalDate customEnd) {
        LocalDate now = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate;

        if (customStart != null && customEnd != null) {
            startDate = customStart;
            endDate = customEnd;
        } else if ("all".equalsIgnoreCase(period)) {
            List<Session> allSessions = mongoTemplate.findAll(Session.class);
            if (allSessions.isEmpty()) {
                return new GoalsDTO("all", 0, 0, 0, 0.0, 0.0, 0, 0);
            }
            startDate = allSessions.stream()
                    .map(Session::getDate).min(LocalDate::compareTo).orElse(now);
            endDate = now;
        } else {
            startDate = resolveStartDate(period, now);
            endDate = now;
        }

        List<Session> sessions = findByDateRange(startDate, endDate);

        long totalFocus = sessions.stream().mapToLong(Session::getTotalFocusMinutes).sum();
        long totalSessions = sessions.size();
        long successfulSessions = sessions.stream().filter(Session::isSuccess).count();
        double successRate = totalSessions > 0
                ? round((double) successfulSessions / totalSessions * 100) : 0.0;

        Set<LocalDate> activeDates = sessions.stream()
                .map(Session::getDate)
                .collect(Collectors.toSet());

        int activeDays = activeDates.size();
        int totalDaysInPeriod = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        double avgFocusPerDay = activeDays > 0 ? round((double) totalFocus / totalDaysInPeriod) : 0.0;

        return new GoalsDTO(period, totalFocus, totalSessions, successfulSessions,
                successRate, avgFocusPerDay, activeDays, totalDaysInPeriod);
    }

    // ==================== HELPERS ====================

    private List<Session> findByDateRange(LocalDate start, LocalDate end) {
        Query query = new Query(Criteria.where("date").gte(start).lte(end));
        return mongoTemplate.find(query, Session.class);
    }

    private LocalDate resolveStartDate(String period, LocalDate now) {
        return switch (period.toLowerCase()) {
            case "week" -> now.minusDays(6);
            case "month" -> now.withDayOfMonth(1);
            case "year" -> now.withDayOfYear(1);
            default -> throw new IllegalArgumentException("Invalid period: " + period + ". Use: week, month, year");
        };
    }

    private List<OverviewEntryDTO> groupByDay(List<Session> sessions, LocalDate start, LocalDate end) {
        Map<LocalDate, List<Session>> grouped = sessions.stream()
                .collect(Collectors.groupingBy(Session::getDate));

        List<OverviewEntryDTO> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        LocalDate current = start;

        while (!current.isAfter(end)) {
            List<Session> daySessions = grouped.getOrDefault(current, List.of());
            long focusMinutes = daySessions.stream().mapToLong(Session::getTotalFocusMinutes).sum();
            long count = daySessions.size();
            long success = daySessions.stream().filter(Session::isSuccess).count();
            double successRate = count > 0 ? round((double) success / count * 100) : 0.0;
            result.add(new OverviewEntryDTO(current.format(fmt), focusMinutes, count, successRate));
            current = current.plusDays(1);
        }

        return result;
    }

    private List<OverviewEntryDTO> groupByWeek(List<Session> sessions, LocalDate start, LocalDate end) {
        // Use composite key "year-week" to handle year boundaries correctly
        Map<String, List<Session>> grouped = sessions.stream()
                .collect(Collectors.groupingBy(s -> {
                    int weekYear = s.getDate().get(IsoFields.WEEK_BASED_YEAR);
                    int week = s.getDate().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                    return weekYear + "-" + week;
                }));

        List<OverviewEntryDTO> result = new ArrayList<>();
        LocalDate current = start;
        Set<String> processedWeeks = new LinkedHashSet<>();

        while (!current.isAfter(end)) {
            int weekYear = current.get(IsoFields.WEEK_BASED_YEAR);
            int week = current.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            String key = weekYear + "-" + week;

            if (processedWeeks.add(key)) {
                List<Session> weekSessions = grouped.getOrDefault(key, List.of());
                long focusMinutes = weekSessions.stream().mapToLong(Session::getTotalFocusMinutes).sum();
                long count = weekSessions.size();
                long success = weekSessions.stream().filter(Session::isSuccess).count();
                double successRate = count > 0 ? round((double) success / count * 100) : 0.0;
                result.add(new OverviewEntryDTO("Semana " + week, focusMinutes, count, successRate));
            }
            current = current.plusDays(1);
        }

        return result;
    }

    private int calculateStreak(List<Session> sessions) {
        Set<LocalDate> studyDays = sessions.stream()
                .map(Session::getDate)
                .collect(Collectors.toCollection(TreeSet::new));

        if (studyDays.isEmpty()) return 0;

        LocalDate today = LocalDate.now();
        int streak = 0;
        LocalDate current = today;

        // Se hoje não estudou, começa de ontem
        if (!studyDays.contains(today)) {
            current = today.minusDays(1);
            if (!studyDays.contains(current)) return 0;
        }

        while (studyDays.contains(current)) {
            streak++;
            current = current.minusDays(1);
        }

        return streak;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

