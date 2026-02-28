package com.pomodoro.service;

import com.pomodoro.dto.dashboard.*;
import com.pomodoro.entity.Category;
import com.pomodoro.entity.Period;
import com.pomodoro.entity.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private DashboardService dashboardService;

    // ==================== HELPERS ====================

    private Session buildSession(LocalDate date, Category category, Period period,
                                  int focusMinutes, int completedCycles, int targetCycles, boolean success) {
        return Session.builder()
                .date(date)
                .dayOfWeek(date.getDayOfWeek())
                .startTime(LocalTime.of(10, 0))
                .category(category)
                .period(period)
                .totalFocusMinutes(focusMinutes)
                .totalBreakMinutes(5)
                .completedCycles(completedCycles)
                .targetCycles(targetCycles)
                .success(success)
                .build();
    }

    private List<Session> sampleSessions() {
        LocalDate today = LocalDate.now();
        return List.of(
                buildSession(today, Category.TECHNOLOGY, Period.MORNING, 50, 4, 4, true),
                buildSession(today.minusDays(1), Category.MATH, Period.AFTERNOON, 25, 2, 4, false),
                buildSession(today.minusDays(1), Category.TECHNOLOGY, Period.NIGHT, 75, 6, 6, true),
                buildSession(today.minusDays(2), Category.ENGLISH, Period.MORNING, 30, 3, 4, false)
        );
    }

    // ==================== SUMMARY ====================

    @Nested
    @DisplayName("GET /dashboard/summary")
    class Summary {

        @Test
        @DisplayName("Should return correct summary with sessions")
        void shouldReturnSummary() {
            when(mongoTemplate.findAll(Session.class)).thenReturn(sampleSessions());

            DashboardSummaryDTO result = dashboardService.getSummary();

            assertThat(result.totalSessions()).isEqualTo(4);
            assertThat(result.successfulSessions()).isEqualTo(2);
            assertThat(result.successRate()).isEqualTo(50.0);
            assertThat(result.totalFocusMinutes()).isEqualTo(180); // 50+25+75+30
            assertThat(result.averageCompletedCycles()).isEqualTo(3.75); // (4+2+6+3)/4
            assertThat(result.averageFocusMinutes()).isEqualTo(45.0); // 180/4
            assertThat(result.currentStreak()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should return empty summary when no sessions")
        void shouldReturnEmptySummary() {
            when(mongoTemplate.findAll(Session.class)).thenReturn(List.of());

            DashboardSummaryDTO result = dashboardService.getSummary();

            assertThat(result.totalSessions()).isZero();
            assertThat(result.successRate()).isZero();
            assertThat(result.currentStreak()).isZero();
        }
    }

    // ==================== OVERVIEW ====================

    @Nested
    @DisplayName("GET /dashboard/overview")
    class Overview {

        @Test
        @DisplayName("Should return weekly overview")
        void shouldReturnWeeklyOverview() {
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(sampleSessions());

            OverviewDTO result = dashboardService.getOverview("week", null, null);

            assertThat(result.period()).isEqualTo("week");
            assertThat(result.totalSessions()).isEqualTo(4);
            assertThat(result.totalFocusMinutes()).isEqualTo(180);
            assertThat(result.data()).isNotEmpty();
            assertThat(result.data()).hasSize(7); // 7 days in a week
        }

        @Test
        @DisplayName("Should return monthly overview")
        void shouldReturnMonthlyOverview() {
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(sampleSessions());

            OverviewDTO result = dashboardService.getOverview("month", null, null);

            assertThat(result.period()).isEqualTo("month");
            assertThat(result.data()).isNotEmpty();
        }

        @Test
        @DisplayName("Should return yearly overview")
        void shouldReturnYearlyOverview() {
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(sampleSessions());

            OverviewDTO result = dashboardService.getOverview("year", null, null);

            assertThat(result.period()).isEqualTo("year");
            assertThat(result.data()).isNotEmpty();
        }

        @Test
        @DisplayName("Should return all-time overview")
        void shouldReturnAllTimeOverview() {
            when(mongoTemplate.findAll(Session.class)).thenReturn(sampleSessions());
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(sampleSessions());

            OverviewDTO result = dashboardService.getOverview("all", null, null);

            assertThat(result.period()).isEqualTo("all");
            assertThat(result.totalSessions()).isEqualTo(4);
            assertThat(result.totalFocusMinutes()).isEqualTo(180);
        }

        @Test
        @DisplayName("Should return overview with custom date range")
        void shouldReturnCustomDateRange() {
            LocalDate start = LocalDate.of(2025, 7, 1);
            LocalDate end = LocalDate.of(2025, 7, 31);
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(sampleSessions());

            OverviewDTO result = dashboardService.getOverview("week", start, end);

            assertThat(result.totalSessions()).isEqualTo(4);
            assertThat(result.data()).hasSize(31); // 31 days in July
        }

        @Test
        @DisplayName("Should throw exception for invalid period")
        void shouldThrowForInvalidPeriod() {
            assertThatThrownBy(() -> dashboardService.getOverview("invalid", null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid period");
        }
    }

    // ==================== BY CATEGORY ====================

    @Nested
    @DisplayName("GET /dashboard/by-category")
    class ByCategory {

        @Test
        @DisplayName("Should return category stats grouped correctly")
        void shouldReturnCategoryStats() {
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(sampleSessions());

            List<CategoryStatsDTO> result = dashboardService.getByCategory("month", null, null);

            assertThat(result).isNotEmpty();
            assertThat(result).hasSizeLessThanOrEqualTo(Category.values().length);

            // TECHNOLOGY should be first (most minutes: 50+75=125)
            assertThat(result.get(0).category()).isEqualTo(Category.TECHNOLOGY);
            assertThat(result.get(0).totalFocusMinutes()).isEqualTo(125);
            assertThat(result.get(0).sessions()).isEqualTo(2);
            assertThat(result.get(0).successRate()).isEqualTo(100.0); // both succeeded

            // Check percentages sum to ~100
            double totalPercentage = result.stream().mapToDouble(CategoryStatsDTO::percentage).sum();
            assertThat(totalPercentage).isCloseTo(100.0, within(0.1));
        }

        @Test
        @DisplayName("Should return empty list when no sessions")
        void shouldReturnEmptyWhenNoSessions() {
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(List.of());

            List<CategoryStatsDTO> result = dashboardService.getByCategory("week", null, null);

            assertThat(result).isEmpty();
        }
    }

    // ==================== HEATMAP ====================

    @Nested
    @DisplayName("GET /dashboard/heatmap")
    class Heatmap {

        @Test
        @DisplayName("Should return heatmap entries for a year")
        void shouldReturnHeatmap() {
            int year = LocalDate.now().getYear();
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(sampleSessions());

            List<HeatmapEntryDTO> result = dashboardService.getHeatmap(year);

            assertThat(result).isNotEmpty();
            // Should have entries from Jan 1 to today
            assertThat(result.get(0).date()).isEqualTo(LocalDate.of(year, 1, 1));
        }

        @Test
        @DisplayName("Should return zero values for days without sessions")
        void shouldReturnZeroForEmptyDays() {
            int year = LocalDate.now().getYear();
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(List.of());

            List<HeatmapEntryDTO> result = dashboardService.getHeatmap(year);

            assertThat(result).isNotEmpty();
            assertThat(result).allSatisfy(entry -> {
                assertThat(entry.totalMinutes()).isZero();
                assertThat(entry.sessions()).isZero();
            });
        }
    }

    // ==================== BY PERIOD ====================

    @Nested
    @DisplayName("GET /dashboard/by-period")
    class ByPeriod {

        @Test
        @DisplayName("Should return stats for all 3 periods")
        void shouldReturnAllPeriods() {
            when(mongoTemplate.findAll(Session.class)).thenReturn(sampleSessions());

            List<PeriodStatsDTO> result = dashboardService.getByPeriod();

            assertThat(result).hasSize(3); // MORNING, AFTERNOON, NIGHT

            PeriodStatsDTO morning = result.stream()
                    .filter(p -> p.period() == Period.MORNING).findFirst().orElseThrow();
            assertThat(morning.sessions()).isEqualTo(2); // 2 morning sessions
            assertThat(morning.totalFocusMinutes()).isEqualTo(80); // 50+30
        }

        @Test
        @DisplayName("Should return zero stats for periods without sessions")
        void shouldReturnZeroForEmptyPeriods() {
            List<Session> morningOnly = List.of(
                    buildSession(LocalDate.now(), Category.TECHNOLOGY, Period.MORNING, 50, 4, 4, true)
            );
            when(mongoTemplate.findAll(Session.class)).thenReturn(morningOnly);

            List<PeriodStatsDTO> result = dashboardService.getByPeriod();

            PeriodStatsDTO afternoon = result.stream()
                    .filter(p -> p.period() == Period.AFTERNOON).findFirst().orElseThrow();
            assertThat(afternoon.sessions()).isZero();
            assertThat(afternoon.totalFocusMinutes()).isZero();
        }
    }

    // ==================== BY WEEKDAY ====================

    @Nested
    @DisplayName("GET /dashboard/by-weekday")
    class ByWeekday {

        @Test
        @DisplayName("Should return stats for all 7 weekdays")
        void shouldReturnAllWeekdays() {
            when(mongoTemplate.findAll(Session.class)).thenReturn(sampleSessions());

            List<WeekdayStatsDTO> result = dashboardService.getByWeekday();

            assertThat(result).hasSize(7);
            // All DayOfWeek values should be present
            assertThat(result.stream().map(WeekdayStatsDTO::dayOfWeek))
                    .containsExactly(DayOfWeek.values());
        }

        @Test
        @DisplayName("Should calculate correct stats per weekday")
        void shouldCalculateCorrectStats() {
            LocalDate today = LocalDate.now();
            when(mongoTemplate.findAll(Session.class)).thenReturn(sampleSessions());

            List<WeekdayStatsDTO> result = dashboardService.getByWeekday();

            // Today's weekday should have at least one session
            WeekdayStatsDTO todayStats = result.stream()
                    .filter(w -> w.dayOfWeek() == today.getDayOfWeek()).findFirst().orElseThrow();
            assertThat(todayStats.sessions()).isGreaterThanOrEqualTo(1);
        }
    }

    // ==================== GOALS ====================

    @Nested
    @DisplayName("GET /dashboard/goals")
    class Goals {

        @Test
        @DisplayName("Should return goals for week")
        void shouldReturnWeeklyGoals() {
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(sampleSessions());

            GoalsDTO result = dashboardService.getGoals("week", null, null);

            assertThat(result.period()).isEqualTo("week");
            assertThat(result.totalSessions()).isEqualTo(4);
            assertThat(result.successfulSessions()).isEqualTo(2);
            assertThat(result.successRate()).isEqualTo(50.0);
            assertThat(result.totalFocusMinutes()).isEqualTo(180);
            assertThat(result.totalDaysInPeriod()).isEqualTo(7);
            assertThat(result.activeDays()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should return goals for month")
        void shouldReturnMonthlyGoals() {
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(sampleSessions());

            GoalsDTO result = dashboardService.getGoals("month", null, null);

            assertThat(result.period()).isEqualTo("month");
            assertThat(result.totalFocusMinutes()).isEqualTo(180);
        }

        @Test
        @DisplayName("Should handle no sessions")
        void shouldHandleNoSessions() {
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(List.of());

            GoalsDTO result = dashboardService.getGoals("week", null, null);

            assertThat(result.totalSessions()).isZero();
            assertThat(result.activeDays()).isZero();
            assertThat(result.averageFocusMinutesPerDay()).isZero();
        }

        @Test
        @DisplayName("Should throw for invalid period")
        void shouldThrowForInvalidPeriod() {
            assertThatThrownBy(() -> dashboardService.getGoals("invalid", null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ==================== STREAK ====================

    @Nested
    @DisplayName("Streak calculation")
    class Streak {

        @Test
        @DisplayName("Should calculate streak of consecutive days")
        void shouldCalculateStreak() {
            LocalDate today = LocalDate.now();
            List<Session> consecutive = List.of(
                    buildSession(today, Category.TECHNOLOGY, Period.MORNING, 50, 4, 4, true),
                    buildSession(today.minusDays(1), Category.MATH, Period.MORNING, 25, 2, 4, false),
                    buildSession(today.minusDays(2), Category.ENGLISH, Period.MORNING, 30, 3, 4, false)
            );
            when(mongoTemplate.findAll(Session.class)).thenReturn(consecutive);

            DashboardSummaryDTO result = dashboardService.getSummary();

            assertThat(result.currentStreak()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return 0 streak when no recent sessions")
        void shouldReturnZeroStreak() {
            LocalDate oldDate = LocalDate.now().minusDays(10);
            List<Session> old = List.of(
                    buildSession(oldDate, Category.TECHNOLOGY, Period.MORNING, 50, 4, 4, true)
            );
            when(mongoTemplate.findAll(Session.class)).thenReturn(old);

            DashboardSummaryDTO result = dashboardService.getSummary();

            assertThat(result.currentStreak()).isZero();
        }

        @Test
        @DisplayName("Should start streak from yesterday if no session today")
        void shouldStartFromYesterday() {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            List<Session> sessions = List.of(
                    buildSession(yesterday, Category.TECHNOLOGY, Period.MORNING, 50, 4, 4, true),
                    buildSession(yesterday.minusDays(1), Category.MATH, Period.MORNING, 25, 2, 4, false)
            );
            when(mongoTemplate.findAll(Session.class)).thenReturn(sessions);

            DashboardSummaryDTO result = dashboardService.getSummary();

            assertThat(result.currentStreak()).isEqualTo(2);
        }
    }
}

