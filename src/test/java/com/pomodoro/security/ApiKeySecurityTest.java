package com.pomodoro.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pomodoro.controller.SessionController;
import com.pomodoro.dto.SessionCreateDTO;
import com.pomodoro.dto.SessionDetailDTO;
import com.pomodoro.dto.SessionSummaryDTO;
import com.pomodoro.entity.Category;
import com.pomodoro.entity.Period;
import com.pomodoro.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
@Import(SecurityConfig.class)
@DisplayName("API Key Security")
class ApiKeySecurityTest {

    private static final String DEVICE_KEY = "pomodoro-pico-w-secret-key-2025";
    private static final String FRONTEND_KEY = "pomodoro-frontend-secret-key-2025";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessionService sessionService;

    private ObjectMapper objectMapper;
    private SessionCreateDTO createDTO;
    private SessionDetailDTO detailMock;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        createDTO = new SessionCreateDTO(
                LocalDate.of(2025, 7, 18),
                DayOfWeek.FRIDAY,
                LocalTime.of(14, 30),
                4, 4, 100, 10,
                true,
                Period.AFTERNOON,
                Category.TECHNOLOGY
        );

        detailMock = new SessionDetailDTO(
                "abc123", LocalDate.of(2025, 7, 18),
                DayOfWeek.FRIDAY, LocalTime.of(14, 30),
                Period.AFTERNOON, Category.TECHNOLOGY,
                4, 4, true, 100, 10
        );
    }

    // -------------------------------------------------------------------------
    // Sem API Key -> 401
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Sem API Key -> 401 Unauthorized")
    class WithoutApiKey {

        @Test
        @DisplayName("POST /sessions -> 401")
        void postShouldReturn401() throws Exception {
            mockMvc.perform(post("/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /sessions -> 401")
        void getShouldReturn401() throws Exception {
            mockMvc.perform(get("/sessions"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /sessions/{id} -> 401")
        void getByIdShouldReturn401() throws Exception {
            mockMvc.perform(get("/sessions/abc123"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // API Key invalida -> 401
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("API Key invalida -> 401 Unauthorized")
    class WithInvalidApiKey {

        @Test
        @DisplayName("POST com chave errada -> 401")
        void postShouldReturn401() throws Exception {
            mockMvc.perform(post("/sessions")
                            .header("X-API-KEY", "chave-errada")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET com chave errada -> 401")
        void getShouldReturn401() throws Exception {
            mockMvc.perform(get("/sessions")
                            .header("X-API-KEY", "chave-errada"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("API Key vazia -> 401")
        void emptyShouldReturn401() throws Exception {
            mockMvc.perform(get("/sessions")
                            .header("X-API-KEY", "   "))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // Device Key (ROLE_DEVICE) -> POST e GET permitidos
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Device Key (ROLE_DEVICE) -> POST + GET")
    class WithDeviceKey {

        @Test
        @DisplayName("POST /sessions -> 201")
        void postShouldReturn201() throws Exception {
            when(sessionService.save(any(SessionCreateDTO.class))).thenReturn(detailMock);

            mockMvc.perform(post("/sessions")
                            .header("X-API-KEY", DEVICE_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("abc123"));
        }

        @Test
        @DisplayName("GET /sessions -> 200")
        void getShouldReturn200() throws Exception {
            SessionSummaryDTO summary = new SessionSummaryDTO(
                    "abc123", LocalDate.of(2025, 7, 18),
                    Period.AFTERNOON, Category.TECHNOLOGY,
                    4, 4, true, 100
            );
            Page<SessionSummaryDTO> page = new PageImpl<>(List.of(summary));
            when(sessionService.findAll(any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/sessions")
                            .header("X-API-KEY", DEVICE_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value("abc123"));
        }

        @Test
        @DisplayName("GET /sessions/{id} -> 200")
        void getByIdShouldReturn200() throws Exception {
            when(sessionService.findById("abc123")).thenReturn(detailMock);

            mockMvc.perform(get("/sessions/abc123")
                            .header("X-API-KEY", DEVICE_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("abc123"));
        }
    }

    // -------------------------------------------------------------------------
    // Frontend Key (ROLE_FRONTEND) -> GET permitido, POST proibido (403)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Frontend Key (ROLE_FRONTEND) -> GET only")
    class WithFrontendKey {

        @Test
        @DisplayName("GET /sessions -> 200")
        void getShouldReturn200() throws Exception {
            SessionSummaryDTO summary = new SessionSummaryDTO(
                    "abc123", LocalDate.of(2025, 7, 18),
                    Period.AFTERNOON, Category.TECHNOLOGY,
                    4, 4, true, 100
            );
            Page<SessionSummaryDTO> page = new PageImpl<>(List.of(summary));
            when(sessionService.findAll(any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/sessions")
                            .header("X-API-KEY", FRONTEND_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value("abc123"));
        }

        @Test
        @DisplayName("GET /sessions/{id} -> 200")
        void getByIdShouldReturn200() throws Exception {
            when(sessionService.findById("abc123")).thenReturn(detailMock);

            mockMvc.perform(get("/sessions/abc123")
                            .header("X-API-KEY", FRONTEND_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("abc123"));
        }

        @Test
        @DisplayName("POST /sessions -> 403 Forbidden")
        void postShouldReturn403() throws Exception {
            mockMvc.perform(post("/sessions")
                            .header("X-API-KEY", FRONTEND_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isForbidden());
        }
    }
}



