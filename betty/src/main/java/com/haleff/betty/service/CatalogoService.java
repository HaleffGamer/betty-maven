package com.haleff.betty.service;

import com.haleff.betty.dto.NovoBarbeiroRequest;
import com.haleff.betty.dto.ServicoRequest;
import com.haleff.betty.model.Barbeiro;
import com.haleff.betty.model.Papel;
import com.haleff.betty.model.Servico;
import com.haleff.betty.model.Usuario;
import com.haleff.betty.repository.BarbeiroRepository;
import com.haleff.betty.repository.ServicoRepository;
import com.haleff.betty.repository.UsuarioRepository;
import com.haleff.betty.security.Senhas;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Gerenciamento de equipe e serviços (só o dono usa). */
@Service
public class CatalogoService {

    private final BarbeiroRepository barbeiros;
    private final ServicoRepository servicos;
    private final UsuarioRepository usuarios;

    public CatalogoService(BarbeiroRepository barbeiros, ServicoRepository servicos, UsuarioRepository usuarios) {
        this.barbeiros = barbeiros;
        this.servicos = servicos;
        this.usuarios = usuarios;
    }

    // ---------------- Barbeiros ----------------

    public List<Barbeiro> todosBarbeiros() {
        return barbeiros.findAll();
    }

    public synchronized Barbeiro criarBarbeiro(NovoBarbeiroRequest req) {
        String nome = req.nome() == null ? "" : req.nome().trim();
        String login = req.login() == null ? "" : req.login().trim().toLowerCase();
        String senha = req.senha() == null ? "" : req.senha();

        if (nome.length() < 2) throw erro(HttpStatus.BAD_REQUEST, "Informe o nome do barbeiro.");
        if (!login.matches("[a-z0-9._-]{3,30}")) {
            throw erro(HttpStatus.BAD_REQUEST, "O login deve ter de 3 a 30 caracteres (letras, números, ponto, hífen).");
        }
        if (senha.length() < 6) throw erro(HttpStatus.BAD_REQUEST, "A senha deve ter pelo menos 6 caracteres.");
        if (usuarios.findByLogin(login).isPresent()) throw erro(HttpStatus.CONFLICT, "Esse login já está em uso.");

        Barbeiro novo = barbeiros.save(new Barbeiro(null, nome, true));
        usuarios.save(new Usuario(null, nome, login, Senhas.hash(senha), Papel.BARBEIRO, novo.id()));
        return novo;
    }

    public Barbeiro definirAtivoBarbeiro(Long id, boolean ativo) {
        Barbeiro b = barbeiros.findById(id)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Barbeiro não encontrado."));
        return barbeiros.save(new Barbeiro(b.id(), b.nome(), ativo));
    }

    // ---------------- Serviços ----------------

    public List<Servico> todosServicos() {
        return servicos.findAll();
    }

    public Servico criarServico(ServicoRequest req) {
        validar(req);
        return servicos.save(new Servico(null, req.nome().trim(), req.duracaoMinutos(), preco(req), true));
    }

    public Servico atualizarServico(Long id, ServicoRequest req) {
        validar(req);
        Servico atual = servicos.findById(id)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Serviço não encontrado."));
        return servicos.save(new Servico(atual.id(), req.nome().trim(), req.duracaoMinutos(), preco(req), atual.ativo()));
    }

    public Servico definirAtivoServico(Long id, boolean ativo) {
        Servico s = servicos.findById(id)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Serviço não encontrado."));
        return servicos.save(new Servico(s.id(), s.nome(), s.duracaoMinutos(), s.preco(), ativo));
    }

    private void validar(ServicoRequest req) {
        if (req.nome() == null || req.nome().trim().length() < 2) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe o nome do serviço.");
        }
        if (req.duracaoMinutos() < 10 || req.duracaoMinutos() > 240) {
            throw erro(HttpStatus.BAD_REQUEST, "A duração deve ficar entre 10 e 240 minutos.");
        }
        if (req.preco() == null || req.preco().signum() <= 0) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe um preço maior que zero.");
        }
    }

    private BigDecimal preco(ServicoRequest req) {
        return req.preco().setScale(2, RoundingMode.HALF_UP);
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
