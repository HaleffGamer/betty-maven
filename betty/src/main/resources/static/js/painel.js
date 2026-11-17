'use strict';

const $ = (id) => document.getElementById(id);
let usuario = null;          // { id, nome, papel, barbeiroId }
let barbeirosAdmin = [];

// ---------------------------------------------------------------
// Utilidades
// ---------------------------------------------------------------
class ErroSessao extends Error {}

async function api(url, opcoes) {
    let resposta;
    try {
        resposta = await fetch(url, opcoes);
    } catch (e) {
        throw new Error('Não consegui falar com o servidor. Confira se o app está rodando.');
    }
    if (resposta.status === 204) return null;
    let corpo = {};
    try { corpo = await resposta.json(); } catch (e) { /* sem corpo */ }
    if (resposta.status === 401 && !url.startsWith('/api/auth/login')) {
        throw new ErroSessao(corpo.message || 'Sessão expirada.');
    }
    if (!resposta.ok) throw new Error(corpo.message || 'Erro inesperado. Tente novamente.');
    return corpo;
}
function enviar(metodo, url, dados) {
    const opcoes = { method: metodo };
    if (dados !== undefined) {
        opcoes.headers = { 'Content-Type': 'application/json' };
        opcoes.body = JSON.stringify(dados);
    }
    return api(url, opcoes);
}

function paraIso(d) {
    return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
}
function dataDeIso(iso) { return new Date(iso + 'T12:00:00'); }
function somarDias(iso, n) { const d = dataDeIso(iso); d.setDate(d.getDate() + n); return paraIso(d); }
function dataExtenso(iso) {
    return dataDeIso(iso).toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' });
}
function dinheiro(v) { return Number(v).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }); }

function el(tag, classe, texto) {
    const e = document.createElement(tag);
    if (classe) e.className = classe;
    if (texto !== undefined) e.textContent = texto;
    return e;
}

let temporizador;
function aviso(texto, tipo) {
    const m = $('msg-painel');
    m.textContent = texto;
    m.className = 'mensagem msg-painel ' + (tipo || 'ok');
    m.hidden = false;
    clearTimeout(temporizador);
    temporizador = setTimeout(() => { m.hidden = true; }, 5000);
}

/** Roda uma ação do painel; se a sessão caiu, volta para o login; senão mostra o erro. */
async function seguro(acao) {
    try {
        await acao();
    } catch (e) {
        if (e instanceof ErroSessao) { mostrarLogin('Sua sessão expirou. Entre novamente.'); return; }
        aviso(e.message, 'erro');
    }
}

// ---------------------------------------------------------------
// Telas
// ---------------------------------------------------------------
function mostrarLogin(mensagem) {
    usuario = null;
    $('tela-painel').hidden = true;
    $('tela-login').hidden = false;
    const erro = $('login-erro');
    erro.hidden = !mensagem;
    erro.textContent = mensagem || '';
    $('login-senha').value = '';
}

function mostrarPainel() {
    $('tela-login').hidden = true;
    $('tela-painel').hidden = false;
    $('usuario-nome').textContent = usuario.nome;
    const dono = usuario.papel === 'DONO';
    const selo = $('usuario-papel');
    selo.textContent = dono ? 'Dono' : 'Barbeiro';
    selo.classList.toggle('barbeiro', !dono);
    document.querySelectorAll('.so-dono').forEach((b) => { b.hidden = !dono; });
    $('filtro-barbeiro').hidden = !dono;
    irParaAba('aba-agenda');
    if (!$('agenda-data').value) $('agenda-data').value = paraIso(new Date());
    seguro(async () => {
        if (dono) await carregarBarbeirosAdmin();
        await carregarAgenda();
    });
}

function irParaAba(id) {
    document.querySelectorAll('.aba').forEach((b) => b.classList.toggle('ativa', b.dataset.aba === id));
    document.querySelectorAll('main .painel').forEach((p) => p.classList.toggle('ativo', p.id === id));
    if (id === 'aba-equipe') seguro(carregarBarbeirosAdmin);
    if (id === 'aba-servicos') seguro(carregarServicosAdmin);
    if (id === 'aba-agenda') seguro(carregarAgenda);
}

// ---------------------------------------------------------------
// Login / logout
// ---------------------------------------------------------------
async function entrar(evento) {
    evento.preventDefault();
    const botao = $('btn-entrar');
    botao.disabled = true;
    botao.textContent = 'Entrando...';
    try {
        usuario = await enviar('POST', '/api/auth/login', {
            login: $('login-usuario').value.trim(),
            senha: $('login-senha').value
        });
        $('login-erro').hidden = true;
        mostrarPainel();
    } catch (e) {
        mostrarLogin(e.message);
    }
    botao.disabled = false;
    botao.textContent = 'Entrar';
}

async function sair() {
    try { await enviar('POST', '/api/auth/logout'); } catch (e) { /* tanto faz */ }
    mostrarLogin();
}

// ---------------------------------------------------------------
// Agenda
// ---------------------------------------------------------------
async function carregarAgenda() {
    const data = $('agenda-data').value;
    const lista = $('lista-agenda');
    lista.innerHTML = '';
    $('agenda-titulo').textContent = data ? dataExtenso(data) : 'Agenda do dia';
    if (!data) return;

    let url = '/api/painel/agenda?data=' + data;
    const filtro = $('agenda-barbeiro').value;
    if (usuario.papel === 'DONO' && filtro) url += '&barbeiroId=' + encodeURIComponent(filtro);

    const itens = await api(url);
    const total = itens.reduce((soma, a) => soma + Number(a.preco), 0);
    $('res-qtd').textContent = itens.length;
    $('res-valor').textContent = dinheiro(total);
    $('agenda-resumo').textContent = itens.length === 0 ? 'Nenhum horário marcado'
        : itens.length + (itens.length === 1 ? ' horário marcado' : ' horários marcados');

    if (itens.length === 0) {
        const vazio = el('div', 'vazio-agenda');
        vazio.appendChild(el('strong', '', 'Dia livre por enquanto'));
        vazio.appendChild(el('span', '', 'Quando alguém agendar, aparece aqui.'));
        lista.appendChild(vazio);
        return;
    }
    itens.forEach((a) => lista.appendChild(itemAgenda(a)));
}

function itemAgenda(a) {
    const item = el('div', 'item-agenda');
    item.appendChild(el('span', 'hora', a.hora));

    const info = el('div', 'info');
    info.appendChild(el('strong', '', a.clienteNome));
    const partes = [a.servico + ' (' + a.duracaoMinutos + ' min)', dinheiro(a.preco)];
    if (usuario.papel === 'DONO') partes.push(a.barbeiro);
    partes.push(a.clienteTelefone);
    info.appendChild(el('small', '', partes.join(' · ')));
    item.appendChild(info);

    const cancelar = el('button', 'btn-cancelar', 'Cancelar');
    cancelar.type = 'button';
    cancelar.addEventListener('click', () => {
        if (!confirm('Cancelar o agendamento de ' + a.clienteNome + ' às ' + a.hora + '?')) return;
        seguro(async () => {
            await enviar('DELETE', '/api/painel/agendamentos/' + a.id);
            aviso('Agendamento cancelado.');
            await carregarAgenda();
        });
    });
    item.appendChild(cancelar);
    return item;
}

function mudarDia(n) {
    $('agenda-data').value = somarDias($('agenda-data').value || paraIso(new Date()), n);
    seguro(carregarAgenda);
}

// ---------------------------------------------------------------
// Equipe (dono)
// ---------------------------------------------------------------
async function carregarBarbeirosAdmin() {
    barbeirosAdmin = await api('/api/painel/admin/barbeiros');

    // filtro da agenda
    const sel = $('agenda-barbeiro');
    const atual = sel.value;
    sel.innerHTML = '';
    const todos = el('option', '', 'Todos');
    todos.value = '';
    sel.appendChild(todos);
    barbeirosAdmin.forEach((b) => {
        const o = el('option', '', b.nome + (b.ativo ? '' : ' (inativo)'));
        o.value = b.id;
        sel.appendChild(o);
    });
    sel.value = atual;

    // lista
    const lista = $('lista-equipe');
    lista.innerHTML = '';
    barbeirosAdmin.forEach((b) => {
        const linha = el('div', 'linha-admin' + (b.ativo ? '' : ' inativo'));
        const info = el('div', 'info');
        info.appendChild(el('strong', '', b.nome));
        info.appendChild(el('small', '', b.ativo ? 'Ativo — aparece para os clientes' : 'Inativo — não recebe agendamentos nem faz login'));
        const botao = el('button', 'btn-sec', b.ativo ? 'Desativar' : 'Reativar');
        botao.type = 'button';
        botao.addEventListener('click', () => seguro(async () => {
            await enviar('PUT', '/api/painel/admin/barbeiros/' + b.id + '/ativo?valor=' + (!b.ativo));
            await carregarBarbeirosAdmin();
        }));
        linha.appendChild(info);
        linha.appendChild(botao);
        lista.appendChild(linha);
    });
}

async function adicionarBarbeiro(evento) {
    evento.preventDefault();
    await seguro(async () => {
        await enviar('POST', '/api/painel/admin/barbeiros', {
            nome: $('nb-nome').value, login: $('nb-login').value, senha: $('nb-senha').value
        });
        $('form-barbeiro').reset();
        aviso('Barbeiro adicionado.');
        await carregarBarbeirosAdmin();
    });
}

// ---------------------------------------------------------------
// Serviços (dono)
// ---------------------------------------------------------------
async function carregarServicosAdmin() {
    const servicos = await api('/api/painel/admin/servicos');
    const lista = $('lista-servicos-admin');
    lista.innerHTML = '';
    servicos.forEach((s) => {
        const linha = el('div', 'linha-admin linha-servico' + (s.ativo ? '' : ' inativo'));

        const nome = el('input', 'campo nome-servico');
        nome.type = 'text'; nome.value = s.nome; nome.maxLength = 60;
        nome.setAttribute('aria-label', 'Nome do serviço');
        const duracao = el('input');
        duracao.type = 'number'; duracao.min = 10; duracao.max = 240; duracao.step = 5; duracao.value = s.duracaoMinutos;
        duracao.setAttribute('aria-label', 'Duração em minutos');
        const preco = el('input');
        preco.type = 'number'; preco.min = 1; preco.step = '0.01'; preco.value = Number(s.preco).toFixed(2);
        preco.setAttribute('aria-label', 'Preço em reais');

        const acoes = el('div', 'acoes');
        const salvar = el('button', 'btn-sec', 'Salvar');
        salvar.type = 'button';
        salvar.addEventListener('click', () => seguro(async () => {
            await enviar('PUT', '/api/painel/admin/servicos/' + s.id, {
                nome: nome.value, duracaoMinutos: Number(duracao.value), preco: Number(preco.value)
            });
            aviso('Serviço atualizado.');
            await carregarServicosAdmin();
        }));
        const ativo = el('button', 'btn-sec', s.ativo ? 'Desativar' : 'Reativar');
        ativo.type = 'button';
        ativo.addEventListener('click', () => seguro(async () => {
            await enviar('PUT', '/api/painel/admin/servicos/' + s.id + '/ativo?valor=' + (!s.ativo));
            await carregarServicosAdmin();
        }));
        acoes.appendChild(salvar);
        acoes.appendChild(ativo);

        linha.appendChild(nome);
        linha.appendChild(duracao);
        linha.appendChild(preco);
        linha.appendChild(acoes);
        lista.appendChild(linha);
    });
}

async function adicionarServico(evento) {
    evento.preventDefault();
    await seguro(async () => {
        await enviar('POST', '/api/painel/admin/servicos', {
            nome: $('ns-nome').value,
            duracaoMinutos: Number($('ns-duracao').value),
            preco: Number($('ns-preco').value)
        });
        $('form-servico').reset();
        $('ns-duracao').value = 30;
        aviso('Serviço adicionado.');
        await carregarServicosAdmin();
    });
}

// ---------------------------------------------------------------
// Inicialização
// ---------------------------------------------------------------
document.addEventListener('DOMContentLoaded', async () => {
    document.querySelectorAll('.aba').forEach((b) => b.addEventListener('click', () => irParaAba(b.dataset.aba)));
    $('form-login').addEventListener('submit', entrar);
    $('btn-sair').addEventListener('click', sair);
    $('dia-anterior').addEventListener('click', () => mudarDia(-1));
    $('dia-seguinte').addEventListener('click', () => mudarDia(1));
    $('agenda-data').addEventListener('change', () => seguro(carregarAgenda));
    $('agenda-barbeiro').addEventListener('change', () => seguro(carregarAgenda));
    $('form-barbeiro').addEventListener('submit', adicionarBarbeiro);
    $('form-servico').addEventListener('submit', adicionarServico);
    $('agenda-data').value = paraIso(new Date());

    if (location.protocol === 'file:') {
        mostrarLogin('Esta página precisa do servidor rodando. Abra http://localhost:8080/painel');
        return;
    }
    try {
        usuario = await api('/api/auth/me');   // já está logado?
        mostrarPainel();
    } catch (e) {
        mostrarLogin();
    }
});
