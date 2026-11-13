// =========================================================
// Início: visão geral. As contagens usam o totalElementos da listagem
// paginada (GET /chamados?status=X&size=1), sem precisar de rota nova na API.
// =========================================================
import { api } from '../api.js';
import { esc, montarHash } from '../ui.js';
import { STATUS, PRIORIDADES } from '../dominio.js';
import { tabelaChamados, abrirFormChamado } from './chamados.js';

const COR_STATUS = {
  ABERTO: 'var(--primaria)',
  EM_ATENDIMENTO: 'var(--aviso)',
  AGUARDANDO_CLIENTE: 'var(--roxo)',
  FECHADO: 'var(--sucesso)',
};

const COR_PRIORIDADE = {
  BAIXA: 'var(--cinza)',
  MEDIA: 'var(--primaria)',
  ALTA: 'var(--aviso)',
  CRITICA: 'var(--perigo)',
};

function indicador({ rotulo, valor, href, cor }) {
  return `<a class="indicador" href="${href}" style="--cor:${cor}"><span>${esc(rotulo)}</span><strong>${valor}</strong></a>`;
}

function primeirosPassos({ clientes, categorias, tecnicos }) {
  const passo = (feito, texto, href) => `<li>${feito ? '✅' : '⬜'} <a href="${href}">${texto}</a></li>`;
  return `
    <div class="cartao" style="margin-bottom:20px">
      <div class="cartao-cabecalho"><h2>Primeiros passos</h2></div>
      <div class="cartao-corpo">
        <p style="margin-top:0" class="texto-suave">O banco H2 é em memória e começa vazio a cada reinício da API. Para abrir um chamado, cadastre:</p>
        <ul class="passos" style="list-style:none;padding:0">
          ${passo(clientes > 0, 'Um cliente', '#/clientes')}
          ${passo(categorias > 0, 'Uma categoria', '#/categorias')}
          ${passo(tecnicos > 0, 'Um técnico', '#/tecnicos')}
          ${passo(false, 'Depois, abra o primeiro chamado', '#/chamados')}
        </ul>
      </div>
    </div>`;
}

export async function render(el) {
  const chavesStatus = Object.keys(STATUS);
  const chavesPrioridade = Object.keys(PRIORIDADES);

  const [porStatus, porPrioridade, recentes, clientes, categorias, tecnicos] = await Promise.all([
    Promise.all(chavesStatus.map((status) => api.chamados.listar({ status, size: 1 }))),
    Promise.all(chavesPrioridade.map((prioridade) => api.chamados.listar({ prioridade, size: 1 }))),
    api.chamados.listar({ size: 5 }),
    api.clientes.listar(),
    api.categorias.listar(),
    api.tecnicos.listar(),
  ]);

  const total = recentes.totalElementos;
  const qtd = { clientes: clientes.length, categorias: categorias.length, tecnicos: tecnicos.length };

  el.innerHTML = `
    <div class="cabecalho">
      <div>
        <h1>Início</h1>
        <p>Visão geral dos chamados de suporte.</p>
      </div>
      <div class="acoes"><button class="btn btn-primario" data-novo>+ Novo chamado</button></div>
    </div>

    ${total === 0 ? primeirosPassos(qtd) : ''}

    <div class="secao-titulo">Chamados por status</div>
    <div class="indicadores">
      ${indicador({ rotulo: 'Total', valor: total, href: '#/chamados', cor: 'var(--texto-suave)' })}
      ${chavesStatus.map((s, i) => indicador({
    rotulo: STATUS[s], valor: porStatus[i].totalElementos, href: montarHash('/chamados', { status: s }), cor: COR_STATUS[s],
  })).join('')}
    </div>

    <div class="secao-titulo">Chamados por prioridade</div>
    <div class="indicadores">
      ${chavesPrioridade.map((p, i) => indicador({
    rotulo: PRIORIDADES[p], valor: porPrioridade[i].totalElementos, href: montarHash('/chamados', { prioridade: p }), cor: COR_PRIORIDADE[p],
  })).join('')}
    </div>

    <div class="secao-titulo">Cadastros</div>
    <div class="indicadores">
      ${indicador({ rotulo: 'Clientes', valor: qtd.clientes, href: '#/clientes', cor: 'var(--primaria)' })}
      ${indicador({ rotulo: 'Categorias', valor: qtd.categorias, href: '#/categorias', cor: 'var(--primaria)' })}
      ${indicador({ rotulo: 'Técnicos', valor: qtd.tecnicos, href: '#/tecnicos', cor: 'var(--primaria)' })}
    </div>

    ${total > 0 ? `
    <div class="cartao">
      <div class="cartao-cabecalho">
        <h2>Últimos chamados</h2>
        <a href="#/chamados">Ver todos →</a>
      </div>
      ${tabelaChamados(recentes.conteudo)}
    </div>` : ''}`;

  el.querySelector('[data-novo]').addEventListener('click', () => abrirFormChamado());
}
