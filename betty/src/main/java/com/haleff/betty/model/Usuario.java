package com.haleff.betty.model;

/** Login da equipe. Clientes NÃO têm login: agendam direto na página inicial. */
public record Usuario(Long id, String nome, String login, String senhaHash, Papel papel, Long barbeiroId) {
}
