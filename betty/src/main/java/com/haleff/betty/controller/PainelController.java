package com.haleff.betty.controller;

import com.haleff.betty.dto.AgendamentoResponse;
import com.haleff.betty.dto.NovoBarbeiroRequest;
import com.haleff.betty.dto.ServicoRequest;
import com.haleff.betty.model.Barbeiro;
import com.haleff.betty.model.Servico;
import com.haleff.betty.security.AuthService;
import com.haleff.betty.security.SessaoUsuario;
import com.haleff.betty.service.AgendamentoService;
import com.haleff.betty.service.CatalogoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * API do painel da equipe. Tudo aqui exige login.
 *  - /api/painel/agenda, /api/painel/agendamentos/{id}  -> dono e barbeiros
 *  - /api/painel/admin/**                                -> só o dono
 */
@RestController
@RequestMapping("/api/painel")
public class PainelController {

    private final AuthService auth;
    private final AgendamentoService agendamentos;
    private final CatalogoService catalogo;

    public PainelController(AuthService auth, AgendamentoService agendamentos, CatalogoService catalogo) {
        this.auth = auth;
        this.agendamentos = agendamentos;
        this.catalogo = catalogo;
    }

    // ---------------- Agenda (dono e barbeiro) ----------------

    @GetMapping("/agenda")
    public List<AgendamentoResponse> agenda(
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(name = "barbeiroId", required = false) Long barbeiroId,
            HttpServletRequest request) {
        SessaoUsuario usuario = auth.exigirLogin(request);
        return agendamentos.listarPainel(usuario, data, barbeiroId);
    }

    @DeleteMapping("/agendamentos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable("id") Long id, HttpServletRequest request) {
        SessaoUsuario usuario = auth.exigirLogin(request);
        agendamentos.cancelar(usuario, id);
    }

    // ---------------- Equipe (só dono) ----------------

    @GetMapping("/admin/barbeiros")
    public List<Barbeiro> barbeiros(HttpServletRequest request) {
        auth.exigirDono(request);
        return catalogo.todosBarbeiros();
    }

    @PostMapping("/admin/barbeiros")
    @ResponseStatus(HttpStatus.CREATED)
    public Barbeiro novoBarbeiro(@RequestBody NovoBarbeiroRequest req, HttpServletRequest request) {
        auth.exigirDono(request);
        return catalogo.criarBarbeiro(req);
    }

    @PutMapping("/admin/barbeiros/{id}/ativo")
    public Barbeiro ativarBarbeiro(@PathVariable("id") Long id,
                                   @RequestParam("valor") boolean valor,
                                   HttpServletRequest request) {
        auth.exigirDono(request);
        return catalogo.definirAtivoBarbeiro(id, valor);
    }

    // ---------------- Serviços (só dono) ----------------

    @GetMapping("/admin/servicos")
    public List<Servico> servicos(HttpServletRequest request) {
        auth.exigirDono(request);
        return catalogo.todosServicos();
    }

    @PostMapping("/admin/servicos")
    @ResponseStatus(HttpStatus.CREATED)
    public Servico novoServico(@RequestBody ServicoRequest req, HttpServletRequest request) {
        auth.exigirDono(request);
        return catalogo.criarServico(req);
    }

    @PutMapping("/admin/servicos/{id}")
    public Servico atualizarServico(@PathVariable("id") Long id,
                                    @RequestBody ServicoRequest req,
                                    HttpServletRequest request) {
        auth.exigirDono(request);
        return catalogo.atualizarServico(id, req);
    }

    @PutMapping("/admin/servicos/{id}/ativo")
    public Servico ativarServico(@PathVariable("id") Long id,
                                 @RequestParam("valor") boolean valor,
                                 HttpServletRequest request) {
        auth.exigirDono(request);
        return catalogo.definirAtivoServico(id, valor);
    }
}
