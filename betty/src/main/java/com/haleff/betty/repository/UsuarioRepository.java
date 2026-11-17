package com.haleff.betty.repository;

import com.haleff.betty.model.Papel;
import com.haleff.betty.model.Usuario;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Tabela "usuarios" (logins da equipe). Os dados iniciais são criados em DadosIniciais. */
@Repository
public class UsuarioRepository {

    private static final RowMapper<Usuario> MAPEADOR = (rs, n) -> {
        long barbeiroId = rs.getLong("barbeiro_id");
        Long barbeiro = rs.wasNull() ? null : barbeiroId;
        return new Usuario(rs.getLong("id"), rs.getString("nome"), rs.getString("login"),
                rs.getString("senha_hash"), Papel.valueOf(rs.getString("papel")), barbeiro);
    };

    private final JdbcClient jdbc;

    public UsuarioRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Só insere (não há edição de usuário por enquanto). */
    public Usuario save(Usuario u) {
        KeyHolder chave = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO usuarios (nome, login, senha_hash, papel, barbeiro_id) VALUES (?, ?, ?, ?, ?)")
                .params(u.nome(), u.login(), u.senhaHash(), u.papel().name(), u.barbeiroId())
                .update(chave);
        return new Usuario(ChavesGeradas.id(chave), u.nome(), u.login(), u.senhaHash(), u.papel(), u.barbeiroId());
    }

    public Optional<Usuario> findByLogin(String login) {
        return jdbc.sql("SELECT id, nome, login, senha_hash, papel, barbeiro_id FROM usuarios WHERE login = ?")
                .param(login)
                .query(MAPEADOR)
                .optional();
    }

    public long count() {
        return jdbc.sql("SELECT COUNT(*) FROM usuarios").query(Long.class).single();
    }
}
