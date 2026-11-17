package com.haleff.betty.security;

import com.haleff.betty.model.Barbeiro;
import com.haleff.betty.model.Papel;
import com.haleff.betty.model.Usuario;
import com.haleff.betty.repository.BarbeiroRepository;
import com.haleff.betty.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final String ATRIBUTO = "usuarioLogado";
    // usado só para gastar o mesmo tempo quando o usuário não existe
    private static final String HASH_FALSO = Senhas.hash("senha-que-ninguem-usa");

    private final UsuarioRepository usuarios;
    private final BarbeiroRepository barbeiros;

    public AuthService(UsuarioRepository usuarios, BarbeiroRepository barbeiros) {
        this.usuarios = usuarios;
        this.barbeiros = barbeiros;
    }

    public SessaoUsuario entrar(String login, String senha, HttpServletRequest request) {
        String l = login == null ? "" : login.trim().toLowerCase();
        String s = senha == null ? "" : senha;

        Usuario u = usuarios.findByLogin(l).orElse(null);
        boolean senhaOk = Senhas.confere(s, u != null ? u.senhaHash() : HASH_FALSO);
        if (u == null || !senhaOk || !contaAtiva(u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos.");
        }

        HttpSession antiga = request.getSession(false);
        if (antiga != null) antiga.invalidate(); // sessão nova a cada login
        SessaoUsuario sessao = new SessaoUsuario(u.id(), u.nome(), u.papel(), u.barbeiroId());
        request.getSession(true).setAttribute(ATRIBUTO, sessao);
        return sessao;
    }

    public void sair(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao != null) sessao.invalidate();
    }

    public SessaoUsuario exigirLogin(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        Object o = sessao == null ? null : sessao.getAttribute(ATRIBUTO);
        if (o instanceof SessaoUsuario u) return u;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Faça login para continuar.");
    }

    public SessaoUsuario exigirDono(HttpServletRequest request) {
        SessaoUsuario u = exigirLogin(request);
        if (u.papel() != Papel.DONO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só o dono pode fazer isso.");
        }
        return u;
    }

    private boolean contaAtiva(Usuario u) {
        if (u.barbeiroId() == null) return true;
        return barbeiros.findById(u.barbeiroId()).map(Barbeiro::ativo).orElse(false);
    }
}
