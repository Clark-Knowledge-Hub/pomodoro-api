package com.pomodoro.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pomodoro.dto.SessionCreateDTO;
import com.pomodoro.dto.SessionDetailDTO;
import com.pomodoro.dto.SessionSummaryDTO;
import com.pomodoro.entity.Category;
import com.pomodoro.entity.Period;
import com.pomodoro.exception.GlobalExceptionHandler;
import com.pomodoro.exception.SessionNotFoundException;
import com.pomodoro.service.SessionService;
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
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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

    private SessionCreateDTO createDTO;
    private SessionSummaryDTO summaryMock;
    private SessionDetailDTO detailMock;

    @BeforeEach
    @SuppressWarnings("removal")
    void setUp() {
        objectMapper = Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        var converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(sessionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();

        createDTO = new SessionCreateDTO(
                LocalDate.of(2025, 7, 18),
                LocalTime.of(14, 30),
                4, 4, 100, 10,
                Category.TECHNOLOGY
        );

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
        @DisplayName("deve retornar 201 e a sessao criada")
        void shouldReturn201WithCreatedSession() throws Exception {
            when(sessionService.save(any(SessionCreateDTO.class))).thenReturn(detailMock);

            mockMvc.perform(post("/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("abc123"))
                    .andExpect(jsonPath("$.category").value("TECHNOLOGY"))
                    .andExpect(jsonPath("$.period").value("AFTERNOON"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.targetCycles").value(4))
                    .andExpect(jsonPath("$.completedCycles").value(4));

            verify(sessionService, times(1)).save(any(SessionCreateDTO.class));
        }

        @Test
        @DisplayName("deve chamar service.save com o body correto")
        void shouldDelegateToService() throws Exception {
            when(sessionService.save(any(SessionCreateDTO.class))).thenReturn(detailMock);

            mockMvc.perform(post("/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isCreated());

            verify(sessionService).save(any(SessionCreateDTO.class));
        }

        @Test
        @DisplayName("deve retornar 400 quando date for null")
        void shouldReturn400WhenDateIsNull() throws Exception {
            SessionCreateDTO invalid = new SessionCreateDTO(
                    null, LocalTime.of(14, 30),
                    4, 4, 100, 10, Category.TECHNOLOGY
            );

            mockMvc.perform(post("/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Validation Failed"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("date"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("date is required"));
        }

        @Test
        @DisplayName("deve retornar 400 quando category for null")
        void shouldReturn400WhenCategoryIsNull() throws Exception {
            SessionCreateDTO invalid = new SessionCreateDTO(
                    LocalDate.of(2025, 7, 18), LocalTime.of(14, 30),
                    4, 4, 100, 10, null
            );

            mockMvc.perform(post("/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("category"));
        }

        @Test
        @DisplayName("deve retornar 400 quando targetCycles for menor que 1")
        void shouldReturn400WhenTargetCyclesLessThan1() throws Exception {
            SessionCreateDTO invalid = new SessionCreateDTO(
                    LocalDate.of(2025, 7, 18), LocalTime.of(14, 30),
                    0, 4, 100, 10, Category.TECHNOLOGY
            );

            mockMvc.perform(post("/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("targetCycles"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("targetCycles must be at least 1"));
        }

        @Test
        @DisplayName("deve retornar 400 quando totalFocusMinutes for negativo")
        void shouldReturn400WhenFocusMinutesNegative() throws Exception {
            SessionCreateDTO invalid = new SessionCreateDTO(
                    LocalDate.of(2025, 7, 18), LocalTime.of(14, 30),
                    4, 4, -10, 10, Category.TECHNOLOGY
            );

            mockMvc.perform(post("/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("totalFocusMinutes"));
        }

        @Test
        @DisplayName("deve retornar 400 com multiplos erros de validacao")
        void shouldReturn400WithMultipleValidationErrors() throws Exception {
            SessionCreateDTO invalid = new SessionCreateDTO(
                    null, null,
                    0, -1, -5, -3, null
            );

            mockMvc.perform(post("/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Validation Failed"))
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)));
        }

        @Test
        @DisplayName("deve retornar 400 quando JSON for invalido (enum errado)")
        void shouldReturn400WhenEnumIsInvalid() throws Exception {
            String invalidJson = """
                    {
                        "date": "2025-07-18",
                        "startTime": "14:30",
                        "targetCycles": 4,
                        "completedCycles": 4,
                        "totalFocusMinutes": 100,
                        "totalBreakMinutes": 10,
                        "category": "INVALIDO"
                    }
                    """;

            mockMvc.perform(post("/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("INVALIDO")));
        }

        @Test
        @DisplayName("deve retornar 400 quando body estiver vazio")
        void shouldReturn400WhenBodyIsEmpty() throws Exception {
            mockMvc.perform(post("/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando JSON for malformado")
        void shouldReturn400WhenJsonIsMalformed() throws Exception {
            mockMvc.perform(post("/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"));
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
        @DisplayName("deve retornar 200 com page vazia quando nao ha sessoes")
        void shouldReturn200WithEmptyPage() throws Exception {
            Page<SessionSummaryDTO> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(sessionService.findAll(any(), any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/sessions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("deve aceitar parametros de paginacao customizados")
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
        @DisplayName("deve aceitar parametros de ordenacao")
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
        @DisplayName("deve retornar 404 com JSON de erro quando id nao existir")
        void shouldReturn404WhenNotFound() throws Exception {
            when(sessionService.findById("naoexiste"))
                    .thenThrow(new SessionNotFoundException("naoexiste"));

            mockMvc.perform(get("/sessions/naoexiste"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Session not found: naoexiste"))
                    .andExpect(jsonPath("$.path").value("/sessions/naoexiste"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }
}
