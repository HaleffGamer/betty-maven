package com.haleff.betty.repository;

import org.springframework.jdbc.support.KeyHolder;

import java.util.Map;

/** Pega o "id" que o banco gerou ao inserir uma linha. */
final class ChavesGeradas {

    private ChavesGeradas() {
    }

    static long id(KeyHolder holder) {
        Map<String, Object> chaves = holder.getKeys();
        if (chaves != null) {
            for (Map.Entry<String, Object> e : chaves.entrySet()) {
                if (e.getKey().equalsIgnoreCase("id") && e.getValue() instanceof Number n) {
                    return n.longValue();
                }
            }
        }
        Number unica = holder.getKey();
        if (unica == null) throw new IllegalStateException("O banco não devolveu o id gerado.");
        return unica.longValue();
    }
}
