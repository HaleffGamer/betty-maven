package com.haleff.betty.dto;

import java.math.BigDecimal;

public record ServicoRequest(String nome, int duracaoMinutos, BigDecimal preco) {
}
