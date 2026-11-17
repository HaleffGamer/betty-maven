package com.haleff.betty.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record Agendamento(
        Long id,
        Long barbeiroId,
        Long servicoId,
        String clienteNome,
        String clienteTelefone,
        LocalDate data,
        LocalTime hora) {
}
