package com.haleff.betty.dto;

import java.math.BigDecimal;

/** Agendamento já "montado" com nomes e textos prontos para exibir. */
public record AgendamentoResponse(
        Long id,
        Long barbeiroId,
        String barbeiro,
        String servico,
        BigDecimal preco,
        int duracaoMinutos,
        String clienteNome,
        String clienteTelefone,
        String data,   // formato yyyy-MM-dd
        String hora) { // formato HH:mm
}
