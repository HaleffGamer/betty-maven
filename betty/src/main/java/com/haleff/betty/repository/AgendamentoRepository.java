package com.haleff.betty.repository;

import com.haleff.betty.model.Agendamento;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/** Tabela "agendamentos" no banco de dados. */
@Repository
public class AgendamentoRepository {

    private static final String COLUNAS =
            "id, barbeiro_id, servico_id, cliente_nome, cliente_telefone, dia, hora_inicio";

    private static final RowMapper<Agendamento> MAPEADOR = (rs, n) -> new Agendamento(
            rs.getLong("id"), rs.getLong("barbeiro_id"), rs.getLong("servico_id"),
            rs.getString("cliente_nome"), rs.getString("cliente_telefone"),
            rs.getObject("dia", LocalDate.class), rs.getObject("hora_inicio", LocalTime.class));

    private final JdbcClient jdbc;

    public AgendamentoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Agendamento save(Agendamento a) {
        KeyHolder chave = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO agendamentos (barbeiro_id, servico_id, cliente_nome, cliente_telefone, dia, hora_inicio) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")
                .params(a.barbeiroId(), a.servicoId(), a.clienteNome(), a.clienteTelefone(), a.data(), a.hora())
                .update(chave);
        return new Agendamento(ChavesGeradas.id(chave), a.barbeiroId(), a.servicoId(),
                a.clienteNome(), a.clienteTelefone(), a.data(), a.hora());
    }

    public Optional<Agendamento> findById(Long id) {
        return jdbc.sql("SELECT " + COLUNAS + " FROM agendamentos WHERE id = ?").param(id).query(MAPEADOR).optional();
    }

    public List<Agendamento> findAll() {
        return jdbc.sql("SELECT " + COLUNAS + " FROM agendamentos ORDER BY dia, hora_inicio").query(MAPEADOR).list();
    }

    public List<Agendamento> findByData(LocalDate data) {
        return jdbc.sql("SELECT " + COLUNAS + " FROM agendamentos WHERE dia = ? ORDER BY hora_inicio")
                .param(data).query(MAPEADOR).list();
    }

    public List<Agendamento> findByBarbeiroIdAndData(Long barbeiroId, LocalDate data) {
        return jdbc.sql("SELECT " + COLUNAS + " FROM agendamentos WHERE barbeiro_id = ? AND dia = ? ORDER BY hora_inicio")
                .params(barbeiroId, data).query(MAPEADOR).list();
    }

    public boolean deleteById(Long id) {
        return jdbc.sql("DELETE FROM agendamentos WHERE id = ?").param(id).update() > 0;
    }
}
