package com.haleff.betty.controller;

import com.haleff.betty.model.Servico;
import com.haleff.betty.repository.ServicoRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    private final ServicoRepository servicos;

    public ServicoController(ServicoRepository servicos) {
        this.servicos = servicos;
    }

    /** Só os serviços ativos aparecem para o cliente. */
    @GetMapping
    public List<Servico> listar() {
        return servicos.findAtivos();
    }
}
