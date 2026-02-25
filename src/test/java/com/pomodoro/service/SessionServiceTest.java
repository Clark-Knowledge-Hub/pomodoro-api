package com.pomodoro.service;

import com.pomodoro.dto.SessionDetailDTO;
import com.pomodoro.dto.SessionFilter;
import com.pomodoro.dto.SessionSummaryDTO;
import com.pomodoro.entity.Category;
import com.pomodoro.entity.Period;
import com.pomodoro.entity.Session;
import com.pomodoro.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService")
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private SessionService sessionService;

    private Session sessionMock;

    @BeforeEach
    void setUp() {
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
    }

    // -------------------------------------------------------------------------
    // save
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("deve salvar e retornar a sessão com id gerado")
        void shouldSaveAndReturnSession() {
            when(sessionRepository.save(sessionMock)).thenReturn(sessionMock);

            Session result = sessionService.save(sessionMock);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("abc123");
            assertThat(result.getCategory()).isEqualTo(Category.TECHNOLOGY);
            verify(sessionRepository, times(1)).save(sessionMock);
        }

        @Test
        @DisplayName("deve repassar qualquer exceção lançada pelo repository")
        void shouldPropagateRepositoryException() {
            when(sessionRepository.save(any())).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> sessionService.save(sessionMock))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");
        }
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("deve retornar SessionDetailDTO quando id existir")
        void shouldReturnDetailDTOWhenFound() {
            when(sessionRepository.findById("abc123")).thenReturn(Optional.of(sessionMock));

            SessionDetailDTO result = sessionService.findById("abc123");

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("abc123");
            assertThat(result.date()).isEqualTo(LocalDate.of(2025, 7, 18));
            assertThat(result.dayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
            assertThat(result.startTime()).isEqualTo(LocalTime.of(14, 30));
            assertThat(result.targetCycles()).isEqualTo(4);
            assertThat(result.completedCycles()).isEqualTo(4);
            assertThat(result.totalFocusMinutes()).isEqualTo(100);
            assertThat(result.totalBreakMinutes()).isEqualTo(10);
            assertThat(result.success()).isTrue();
            assertThat(result.period()).isEqualTo(Period.AFTERNOON);
            assertThat(result.category()).isEqualTo(Category.TECHNOLOGY);
        }

        @Test
        @DisplayName("deve lançar RuntimeException quando id não existir")
        void shouldThrowWhenNotFound() {
            when(sessionRepository.findById("naoexiste")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sessionService.findById("naoexiste"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Session not found");
        }
    }

    // -------------------------------------------------------------------------
    // findAll — paginação
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("deve retornar page com SessionSummaryDTO corretamente mapeado")
        void shouldReturnPageOfSummaryDTOs() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("date").descending());

            when(mongoTemplate.count(any(Query.class), eq(Session.class))).thenReturn(1L);
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(List.of(sessionMock));

            Page<SessionSummaryDTO> result = sessionService.findAll(null, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);

            SessionSummaryDTO dto = result.getContent().get(0);
            assertThat(dto.id()).isEqualTo("abc123");
            assertThat(dto.date()).isEqualTo(LocalDate.of(2025, 7, 18));
            assertThat(dto.completedCycles()).isEqualTo(4);
            assertThat(dto.targetCycles()).isEqualTo(4);
            assertThat(dto.success()).isTrue();
            assertThat(dto.totalFocusMinutes()).isEqualTo(100);
            assertThat(dto.period()).isEqualTo(Period.AFTERNOON);
            assertThat(dto.category()).isEqualTo(Category.TECHNOLOGY);
        }

        @Test
        @DisplayName("deve retornar page vazia quando não houver sessões")
        void shouldReturnEmptyPageWhenNoSessions() {
            Pageable pageable = PageRequest.of(0, 10);

            when(mongoTemplate.count(any(Query.class), eq(Session.class))).thenReturn(0L);
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(List.of());

            Page<SessionSummaryDTO> result = sessionService.findAll(null, pageable);

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("deve respeitar paginação — page e size")
        void shouldRespectPagination() {
            Pageable pageable = PageRequest.of(1, 5);

            when(mongoTemplate.count(any(Query.class), eq(Session.class))).thenReturn(10L);
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(List.of(sessionMock));

            Page<SessionSummaryDTO> result = sessionService.findAll(null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(10);
            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(5);
        }
    }

    // -------------------------------------------------------------------------
    // findAll — filtros
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("findAll() — filtros")
    class FindAllFilters {

        @Test
        @DisplayName("deve aplicar filtro por período")
        void shouldApplyPeriodFilter() {
            SessionFilter filter = new SessionFilter(null, null, Period.AFTERNOON, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            when(mongoTemplate.count(any(Query.class), eq(Session.class))).thenReturn(1L);
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(List.of(sessionMock));

            Page<SessionSummaryDTO> result = sessionService.findAll(filter, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).period()).isEqualTo(Period.AFTERNOON);
            verify(mongoTemplate, times(1)).find(any(Query.class), eq(Session.class));
        }

        @Test
        @DisplayName("deve aplicar filtro por categoria")
        void shouldApplyCategoryFilter() {
            SessionFilter filter = new SessionFilter(null, null, null, Category.TECHNOLOGY, null);
            Pageable pageable = PageRequest.of(0, 10);

            when(mongoTemplate.count(any(Query.class), eq(Session.class))).thenReturn(1L);
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(List.of(sessionMock));

            Page<SessionSummaryDTO> result = sessionService.findAll(filter, pageable);

            assertThat(result.getContent().get(0).category()).isEqualTo(Category.TECHNOLOGY);
        }

        @Test
        @DisplayName("deve aplicar filtro por sucesso = true")
        void shouldApplySuccessTrueFilter() {
            SessionFilter filter = new SessionFilter(null, null, null, null, true);
            Pageable pageable = PageRequest.of(0, 10);

            when(mongoTemplate.count(any(Query.class), eq(Session.class))).thenReturn(1L);
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(List.of(sessionMock));

            Page<SessionSummaryDTO> result = sessionService.findAll(filter, pageable);

            assertThat(result.getContent().get(0).success()).isTrue();
        }

        @Test
        @DisplayName("deve aplicar filtro por sucesso = false")
        void shouldApplySuccessFalseFilter() {
            Session failedSession = Session.builder()
                    .id("xyz")
                    .date(LocalDate.of(2025, 7, 19))
                    .dayOfWeek(DayOfWeek.SATURDAY)
                    .startTime(LocalTime.of(9, 0))
                    .targetCycles(4)
                    .completedCycles(2)
                    .totalFocusMinutes(50)
                    .totalBreakMinutes(20)
                    .success(false)
                    .period(Period.MORNING)
                    .category(Category.MATH)
                    .build();

            SessionFilter filter = new SessionFilter(null, null, null, null, false);
            Pageable pageable = PageRequest.of(0, 10);

            when(mongoTemplate.count(any(Query.class), eq(Session.class))).thenReturn(1L);
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(List.of(failedSession));

            Page<SessionSummaryDTO> result = sessionService.findAll(filter, pageable);

            assertThat(result.getContent().get(0).success()).isFalse();
        }

        @Test
        @DisplayName("deve aplicar filtro de intervalo de datas (startDate e endDate)")
        void shouldApplyDateRangeFilter() {
            SessionFilter filter = new SessionFilter(
                    LocalDate.of(2025, 7, 1),
                    LocalDate.of(2025, 7, 31),
                    null, null, null
            );
            Pageable pageable = PageRequest.of(0, 10);

            when(mongoTemplate.count(any(Query.class), eq(Session.class))).thenReturn(1L);
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(List.of(sessionMock));

            Page<SessionSummaryDTO> result = sessionService.findAll(filter, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(mongoTemplate, times(1)).find(any(Query.class), eq(Session.class));
        }

        @Test
        @DisplayName("deve aplicar filtro somente com startDate")
        void shouldApplyOnlyStartDateFilter() {
            SessionFilter filter = new SessionFilter(LocalDate.of(2025, 7, 1), null, null, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            when(mongoTemplate.count(any(Query.class), eq(Session.class))).thenReturn(1L);
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(List.of(sessionMock));

            Page<SessionSummaryDTO> result = sessionService.findAll(filter, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("deve aplicar filtro somente com endDate")
        void shouldApplyOnlyEndDateFilter() {
            SessionFilter filter = new SessionFilter(null, LocalDate.of(2025, 12, 31), null, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            when(mongoTemplate.count(any(Query.class), eq(Session.class))).thenReturn(1L);
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(List.of(sessionMock));

            Page<SessionSummaryDTO> result = sessionService.findAll(filter, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("deve funcionar sem nenhum filtro (filter null)")
        void shouldWorkWithNullFilter() {
            Pageable pageable = PageRequest.of(0, 10);

            when(mongoTemplate.count(any(Query.class), eq(Session.class))).thenReturn(2L);
            when(mongoTemplate.find(any(Query.class), eq(Session.class))).thenReturn(List.of(sessionMock, sessionMock));

            Page<SessionSummaryDTO> result = sessionService.findAll(null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);
        }
    }
}

