package com.haleff.betty.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Dados que o front-end envia para criar um agendamento. */
public record AgendamentoRequest(
        Long barbeiroId,
        Long servicoId,
        String clienteNome,
        String clienteTelefone,
        LocalDate data,
        LocalTime hora) {
}
