package com.haleff.betty.controller;

import com.haleff.betty.dto.AgendamentoRequest;
import com.haleff.betty.dto.AgendamentoResponse;
import com.haleff.betty.service.AgendamentoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Parte PÚBLICA (cliente): ver horários livres e agendar. Listar/cancelar fica no painel da equipe. */
@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendamentoService service;

    public AgendamentoController(AgendamentoService service) {
        this.service = service;
    }

    /** GET /api/agendamentos/horarios?barbeiroId=1&servicoId=1&data=2026-11-10 */
    @GetMapping("/horarios")
    public List<String> horarios(
            @RequestParam("barbeiroId") Long barbeiroId,
            @RequestParam("servicoId") Long servicoId,
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.horariosDisponiveis(barbeiroId, servicoId, data);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoResponse criar(@RequestBody AgendamentoRequest request) {
        return service.criar(request);
    }
}
