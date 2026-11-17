package com.haleff.betty.repository;

import com.haleff.betty.model.Servico;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Tabela "servicos" no banco de dados. */
@Repository
public class ServicoRepository {

    private static final String COLUNAS = "id, nome, duracao_minutos, preco, ativo";

    private static final RowMapper<Servico> MAPEADOR = (rs, n) -> new Servico(
            rs.getLong("id"), rs.getString("nome"), rs.getInt("duracao_minutos"),
            rs.getBigDecimal("preco"), rs.getBoolean("ativo"));

    private final JdbcClient jdbc;

    public ServicoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** id nulo = INSERT (novo); id preenchido = UPDATE. */
    public Servico save(Servico s) {
        if (s.id() == null) {
            KeyHolder chave = new GeneratedKeyHolder();
            jdbc.sql("INSERT INTO servicos (nome, duracao_minutos, preco, ativo) VALUES (?, ?, ?, ?)")
                    .params(s.nome(), s.duracaoMinutos(), s.preco(), s.ativo())
                    .update(chave);
            return new Servico(ChavesGeradas.id(chave), s.nome(), s.duracaoMinutos(), s.preco(), s.ativo());
        }
        jdbc.sql("UPDATE servicos SET nome = ?, duracao_minutos = ?, preco = ?, ativo = ? WHERE id = ?")
                .params(s.nome(), s.duracaoMinutos(), s.preco(), s.ativo(), s.id())
                .update();
        return s;
    }

    public List<Servico> findAll() {
        return jdbc.sql("SELECT " + COLUNAS + " FROM servicos ORDER BY id").query(MAPEADOR).list();
    }

    public List<Servico> findAtivos() {
        return jdbc.sql("SELECT " + COLUNAS + " FROM servicos WHERE ativo = TRUE ORDER BY id").query(MAPEADOR).list();
    }

    public Optional<Servico> findById(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql("SELECT " + COLUNAS + " FROM servicos WHERE id = ?").param(id).query(MAPEADOR).optional();
    }
}
