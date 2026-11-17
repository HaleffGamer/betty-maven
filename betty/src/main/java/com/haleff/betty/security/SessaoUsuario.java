package com.haleff.betty.security;

import com.haleff.betty.model.Papel;

import java.io.Serializable;

/** O que guardamos na sessão depois do login (nunca a senha). */
public record SessaoUsuario(Long id, String nome, Papel papel, Long barbeiroId) implements Serializable {
}
