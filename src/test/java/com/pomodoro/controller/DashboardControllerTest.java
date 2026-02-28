package com.pomodoro.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pomodoro.dto.dashboard.*;
import com.pomodoro.entity.Category;
import com.pomodoro.entity.Period;
import com.pomodoro.exception.GlobalExceptionHandler;
import com.pomodoro.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardController")
class DashboardControllerTest {
    @Mock
    private DashboardService dashboardService;
    @InjectMocks
    private DashboardController dashboardController;
    private MockMvc mockMvc;
    @BeforeEach
    @SuppressWarnings("removal")
    void setUp() {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        var converter = new MappingJackson2HttpMessageConverter(objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }
    // ==================== SUMMARY ====================
    @Nested
    @DisplayName("GET /dashboard/summary")
    class SummaryEndpoint {
        @Test
        @DisplayName("Should return 200 with summary data")
        void shouldReturnSummary() throws Exception {
            var summary = new DashboardSummaryDTO(10, 7, 70.0, 500, 3.5, 50.0, 5);
            when(dashboardService.getSummary()).thenReturn(summary);
            mockMvc.perform(get("/dashboard/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalSessions").value(10))
                    .andExpect(jsonPath("$.successfulSessions").value(7))
                    .andExpect(jsonPath("$.successRate").value(70.0))
                    .andExpect(jsonPath("$.totalFocusMinutes").value(500))
                    .andExpect(jsonPath("$.averageCompletedCycles").value(3.5))
                    .andExpect(jsonPath("$.currentStreak").value(5));
        }
        @Test
        @DisplayName("Should return 200 with empty summary")
        void shouldReturnEmptySummary() throws Exception {
            var empty = new DashboardSummaryDTO(0, 0, 0.0, 0, 0.0, 0.0, 0);
            when(dashboardService.getSummary()).thenReturn(empty);
            mockMvc.perform(get("/dashboard/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalSessions").value(0))
                    .andExpect(jsonPath("$.currentStreak").value(0));
        }
    }
    // ==================== OVERVIEW ====================
    @Nested
    @DisplayName("GET /dashboard/overview")
    class OverviewEndpoint {
        @Test
        @DisplayName("Should return 200 with weekly overview")
        void shouldReturnWeeklyOverview() throws Exception {
            var entry = new OverviewEntryDTO("27/02", 50, 2, 100.0);
            var overview = new OverviewDTO("week", 150, 5, 80.0, List.of(entry));
            when(dashboardService.getOverview(eq("week"), isNull(), isNull())).thenReturn(overview);
            mockMvc.perform(get("/dashboard/overview").param("period", "week"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.period").value("week"))
                    .andExpect(jsonPath("$.totalFocusMinutes").value(150))
                    .andExpect(jsonPath("$.totalSessions").value(5))
                    .andExpect(jsonPath("$.data[0].label").value("27/02"));
        }
        @Test
        @DisplayName("Should use default period (week)")
        void shouldUseDefaultPeriod() throws Exception {
            var overview = new OverviewDTO("week", 0, 0, 0.0, List.of());
            when(dashboardService.getOverview(eq("week"), isNull(), isNull())).thenReturn(overview);
            mockMvc.perform(get("/dashboard/overview"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.period").value("week"));
        }
        @Test
        @DisplayName("Should return 400 for invalid period")
        void shouldReturn400ForInvalidPeriod() throws Exception {
            when(dashboardService.getOverview(eq("invalid"), isNull(), isNull()))
                    .thenThrow(new IllegalArgumentException("Invalid period: invalid. Use: week, month, year, all"));
            mockMvc.perform(get("/dashboard/overview").param("period", "invalid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }
    }
    // ==================== BY CATEGORY ====================
    @Nested
    @DisplayName("GET /dashboard/by-category")
    class ByCategoryEndpoint {
        @Test
        @DisplayName("Should return 200 with category stats")
        void shouldReturnCategoryStats() throws Exception {
            var tech = new CategoryStatsDTO(Category.TECHNOLOGY, 200, 5, 80.0, 60.0);
            var math = new CategoryStatsDTO(Category.MATH, 100, 3, 66.67, 30.0);
            when(dashboardService.getByCategory(eq("month"), isNull(), isNull())).thenReturn(List.of(tech, math));
            mockMvc.perform(get("/dashboard/by-category").param("period", "month"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].category").value("TECHNOLOGY"))
                    .andExpect(jsonPath("$[0].totalFocusMinutes").value(200))
                    .andExpect(jsonPath("$[1].category").value("MATH"));
        }
        @Test
        @DisplayName("Should return empty list when no sessions")
        void shouldReturnEmptyList() throws Exception {
            when(dashboardService.getByCategory(eq("month"), isNull(), isNull())).thenReturn(List.of());
            mockMvc.perform(get("/dashboard/by-category").param("period", "month"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }
    // ==================== HEATMAP ====================
    @Nested
    @DisplayName("GET /dashboard/heatmap")
    class HeatmapEndpoint {
        @Test
        @DisplayName("Should return 200 with heatmap data")
        void shouldReturnHeatmap() throws Exception {
            var entry1 = new HeatmapEntryDTO(LocalDate.of(2026, 1, 1), 0, 0);
            var entry2 = new HeatmapEntryDTO(LocalDate.of(2026, 1, 2), 50, 2);
            when(dashboardService.getHeatmap(2026)).thenReturn(List.of(entry1, entry2));
            mockMvc.perform(get("/dashboard/heatmap").param("year", "2026"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].date").value("2026-01-01"))
                    .andExpect(jsonPath("$[0].totalMinutes").value(0))
                    .andExpect(jsonPath("$[1].totalMinutes").value(50))
                    .andExpect(jsonPath("$[1].sessions").value(2));
        }
    }
    // ==================== BY PERIOD ====================
    @Nested
    @DisplayName("GET /dashboard/by-period")
    class ByPeriodEndpoint {
        @Test
        @DisplayName("Should return 200 with all 3 periods")
        void shouldReturnAllPeriods() throws Exception {
            var morning = new PeriodStatsDTO(Period.MORNING, 200, 5, 80.0, 40.0);
            var afternoon = new PeriodStatsDTO(Period.AFTERNOON, 100, 3, 66.67, 33.33);
            var night = new PeriodStatsDTO(Period.NIGHT, 50, 1, 100.0, 50.0);
            when(dashboardService.getByPeriod()).thenReturn(List.of(morning, afternoon, night));
            mockMvc.perform(get("/dashboard/by-period"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].period").value("MORNING"))
                    .andExpect(jsonPath("$[1].period").value("AFTERNOON"))
                    .andExpect(jsonPath("$[2].period").value("NIGHT"));
        }
    }
    // ==================== BY WEEKDAY ====================
    @Nested
    @DisplayName("GET /dashboard/by-weekday")
    class ByWeekdayEndpoint {
        @Test
        @DisplayName("Should return 200 with all 7 weekdays")
        void shouldReturnAllWeekdays() throws Exception {
            List<WeekdayStatsDTO> weekdays = List.of(
                    new WeekdayStatsDTO(DayOfWeek.MONDAY, 100, 3, 66.67, 33.33),
                    new WeekdayStatsDTO(DayOfWeek.TUESDAY, 80, 2, 50.0, 40.0),
                    new WeekdayStatsDTO(DayOfWeek.WEDNESDAY, 0, 0, 0.0, 0.0),
                    new WeekdayStatsDTO(DayOfWeek.THURSDAY, 50, 1, 100.0, 50.0),
                    new WeekdayStatsDTO(DayOfWeek.FRIDAY, 120, 4, 75.0, 30.0),
                    new WeekdayStatsDTO(DayOfWeek.SATURDAY, 0, 0, 0.0, 0.0),
                    new WeekdayStatsDTO(DayOfWeek.SUNDAY, 60, 2, 100.0, 30.0)
            );
            when(dashboardService.getByWeekday()).thenReturn(weekdays);
            mockMvc.perform(get("/dashboard/by-weekday"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(7))
                    .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                    .andExpect(jsonPath("$[4].dayOfWeek").value("FRIDAY"));
        }
    }
    // ==================== GOALS ====================
    @Nested
    @DisplayName("GET /dashboard/goals")
    class GoalsEndpoint {
        @Test
        @DisplayName("Should return 200 with goals data")
        void shouldReturnGoals() throws Exception {
            var goals = new GoalsDTO("week", 180, 4, 2, 50.0, 25.71, 3, 7);
            when(dashboardService.getGoals(eq("week"), isNull(), isNull())).thenReturn(goals);
            mockMvc.perform(get("/dashboard/goals").param("period", "week"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.period").value("week"))
                    .andExpect(jsonPath("$.totalFocusMinutes").value(180))
                    .andExpect(jsonPath("$.totalSessions").value(4))
                    .andExpect(jsonPath("$.successfulSessions").value(2))
                    .andExpect(jsonPath("$.activeDays").value(3))
                    .andExpect(jsonPath("$.totalDaysInPeriod").value(7));
        }
        @Test
        @DisplayName("Should return 400 for invalid period")
        void shouldReturn400ForInvalidPeriod() throws Exception {
            when(dashboardService.getGoals(eq("invalid"), isNull(), isNull()))
                    .thenThrow(new IllegalArgumentException("Invalid period: invalid. Use: week, month, year, all"));
            mockMvc.perform(get("/dashboard/goals").param("period", "invalid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }
    }
}
