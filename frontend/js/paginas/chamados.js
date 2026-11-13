// =========================================================
// Tela de Chamados: GET /chamados com filtros, paginação e ordenação.
// Os filtros ficam na URL (#/chamados?status=ABERTO&page=1), então o
// botão "voltar" do navegador e os links do Início funcionam.
// Também exporta a tabela e o formulário, usados em outras telas.
// =========================================================
import { api } from '../api.js';
import {
  esc, formatarData, etiquetaStatus, etiquetaPrioridade, vazio, opcoesHtml, abrirModal,
  abrirFormulario, mostrarErro, toast, irPara, recarregar, paginacaoHtml, ligarPaginacao, lerOrdenacao,
} from '../ui.js';
import {
  STATUS, PRIORIDADES, PERFIS, CAMPOS_ORDENACAO, ORDENACAO_PADRAO,
} from '../dominio.js';

// ---------- Filtros <-> URL ----------

function inteiro(valor, padrao) {
  const n = Number.parseInt(valor, 10);
  return Number.isNaN(n) || n < 0 ? padrao : n;
}

/** Lê os filtros da query da URL, aplicando os mesmos padrões da API. */
export function lerFiltros(query) {
  return {
    status: query.status ?? '',
    prioridade: query.prioridade ?? '',
    clienteId: query.clienteId ?? '',
    page: inteiro(query.page, 0),
    size: inteiro(query.size, 10) || 10,
    sort: query.sort || ORDENACAO_PADRAO,
  };
}

/** Tira da URL o que já é padrão, para ela ficar curta. */
export function paramsDaUrl(f) {
  return {
    status: f.status,
    prioridade: f.prioridade,
    clienteId: f.clienteId,
    page: f.page > 0 ? f.page : '',
    size: f.size !== 10 ? f.size : '',
    sort: f.sort !== ORDENACAO_PADRAO ? f.sort : '',
  };
}

export const FILTROS_LIMPOS = { status: '', prioridade: '', clienteId: '', sort: ORDENACAO_PADRAO, page: 0 };

export function filtrosHtml(f, { clientes = null } = {}) {
  const { campo, direcao } = lerOrdenacao(f.sort);
  const temFiltro = f.status || f.prioridade || f.clienteId || f.sort !== ORDENACAO_PADRAO;
  return `
    <form class="filtros" data-filtros>
      <div class="campo">
        <label for="f-status">Status</label>
        <select id="f-status" name="status"><option value="">Todos</option>${opcoesHtml(STATUS, f.status)}</select>
      </div>
      <div class="campo">
        <label for="f-prioridade">Prioridade</label>
        <select id="f-prioridade" name="prioridade"><option value="">Todas</option>${opcoesHtml(PRIORIDADES, f.prioridade)}</select>
      </div>
      ${clientes ? `
      <div class="campo">
        <label for="f-cliente">Cliente</label>
        <select id="f-cliente" name="clienteId"><option value="">Todos</option>${opcoesHtml(clientes.map((c) => ({ valor: c.id, rotulo: c.nome })), f.clienteId)}</select>
      </div>` : ''}
      <div class="campo">
        <label for="f-ordem">Ordenar por</label>
        <select id="f-ordem" name="campo">${opcoesHtml(CAMPOS_ORDENACAO, campo)}</select>
      </div>
      <div class="campo">
        <label for="f-direcao">Direção</label>
        <select id="f-direcao" name="direcao">${opcoesHtml({ desc: 'Decrescente', asc: 'Crescente' }, direcao)}</select>
      </div>
      <button type="button" class="btn" data-limpar${temFiltro ? '' : ' disabled'}>Limpar filtros</button>
    </form>`;
}

export function ligarFiltros(el, mudar) {
  const form = el.querySelector('[data-filtros]');
  form.addEventListener('change', () => {
    const campos = form.elements;
    mudar({
      status: campos.status.value,
      prioridade: campos.prioridade.value,
      ...(campos.clienteId ? { clienteId: campos.clienteId.value } : {}),
      sort: `${campos.campo.value},${campos.direcao.value}`,
      page: 0,
    });
  });
  form.querySelector('[data-limpar]').addEventListener('click', () => mudar(FILTROS_LIMPOS));
}

// ---------- Tabela ----------

/** Tabela de chamados. Com `sort`, os cabeçalhos ficam clicáveis para ordenar. */
export function tabelaChamados(chamados, { sort = null, mostrarCliente = true } = {}) {
  const ord = sort ? lerOrdenacao(sort) : null;
  const th = (campo, rotulo, classe = '') => {
    if (!ord || !campo) return `<th class="${classe}">${rotulo}</th>`;
    const ativo = ord.campo === campo;
    const seta = ativo ? (ord.direcao === 'desc' ? '↓' : '↑') : '';
    const ariaSort = ativo ? (ord.direcao === 'desc' ? 'descending' : 'ascending') : 'none';
    return `<th class="ordenavel ${classe}" data-ordenar="${campo}" tabindex="0" aria-sort="${ariaSort}" title="Ordenar por ${rotulo}">${rotulo} <span class="seta">${seta}</span></th>`;
  };

  return `
    <div class="tabela-wrap">
      <table>
        <thead><tr>
          ${th('id', 'Nº')}
          ${th('titulo', 'Título')}
          ${mostrarCliente ? th(null, 'Cliente', 'esconder-celular') : ''}
          ${th(null, 'Técnico', 'esconder-celular')}
          ${th('prioridade', 'Prioridade')}
          ${th('status', 'Status')}
          ${th('dataAbertura', 'Abertura', 'esconder-celular')}
        </tr></thead>
        <tbody>
          ${chamados.map((c) => `
            <tr data-href="#/chamados/${c.id}">
              <td class="num">${c.id}</td>
              <td class="titulo-celula">
                <a href="#/chamados/${c.id}">${esc(c.titulo)}</a>
                <div class="texto-suave" style="font-size:.82rem">${esc(c.categoriaNome)}</div>
              </td>
              ${mostrarCliente ? `<td class="esconder-celular">${esc(c.clienteNome)}</td>` : ''}
              <td class="esconder-celular">${esc(c.tecnicoNome)}</td>
              <td>${etiquetaPrioridade(c.prioridade)}</td>
              <td>${etiquetaStatus(c.status)}</td>
              <td class="esconder-celular num">${formatarData(c.dataAbertura)}</td>
            </tr>`).join('')}
        </tbody>
      </table>
    </div>`;
}

/** Clique no cabeçalho: mesma coluna inverte a direção; coluna nova começa asc (datas: desc). */
export function ligarOrdenacao(el, sort, mudar) {
  const ord = lerOrdenacao(sort);
  el.querySelectorAll('[data-ordenar]').forEach((th) => {
    const ordenar = () => {
      const campo = th.dataset.ordenar;
      let direcao;
      if (campo === ord.campo) direcao = ord.direcao === 'asc' ? 'desc' : 'asc';
      else direcao = campo.startsWith('data') ? 'desc' : 'asc';
      mudar(`${campo},${direcao}`);
    };
    th.addEventListener('click', ordenar);
    th.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); ordenar(); }
    });
  });
}

// ---------- Formulário (novo / editar) ----------

/**
 * Abre o formulário de chamado.
 * Sem `chamado` -> POST /chamados e vai para o detalhe do chamado criado.
 * Com `chamado` -> PUT /chamados/{id} e recarrega a tela.
 */
export async function abrirFormChamado({ chamado = null, clienteId = null } = {}) {
  let listas;
  try {
    listas = await Promise.all([api.clientes.listar(), api.categorias.listar(), api.tecnicos.listar()]);
  } catch (erro) {
    mostrarErro(erro);
    return;
  }
  const [clientes, categorias, tecnicos] = listas;

  const faltando = [];
  if (!clientes.length) faltando.push(['clientes', 'um cliente']);
  if (!categorias.length) faltando.push(['categorias', 'uma categoria']);
  if (!tecnicos.length) faltando.push(['tecnicos', 'um técnico']);
  if (faltando.length) {
    abrirModal({
      titulo: 'Faltam cadastros',
      corpo: `
        <div class="modal-corpo">
          <p style="margin-top:0">Para abrir um chamado é preciso ter pelo menos:</p>
          <ul>${faltando.map(([rota, texto]) => `<li><a href="#/${rota}" data-fechar>Cadastrar ${texto}</a></li>`).join('')}</ul>
        </div>
        <div class="modal-rodape"><button type="button" class="btn" data-fechar>Entendi</button></div>`,
    });
    return;
  }

  const campos = [
    { nome: 'titulo', rotulo: 'Título', obrigatorio: true, max: 150, placeholder: 'Resumo do problema' },
    { nome: 'descricao', rotulo: 'Descrição', tipo: 'textarea', obrigatorio: true, max: 2000, linhas: 5 },
    [
      { nome: 'prioridade', rotulo: 'Prioridade', tipo: 'select', obrigatorio: true, opcoes: PRIORIDADES },
      {
        nome: 'categoriaId', rotulo: 'Categoria', tipo: 'select', obrigatorio: true, numero: true,
        opcoes: categorias.map((c) => ({ valor: c.id, rotulo: c.nome })),
      },
    ],
    [
      {
        nome: 'clienteId', rotulo: 'Cliente', tipo: 'select', obrigatorio: true, numero: true,
        opcoes: clientes.map((c) => ({ valor: c.id, rotulo: c.nome })),
      },
      {
        nome: 'tecnicoId', rotulo: 'Técnico responsável', tipo: 'select', obrigatorio: true, numero: true,
        opcoes: tecnicos.map((t) => ({ valor: t.id, rotulo: `${t.nome} (${PERFIS[t.perfil] ?? t.perfil})` })),
      },
    ],
  ];

  const valores = chamado
    ? {
      titulo: chamado.titulo,
      descricao: chamado.descricao,
      prioridade: chamado.prioridade,
      categoriaId: chamado.categoriaId,
      clienteId: chamado.clienteId,
      tecnicoId: chamado.tecnicoId,
    }
    : { prioridade: 'MEDIA', clienteId };

  abrirFormulario({
    titulo: chamado ? `Editar chamado #${chamado.id}` : 'Novo chamado',
    campos,
    valores,
    textoBotao: chamado ? 'Salvar' : 'Abrir chamado',
    aoEnviar: async (dados) => {
      if (chamado) {
        await api.chamados.atualizar(chamado.id, dados);
        toast('Chamado atualizado');
        recarregar();
      } else {
        const criado = await api.chamados.criar(dados);
        toast(`Chamado #${criado.id} aberto`);
        irPara(`/chamados/${criado.id}`);
      }
    },
  });
}

// ---------- Tela ----------

export async function render(el, _params, query) {
  const f = lerFiltros(query);
  const [pagina, clientes] = await Promise.all([api.chamados.listar(f), api.clientes.listar()]);
  const mudar = (parcial) => irPara('/chamados', paramsDaUrl({ ...f, ...parcial }));
  const temFiltro = f.status || f.prioridade || f.clienteId;

  let corpo;
  if (pagina.conteudo.length) {
    corpo = tabelaChamados(pagina.conteudo, { sort: f.sort });
  } else if (temFiltro) {
    corpo = vazio('Nenhum chamado encontrado', 'Nenhum chamado combina com os filtros escolhidos.',
      '<button class="btn" data-limpar-vazio>Limpar filtros</button>');
  } else if (pagina.totalElementos > 0) {
    corpo = vazio('Página vazia', 'Não há chamados nesta página.',
      '<button class="btn" data-primeira>Ir para a primeira página</button>');
  } else {
    corpo = vazio('Nenhum chamado aberto ainda', 'Abra o primeiro chamado para começar.',
      '<button class="btn btn-primario" data-novo>+ Novo chamado</button>');
  }

  el.innerHTML = `
    <div class="cabecalho">
      <div>
        <h1>Chamados</h1>
        <p>Abra, acompanhe e filtre os chamados de suporte.</p>
      </div>
      <div class="acoes"><button class="btn btn-primario" data-novo>+ Novo chamado</button></div>
    </div>
    <div class="cartao">
      ${filtrosHtml(f, { clientes })}
      ${corpo}
      ${pagina.totalElementos > 0 ? paginacaoHtml(pagina) : ''}
    </div>`;

  ligarFiltros(el, mudar);
  ligarOrdenacao(el, f.sort, (sort) => mudar({ sort, page: 0 }));
  ligarPaginacao(el, mudar);

  el.addEventListener('click', (e) => {
    if (e.target.closest('[data-novo]')) abrirFormChamado({ clienteId: f.clienteId || null });
    else if (e.target.closest('[data-limpar-vazio]')) mudar(FILTROS_LIMPOS);
    else if (e.target.closest('[data-primeira]')) mudar({ page: 0 });
  });
}
