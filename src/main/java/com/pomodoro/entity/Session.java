package com.pomodoro.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Document(collection = "sessions")
public class Session {

    @Id
    private String id;

    // Data
    private LocalDate date;         // ano, mes, dia
    private DayOfWeek dayOfWeek;    // dia_semana
    private LocalTime startTime;    // hora, minuto

    // Ciclos Pomodoro
    private int targetCycles;       // quantidade_ciclos_pomodoro_meta
    private int completedCycles;    // ciclos_feitos

    // Tempo
    private int totalFocusMinutes;  // tempo_total_de_foco_em_min
    private int totalBreakMinutes;  // tempo_total_pausa_em_minutos (distração)

    // Resultado
    private boolean success;        // sucesso (bateu a meta de ciclos)

    // Enums
    private Period period;          // manha, tarde, noite
    private Category category;      // tecnologia, matematica, etc.
}