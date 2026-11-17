package com.haleff.betty.repository;

import com.haleff.betty.model.Barbeiro;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Tabela "barbeiros" no banco de dados. */
@Repository
public class BarbeiroRepository {

    private static final RowMapper<Barbeiro> MAPEADOR = (rs, n) ->
            new Barbeiro(rs.getLong("id"), rs.getString("nome"), rs.getBoolean("ativo"));

    private final JdbcClient jdbc;

    public BarbeiroRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** id nulo = INSERT (novo); id preenchido = UPDATE. */
    public Barbeiro save(Barbeiro b) {
        if (b.id() == null) {
            KeyHolder chave = new GeneratedKeyHolder();
            jdbc.sql("INSERT INTO barbeiros (nome, ativo) VALUES (?, ?)")
                    .params(b.nome(), b.ativo())
                    .update(chave);
            return new Barbeiro(ChavesGeradas.id(chave), b.nome(), b.ativo());
        }
        jdbc.sql("UPDATE barbeiros SET nome = ?, ativo = ? WHERE id = ?")
                .params(b.nome(), b.ativo(), b.id())
                .update();
        return b;
    }

    public List<Barbeiro> findAll() {
        return jdbc.sql("SELECT id, nome, ativo FROM barbeiros ORDER BY id").query(MAPEADOR).list();
    }

    public List<Barbeiro> findAtivos() {
        return jdbc.sql("SELECT id, nome, ativo FROM barbeiros WHERE ativo = TRUE ORDER BY id").query(MAPEADOR).list();
    }

    public Optional<Barbeiro> findById(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql("SELECT id, nome, ativo FROM barbeiros WHERE id = ?").param(id).query(MAPEADOR).optional();
    }
}
