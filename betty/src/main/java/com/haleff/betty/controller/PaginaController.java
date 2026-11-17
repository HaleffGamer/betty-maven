package com.haleff.betty.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serve as páginas HTML (templates/). */
@Controller
public class PaginaController {

    /** Página do cliente: http://localhost:8080/ */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /** Painel da equipe (dono e barbeiros): http://localhost:8080/painel */
    @GetMapping("/painel")
    public String painel() {
        return "painel";
    }
}
