package com.haleff.betty;

import com.haleff.betty.model.Barbeiro;
import com.haleff.betty.model.Papel;
import com.haleff.betty.model.Servico;
import com.haleff.betty.model.Usuario;
import com.haleff.betty.repository.BarbeiroRepository;
import com.haleff.betty.repository.ServicoRepository;
import com.haleff.betty.repository.UsuarioRepository;
import com.haleff.betty.security.Senhas;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Roda uma vez quando o app inicia e preenche o banco SÓ se ele estiver vazio:
 * barbeiros e serviços de exemplo, e as contas de login.
 */
@Component
public class DadosIniciais implements ApplicationRunner {

    private final BarbeiroRepository barbeiros;
    private final ServicoRepository servicos;
    private final UsuarioRepository usuarios;
    private final String senhaDono;

    public DadosIniciais(BarbeiroRepository barbeiros, ServicoRepository servicos, UsuarioRepository usuarios,
                         @Value("${betty.dono.senha:dono123}") String senhaDono) {
        this.barbeiros = barbeiros;
        this.servicos = servicos;
        this.usuarios = usuarios;
        this.senhaDono = senhaDono;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (barbeiros.findAll().isEmpty()) {
            criarBarbeiro("Carlos", "carlos", "carlos123");
            criarBarbeiro("Rafael", "rafael", "rafael123");
            criarBarbeiro("João", "joao", "joao123");
        }
        if (servicos.findAll().isEmpty()) {
            servicos.save(new Servico(null, "Corte de cabelo", 30, new BigDecimal("40.00"), true));
            servicos.save(new Servico(null, "Barba", 30, new BigDecimal("30.00"), true));
            servicos.save(new Servico(null, "Corte + Barba", 60, new BigDecimal("65.00"), true));
            servicos.save(new Servico(null, "Sobrancelha", 30, new BigDecimal("15.00"), true));
        }
        if (usuarios.findByLogin("dono").isEmpty()) {
            usuarios.save(new Usuario(null, "Dono", "dono", Senhas.hash(senhaDono), Papel.DONO, null));
        }
    }

    private void criarBarbeiro(String nome, String login, String senha) {
        Barbeiro b = barbeiros.save(new Barbeiro(null, nome, true));
        usuarios.save(new Usuario(null, nome, login, Senhas.hash(senha), Papel.BARBEIRO, b.id()));
    }
}
