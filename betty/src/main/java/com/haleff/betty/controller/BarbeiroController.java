package com.haleff.betty.controller;

import com.haleff.betty.model.Barbeiro;
import com.haleff.betty.repository.BarbeiroRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/barbeiros")
public class BarbeiroController {

    private final BarbeiroRepository barbeiros;

    public BarbeiroController(BarbeiroRepository barbeiros) {
        this.barbeiros = barbeiros;
    }

    /** Só os barbeiros ativos aparecem para o cliente. */
    @GetMapping
    public List<Barbeiro> listar() {
        return barbeiros.findAtivos();
    }
}
