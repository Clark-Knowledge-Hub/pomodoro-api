package com.pomodoro.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pomodoro.dto.SessionDetailDTO;
import com.pomodoro.dto.SessionFilter;
import com.pomodoro.dto.SessionSummaryDTO;
import com.pomodoro.entity.Category;
import com.pomodoro.entity.Period;
import com.pomodoro.entity.Session;
import com.pomodoro.service.SessionService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionController")
class SessionControllerTest {

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private SessionController sessionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private Session sessionMock;
    private SessionSummaryDTO summaryMock;
    private SessionDetailDTO detailMock;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(sessionController)
                .setMessageConverters(converter)
                .build();

        sessionMock = Session.builder()
                .id("abc123")
                .date(LocalDate.of(2025, 7, 18))
                .dayOfWeek(DayOfWeek.FRIDAY)
                .startTime(LocalTime.of(14, 30))
                .targetCycles(4)
                .completedCycles(4)
                .totalFocusMinutes(100)
                .totalBreakMinutes(10)
                .success(true)
                .period(Period.AFTERNOON)
                .category(Category.TECHNOLOGY)
                .build();

        summaryMock = new SessionSummaryDTO(
                "abc123",
                LocalDate.of(2025, 7, 18),
                Period.AFTERNOON,
                Category.TECHNOLOGY,
                4, 4, true, 100
        );

        detailMock = new SessionDetailDTO(
                "abc123",
                LocalDate.of(2025, 7, 18),
                DayOfWeek.FRIDAY,
                LocalTime.of(14, 30),
                Period.AFTERNOON,
                Category.TECHNOLOGY,
                4, 4, true, 100, 10
        );
    }

    // -------------------------------------------------------------------------
    // POST /sessions
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /sessions")
    class Create {

        @Test
        @DisplayName("deve retornar 201 e a sessão criada")
        void shouldReturn201WithCreatedSession() throws Exception {
            when(sessionService.save(any(Session.class))).thenReturn(sessionMock);

            mockMvc.perform(post("/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sessionMock)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("abc123"))
                    .andExpect(jsonPath("$.category").value("TECHNOLOGY"))
                    .andExpect(jsonPath("$.period").value("AFTERNOON"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.targetCycles").value(4))
                    .andExpect(jsonPath("$.completedCycles").value(4));

            verify(sessionService, times(1)).save(any(Session.class));
        }

        @Test
        @DisplayName("deve chamar service.save com o body correto")
        void shouldDelegateToService() throws Exception {
            when(sessionService.save(any(Session.class))).thenReturn(sessionMock);

            mockMvc.perform(post("/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sessionMock)))
                    .andExpect(status().isCreated());

            verify(sessionService).save(any(Session.class));
        }
    }

    // -------------------------------------------------------------------------
    // GET /sessions
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /sessions")
    class FindAll {

        @Test
        @DisplayName("deve retornar 200 com page de SessionSummaryDTO")
        void shouldReturn200WithPage() throws Exception {
            Page<SessionSummaryDTO> page = new PageImpl<>(
                    List.of(summaryMock),
                    PageRequest.of(0, 10, Sort.by("date").descending()),
                    1
            );
            when(sessionService.findAll(any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/sessions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value("abc123"))
                    .andExpect(jsonPath("$.content[0].period").value("AFTERNOON"))
                    .andExpect(jsonPath("$.content[0].category").value("TECHNOLOGY"))
                    .andExpect(jsonPath("$.content[0].success").value(true))
                    .andExpect(jsonPath("$.content[0].totalFocusMinutes").value(100))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));

            verify(sessionService, times(1)).findAll(any(), any(Pageable.class));
        }

        @Test
        @DisplayName("deve retornar 200 com page vazia quando não há sessões")
        void shouldReturn200WithEmptyPage() throws Exception {
            Page<SessionSummaryDTO> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(sessionService.findAll(any(), any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/sessions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("deve aceitar parâmetros de paginação customizados")
        void shouldAcceptCustomPaginationParams() throws Exception {
            Page<SessionSummaryDTO> page = new PageImpl<>(List.of(summaryMock), PageRequest.of(2, 5), 15);
            when(sessionService.findAll(any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/sessions")
                            .param("page", "2")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(15));
        }

        @Test
        @DisplayName("deve aceitar parâmetros de ordenação")
        void shouldAcceptSortParams() throws Exception {
            Page<SessionSummaryDTO> page = new PageImpl<>(
                    List.of(summaryMock),
                    PageRequest.of(0, 10, Sort.by("date").ascending()),
                    1
            );
            when(sessionService.findAll(any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/sessions")
                            .param("sortBy", "date")
                            .param("sortDir", "asc"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve aceitar filtros por query params")
        void shouldAcceptFilterParams() throws Exception {
            Page<SessionSummaryDTO> page = new PageImpl<>(
                    List.of(summaryMock),
                    PageRequest.of(0, 10),
                    1
            );
            when(sessionService.findAll(any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/sessions")
                            .param("period", "AFTERNOON")
                            .param("category", "TECHNOLOGY")
                            .param("success", "true")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].period").value("AFTERNOON"));
        }
    }

    // -------------------------------------------------------------------------
    // GET /sessions/{id}
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /sessions/{id}")
    class FindById {

        @Test
        @DisplayName("deve retornar 200 com SessionDetailDTO quando id existir")
        void shouldReturn200WithDetailDTO() throws Exception {
            when(sessionService.findById("abc123")).thenReturn(detailMock);

            mockMvc.perform(get("/sessions/abc123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("abc123"))
                    .andExpect(jsonPath("$.dayOfWeek").value("FRIDAY"))
                    .andExpect(jsonPath("$.targetCycles").value(4))
                    .andExpect(jsonPath("$.completedCycles").value(4))
                    .andExpect(jsonPath("$.totalFocusMinutes").value(100))
                    .andExpect(jsonPath("$.totalBreakMinutes").value(10))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.period").value("AFTERNOON"))
                    .andExpect(jsonPath("$.category").value("TECHNOLOGY"));

            verify(sessionService, times(1)).findById("abc123");
        }

        @Test
        @DisplayName("deve lançar exceção quando id não existir")
        void shouldThrowWhenNotFound() {
            when(sessionService.findById("naoexiste"))
                    .thenThrow(new RuntimeException("Session not found: naoexiste"));

            ServletException ex = assertThrows(ServletException.class, () ->
                    mockMvc.perform(get("/sessions/naoexiste"))
            );

            assertThat(ex.getRootCause().getMessage()).contains("Session not found: naoexiste");
        }
    }
}
