package com.haleff.betty.service;

import com.haleff.betty.dto.AgendamentoRequest;
import com.haleff.betty.dto.AgendamentoResponse;
import com.haleff.betty.model.Agendamento;
import com.haleff.betty.model.Barbeiro;
import com.haleff.betty.model.Servico;
import com.haleff.betty.repository.AgendamentoRepository;
import com.haleff.betty.repository.BarbeiroRepository;
import com.haleff.betty.repository.ServicoRepository;
import com.haleff.betty.security.SessaoUsuario;
import com.haleff.betty.model.Papel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgendamentoService {

    // ---- Regras de funcionamento da barbearia (ajuste como quiser) ----
    private static final LocalTime ABERTURA = LocalTime.of(9, 0);
    private static final LocalTime FECHAMENTO = LocalTime.of(19, 0);
    private static final LocalTime ALMOCO_INICIO = LocalTime.of(12, 0);
    private static final LocalTime ALMOCO_FIM = LocalTime.of(13, 0);
    private static final int PASSO_MINUTOS = 30;
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final AgendamentoRepository agendamentos;
    private final BarbeiroRepository barbeiros;
    private final ServicoRepository servicos;

    public AgendamentoService(AgendamentoRepository agendamentos,
                              BarbeiroRepository barbeiros,
                              ServicoRepository servicos) {
        this.agendamentos = agendamentos;
        this.barbeiros = barbeiros;
        this.servicos = servicos;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public List<String> horariosDisponiveis(Long barbeiroId, Long servicoId, LocalDate data) {
        Barbeiro barbeiro = buscarBarbeiroAtivo(barbeiroId);
        Servico servico = buscarServicoAtivo(servicoId);
        return gerarHorariosLivres(barbeiro, servico, data).stream()
                .map(LocalTime::toString) // "09:00"
                .toList();
    }

    /** Agenda do painel. Barbeiro só enxerga a própria; dono enxerga todas (e pode filtrar). */
    public List<AgendamentoResponse> listarPainel(SessaoUsuario usuario, LocalDate data, Long barbeiroId) {
        if (data == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe a data.");
        }
        Long filtro = usuario.papel() == Papel.BARBEIRO ? usuario.barbeiroId() : barbeiroId;
        return agendamentos.findByData(data).stream()
                .filter(a -> filtro == null || a.barbeiroId().equals(filtro))
                .map(this::paraResponse)
                .toList();
    }

    // ------------------------------------------------------------------
    // Comandos
    // ------------------------------------------------------------------

    /** synchronized: evita que duas pessoas reservem o mesmo horário ao mesmo tempo. */
    public synchronized AgendamentoResponse criar(AgendamentoRequest req) {
        String nome = req.clienteNome() == null ? "" : req.clienteNome().trim();
        String telefone = req.clienteTelefone() == null ? "" : req.clienteTelefone().trim();
        String digitos = telefone.replaceAll("\\D", "");

        if (nome.length() < 2) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe o nome do cliente.");
        }
        if (digitos.length() < 10 || digitos.length() > 11) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe um telefone válido com DDD.");
        }
        if (req.data() == null || req.hora() == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe a data e o horário.");
        }

        Barbeiro barbeiro = buscarBarbeiroAtivo(req.barbeiroId());
        Servico servico = buscarServicoAtivo(req.servicoId());

        if (!gerarHorariosLivres(barbeiro, servico, req.data()).contains(req.hora())) {
            throw erro(HttpStatus.CONFLICT, "Esse horário não está mais disponível. Escolha outro.");
        }

        Agendamento salvo = agendamentos.save(new Agendamento(
                null, barbeiro.id(), servico.id(), nome, telefone, req.data(), req.hora()));
        return paraResponse(salvo);
    }

    public void cancelar(SessaoUsuario usuario, Long id) {
        Agendamento a = agendamentos.findById(id)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Agendamento não encontrado."));
        if (usuario.papel() == Papel.BARBEIRO && !a.barbeiroId().equals(usuario.barbeiroId())) {
            throw erro(HttpStatus.FORBIDDEN, "Você só pode cancelar os seus próprios atendimentos.");
        }
        agendamentos.deleteById(id);
    }

    // ------------------------------------------------------------------
    // Regras internas
    // ------------------------------------------------------------------

    private List<LocalTime> gerarHorariosLivres(Barbeiro barbeiro, Servico servico, LocalDate data) {
        if (data == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe a data.");
        }

        List<LocalTime> livres = new ArrayList<>();
        LocalDate hoje = LocalDate.now(FUSO);

        // Fechado aos domingos e sem agenda no passado
        if (data.getDayOfWeek() == DayOfWeek.SUNDAY || data.isBefore(hoje)) {
            return livres;
        }

        LocalTime agora = data.equals(hoje) ? LocalTime.now(FUSO) : null;
        List<Agendamento> doDia = agendamentos.findByBarbeiroIdAndData(barbeiro.id(), data);
        int duracao = servico.duracaoMinutos();

        for (LocalTime inicio = ABERTURA;
             !inicio.plusMinutes(duracao).isAfter(FECHAMENTO);
             inicio = inicio.plusMinutes(PASSO_MINUTOS)) {

            LocalTime fim = inicio.plusMinutes(duracao);

            if (agora != null && !inicio.isAfter(agora)) continue;               // já passou
            if (sobrepoe(inicio, fim, ALMOCO_INICIO, ALMOCO_FIM)) continue;      // horário de almoço
            if (ocupado(doDia, inicio, fim)) continue;                           // já reservado

            livres.add(inicio);
        }
        return livres;
    }

    private boolean ocupado(List<Agendamento> doDia, LocalTime inicio, LocalTime fim) {
        for (Agendamento a : doDia) {
            int duracaoExistente = servicos.findById(a.servicoId())
                    .map(Servico::duracaoMinutos)
                    .orElse(PASSO_MINUTOS);
            if (sobrepoe(inicio, fim, a.hora(), a.hora().plusMinutes(duracaoExistente))) {
                return true;
            }
        }
        return false;
    }

    /** Dois intervalos [inicio, fim) se sobrepõem? */
    private boolean sobrepoe(LocalTime aIni, LocalTime aFim, LocalTime bIni, LocalTime bFim) {
        return aIni.isBefore(bFim) && bIni.isBefore(aFim);
    }

    private Barbeiro buscarBarbeiroAtivo(Long id) {
        return barbeiros.findById(id).filter(Barbeiro::ativo)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Barbeiro não encontrado."));
    }

    private Servico buscarServicoAtivo(Long id) {
        return servicos.findById(id).filter(Servico::ativo)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Serviço não encontrado."));
    }

    private AgendamentoResponse paraResponse(Agendamento a) {
        String nomeBarbeiro = barbeiros.findById(a.barbeiroId()).map(Barbeiro::nome).orElse("?");
        Servico servico = servicos.findById(a.servicoId()).orElse(null);
        return new AgendamentoResponse(a.id(), a.barbeiroId(), nomeBarbeiro,
                servico == null ? "?" : servico.nome(),
                servico == null ? BigDecimal.ZERO : servico.preco(),
                servico == null ? PASSO_MINUTOS : servico.duracaoMinutos(),
                a.clienteNome(), a.clienteTelefone(), a.data().toString(), a.hora().toString());
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
