'use strict';

// ---------------------------------------------------------------
// Modo demonstração: se o HTML for aberto direto do arquivo (file://),
// não existe servidor/API. Então usamos dados de mentira só para você
// ver como a tela fica. Quando rodar pelo Spring, tudo é real.
// ---------------------------------------------------------------
const MODO_DEMO = location.protocol === 'file:';

const demo = {
    servicos: [
        { id: 1, nome: 'Corte de cabelo', duracaoMinutos: 30, preco: 40 },
        { id: 2, nome: 'Barba', duracaoMinutos: 30, preco: 30 },
        { id: 3, nome: 'Corte + Barba', duracaoMinutos: 60, preco: 65 },
        { id: 4, nome: 'Sobrancelha', duracaoMinutos: 30, preco: 15 }
    ],
    barbeiros: [{ id: 1, nome: 'Carlos' }, { id: 2, nome: 'Rafael' }, { id: 3, nome: 'João' }],
    agendamentos: [],
    proximoId: 1
};

function hhmm(minutos) {
    return String(Math.floor(minutos / 60)).padStart(2, '0') + ':' + String(minutos % 60).padStart(2, '0');
}

async function apiDemo(url, opcoes) {
    const u = new URL(url, 'http://demo');
    const p = u.searchParams;
    const metodo = (opcoes && opcoes.method) || 'GET';

    if (u.pathname === '/api/servicos') return demo.servicos;
    if (u.pathname === '/api/barbeiros') return demo.barbeiros;

    if (u.pathname === '/api/agendamentos/horarios') {
        const servico = demo.servicos.find((s) => s.id == p.get('servicoId'));
        const data = p.get('data');
        if (new Date(data + 'T12:00').getDay() === 0) return [];
        const ocupados = demo.agendamentos.filter((a) => a.data === data && a.barbeiroId == p.get('barbeiroId'));
        const livres = [];
        for (let m = 9 * 60; m + servico.duracaoMinutos <= 19 * 60; m += 30) {
            const fim = m + servico.duracaoMinutos;
            if (m < 13 * 60 && fim > 12 * 60) continue; // almoço
            const bate = ocupados.some((a) => m < a.fimMin && a.iniMin < fim);
            if (!bate) livres.push(hhmm(m));
        }
        return livres;
    }

    if (u.pathname === '/api/agendamentos' && metodo === 'GET') {
        const data = p.get('data');
        return demo.agendamentos.filter((a) => a.data === data).sort((a, b) => a.iniMin - b.iniMin);
    }

    if (u.pathname === '/api/agendamentos' && metodo === 'POST') {
        const d = JSON.parse(opcoes.body);
        const servico = demo.servicos.find((s) => s.id === d.servicoId);
        const barbeiro = demo.barbeiros.find((b) => b.id === d.barbeiroId);
        const [h, m] = d.hora.split(':').map(Number);
        const novo = {
            id: demo.proximoId++, barbeiroId: d.barbeiroId, barbeiro: barbeiro.nome, servico: servico.nome,
            clienteNome: d.clienteNome, clienteTelefone: d.clienteTelefone, data: d.data, hora: d.hora,
            iniMin: h * 60 + m, fimMin: h * 60 + m + servico.duracaoMinutos
        };
        demo.agendamentos.push(novo);
        return novo;
    }

    if (metodo === 'DELETE') {
        const id = Number(u.pathname.split('/').pop());
        demo.agendamentos = demo.agendamentos.filter((a) => a.id !== id);
        return null;
    }
    throw new Error('Rota de demonstração não encontrada.');
}

// ---------------------------------------------------------------
// Estado da tela
// ---------------------------------------------------------------
const estado = { servico: null, barbeiro: null, hora: null };
const $ = (id) => document.getElementById(id);

// ---------------------------------------------------------------
// Utilidades
// ---------------------------------------------------------------
async function chamarApi(url, opcoes) {
    if (MODO_DEMO) return apiDemo(url, opcoes);

    let resposta;
    try {
        resposta = await fetch(url, opcoes);
    } catch (e) {
        throw new Error('Não consegui falar com o servidor. Confira se o app está rodando.');
    }
    if (resposta.status === 204) return null;
    let corpo = {};
    try { corpo = await resposta.json(); } catch (e) { /* sem corpo */ }
    if (!resposta.ok) throw new Error(corpo.message || 'Erro inesperado. Tente novamente.');
    return corpo;
}

function paraIso(d) {
    return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
}
function dataDeIso(iso) { return new Date(iso + 'T12:00:00'); } // meio-dia evita erro de fuso
function somarDias(iso, n) { const d = dataDeIso(iso); d.setDate(d.getDate() + n); return paraIso(d); }

function formatarPreco(valor) {
    return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}
function formatarData(iso) {
    const p = iso.split('-');
    return p[2] + '/' + p[1] + '/' + p[0];
}
function dataExtenso(iso) {
    return dataDeIso(iso).toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' });
}

function mostrarMensagem(texto, tipo) {
    const el = $('mensagem');
    el.textContent = texto;
    el.className = 'mensagem ' + tipo;
    el.hidden = false;
}
function esconderMensagem() { $('mensagem').hidden = true; }

// ---------------------------------------------------------------
// Ícones (SVG simples, sem imagens externas)
// ---------------------------------------------------------------
const SVG_BASE = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">';
const ICONES = {
    corte: SVG_BASE + '<circle cx="6" cy="18" r="3"/><circle cx="18" cy="18" r="3"/><path d="M8 16L19 3M16 16L5 3"/></svg>',
    barba: SVG_BASE + '<path d="M4 8c0 7 3 12 8 12s8-5 8-12"/><path d="M8 13c1.5 1.2 6.5 1.2 8 0"/><path d="M4 8c2-3 5-4 8-4s6 1 8 4"/></svg>',
    combo: SVG_BASE + '<circle cx="6" cy="19" r="2.5"/><circle cx="12" cy="19" r="2.5"/><path d="M7.5 17L15 4M10.5 17L3 4"/><path d="M16 9c0 5 1.5 8 4 8"/></svg>',
    sobrancelha: SVG_BASE + '<path d="M3 14c3-5 9-6 18-2"/><path d="M3 18c4-2 9-2 12-1"/></svg>'
};
function iconeDoServico(nome) {
    const n = nome.toLowerCase();
    if (n.includes('+')) return ICONES.combo;
    if (n.includes('barba')) return ICONES.barba;
    if (n.includes('sobrancelha')) return ICONES.sobrancelha;
    return ICONES.corte;
}

// ---------------------------------------------------------------
// Serviços e barbeiros
// ---------------------------------------------------------------
function criarCard(iconeHtml, titulo, subtitulo, aoClicar) {
    const botao = document.createElement('button');
    botao.type = 'button';
    botao.className = 'card';

    const icone = document.createElement('span');
    icone.className = 'icone';
    icone.innerHTML = iconeHtml; // HTML fixo, criado aqui no código (nunca vem do usuário)

    const texto = document.createElement('span');
    const strong = document.createElement('strong');
    strong.textContent = titulo;
    texto.appendChild(strong);
    if (subtitulo) {
        const small = document.createElement('small');
        small.textContent = subtitulo;
        texto.appendChild(small);
    }
    botao.appendChild(icone);
    botao.appendChild(texto);
    botao.addEventListener('click', () => aoClicar(botao));
    return botao;
}

function selecionarCard(container, botao) {
    container.querySelectorAll('.card').forEach((c) => c.classList.remove('selecionado'));
    botao.classList.add('selecionado');
}

async function carregarServicos() {
    const lista = await chamarApi('/api/servicos');
    const container = $('lista-servicos');
    container.innerHTML = '';
    lista.forEach((s) => {
        const detalhe = s.duracaoMinutos + ' min · ' + formatarPreco(s.preco);
        container.appendChild(criarCard(iconeDoServico(s.nome), s.nome, detalhe, (botao) => {
            estado.servico = s;
            selecionarCard(container, botao);
            atualizarHorarios();
        }));
    });
}

async function carregarBarbeiros() {
    const lista = await chamarApi('/api/barbeiros');
    const container = $('lista-barbeiros');
    container.innerHTML = '';
    lista.forEach((b) => {
        const avatar = '<span class="avatar">' + b.nome.charAt(0).toUpperCase() + '</span>';
        const card = criarCard(avatar, b.nome, 'Barbeiro', (botao) => {
            estado.barbeiro = b;
            selecionarCard(container, botao);
            atualizarHorarios();
        });
        card.querySelector('.icone').style.background = 'transparent';
        container.appendChild(card);
    });
}

// ---------------------------------------------------------------
// Dias (faixa) e horários
// ---------------------------------------------------------------
const DIAS_CURTOS = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'];

function montarFaixaDias() {
    const faixa = $('faixa-dias');
    faixa.innerHTML = '';
    const hoje = paraIso(new Date());
    for (let i = 0; i < 14; i++) {
        const iso = somarDias(hoje, i);
        const d = dataDeIso(iso);
        const botao = document.createElement('button');
        botao.type = 'button';
        botao.className = 'dia';
        botao.dataset.data = iso;
        botao.disabled = d.getDay() === 0; // domingo fechado

        const sem = document.createElement('small');
        sem.textContent = i === 0 ? 'hoje' : DIAS_CURTOS[d.getDay()];
        const num = document.createElement('b');
        num.textContent = d.getDate();
        botao.appendChild(sem);
        botao.appendChild(num);
        botao.addEventListener('click', () => {
            $('campo-data').value = iso;
            atualizarHorarios();
        });
        faixa.appendChild(botao);
    }
}

function marcarDiaNaFaixa() {
    const valor = $('campo-data').value;
    document.querySelectorAll('#faixa-dias .dia').forEach((b) => {
        b.classList.toggle('selecionado', b.dataset.data === valor);
    });
}

async function atualizarHorarios() {
    estado.hora = null;
    marcarDiaNaFaixa();
    atualizarResumo();

    const lista = $('lista-horarios');
    const aviso = $('aviso-horarios');
    lista.innerHTML = '';

    const data = $('campo-data').value;
    if (!estado.servico || !estado.barbeiro || !data) {
        aviso.textContent = 'Selecione serviço, barbeiro e data.';
        return;
    }

    aviso.textContent = 'Carregando horários...';
    try {
        const url = '/api/agendamentos/horarios?barbeiroId=' + estado.barbeiro.id
            + '&servicoId=' + estado.servico.id + '&data=' + data;
        const horarios = await chamarApi(url);

        if (data !== $('campo-data').value) return; // o usuário já trocou de dia
        if (horarios.length === 0) {
            aviso.textContent = 'Sem horários livres neste dia (domingo é fechado). Tente outro dia.';
            return;
        }
        aviso.textContent = 'Escolha um horário:';
        horarios.forEach((h) => {
            const botao = document.createElement('button');
            botao.type = 'button';
            botao.className = 'horario';
            botao.textContent = h;
            botao.addEventListener('click', () => {
                estado.hora = h;
                lista.querySelectorAll('.horario').forEach((x) => x.classList.remove('selecionado'));
                botao.classList.add('selecionado');
                atualizarResumo();
            });
            lista.appendChild(botao);
        });
    } catch (erro) {
        aviso.textContent = erro.message;
    }
}

// ---------------------------------------------------------------
// Comanda (resumo) e validação
// ---------------------------------------------------------------
function definir(id, texto) {
    const el = $(id);
    el.textContent = texto || '—';
    el.classList.toggle('vazio', !texto);
}

function formularioValido() {
    return Boolean(estado.servico && estado.barbeiro && estado.hora && $('campo-data').value
        && $('campo-nome').value.trim().length >= 2
        && $('campo-telefone').value.replace(/\D/g, '').length >= 10);
}

function atualizarResumo() {
    const data = $('campo-data').value;
    definir('res-servico', estado.servico && estado.servico.nome);
    definir('res-barbeiro', estado.barbeiro && estado.barbeiro.nome);
    definir('res-data', data && dataExtenso(data));
    definir('res-hora', estado.hora);
    $('res-total').textContent = formatarPreco(estado.servico ? estado.servico.preco : 0);

    const ok = formularioValido();
    $('btn-confirmar').disabled = !ok;
    $('dica-confirmar').hidden = ok;

    // marca os blocos concluídos (número fica verde)
    const blocos = document.querySelectorAll('#agendar .bloco');
    const feitos = [
        Boolean(estado.servico), Boolean(estado.barbeiro), Boolean(estado.hora),
        $('campo-nome').value.trim().length >= 2 && $('campo-telefone').value.replace(/\D/g, '').length >= 10
    ];
    blocos.forEach((b, i) => b.classList.toggle('completo', feitos[i]));
}

function mascararTelefone() {
    let d = $('campo-telefone').value.replace(/\D/g, '').slice(0, 11);
    let t = d;
    if (d.length > 6) {
        const meio = d.length === 11 ? 7 : 6;
        t = '(' + d.slice(0, 2) + ') ' + d.slice(2, meio) + '-' + d.slice(meio);
    } else if (d.length > 2) {
        t = '(' + d.slice(0, 2) + ') ' + d.slice(2);
    }
    $('campo-telefone').value = t;
}

// ---------------------------------------------------------------
// Confirmação
// ---------------------------------------------------------------
async function confirmarAgendamento() {
    esconderMensagem();
    const botao = $('btn-confirmar');
    botao.disabled = true;
    botao.textContent = 'Agendando...';

    const dados = {
        servicoId: estado.servico.id,
        barbeiroId: estado.barbeiro.id,
        data: $('campo-data').value,
        hora: estado.hora,
        clienteNome: $('campo-nome').value.trim(),
        clienteTelefone: $('campo-telefone').value.trim()
    };

    try {
        const criado = await chamarApi('/api/agendamentos', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(dados)
        });
        $('dialogo-texto').textContent = criado.servico + ' com ' + criado.barbeiro
            + ', ' + dataExtenso(criado.data) + ' às ' + criado.hora + '.';
        abrirDialogo();
        limparFormulario();
    } catch (erro) {
        mostrarMensagem(erro.message, 'erro');
        await atualizarHorarios(); // o horário pode ter sido ocupado por outra pessoa
    }
    botao.textContent = 'Confirmar agendamento';
    atualizarResumo();
}

function limparFormulario() {
    $('campo-nome').value = '';
    $('campo-telefone').value = '';
    estado.hora = null;
    atualizarHorarios();
}

function abrirDialogo() {
    const d = $('dialogo');
    if (typeof d.showModal === 'function') d.showModal(); else d.setAttribute('open', '');
}
function fecharDialogo() {
    const d = $('dialogo');
    if (typeof d.close === 'function') d.close(); else d.removeAttribute('open');
    window.scrollTo({ top: 0 });
}

// ---------------------------------------------------------------
// Inicialização
// ---------------------------------------------------------------
document.addEventListener('DOMContentLoaded', async () => {
    if (MODO_DEMO) $('faixa-demo').hidden = false;

    // poste de barbeiro parado para quem prefere menos movimento
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        document.querySelectorAll('svg').forEach((s) => s.pauseAnimations && s.pauseAnimations());
    }

    const hoje = paraIso(new Date());
    // se hoje é domingo, começa na segunda
    const inicio = dataDeIso(hoje).getDay() === 0 ? somarDias(hoje, 1) : hoje;

    $('campo-data').min = hoje;
    $('campo-data').value = inicio;
    montarFaixaDias();
    marcarDiaNaFaixa();

    $('campo-data').addEventListener('change', atualizarHorarios);
    $('campo-nome').addEventListener('input', atualizarResumo);
    $('campo-telefone').addEventListener('input', () => { mascararTelefone(); atualizarResumo(); });
    $('btn-confirmar').addEventListener('click', confirmarAgendamento);
    $('dialogo-fechar').addEventListener('click', fecharDialogo);

    atualizarResumo();

    try {
        await Promise.all([carregarServicos(), carregarBarbeiros()]);
    } catch (erro) {
        mostrarMensagem('Não foi possível carregar os dados: ' + erro.message, 'erro');
    }
});
