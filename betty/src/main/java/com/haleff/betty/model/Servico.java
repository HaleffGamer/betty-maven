package com.haleff.betty.model;

import java.math.BigDecimal;

public record Servico(Long id, String nome, int duracaoMinutos, BigDecimal preco, boolean ativo) {
}
