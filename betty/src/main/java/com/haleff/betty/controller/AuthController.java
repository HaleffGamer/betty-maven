package com.haleff.betty.controller;

import com.haleff.betty.dto.LoginRequest;
import com.haleff.betty.security.AuthService;
import com.haleff.betty.security.SessaoUsuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public SessaoUsuario entrar(@RequestBody LoginRequest req, HttpServletRequest request) {
        return auth.entrar(req.login(), req.senha(), request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sair(HttpServletRequest request) {
        auth.sair(request);
    }

    /** Quem está logado agora (401 se ninguém). */
    @GetMapping("/me")
    public SessaoUsuario eu(HttpServletRequest request) {
        return auth.exigirLogin(request);
    }
}
