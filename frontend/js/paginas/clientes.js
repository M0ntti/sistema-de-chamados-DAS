// =========================================================
// Tela de Clientes: CRUD em /clientes
// e detalhe do cliente com os chamados dele (GET /clientes/{id}/chamados).
// =========================================================
import { api } from '../api.js';
import {
  esc, vazio, irPara, paginacaoHtml, ligarPaginacao,
} from '../ui.js';
import { telaCrud } from '../crud.js';
import {
  lerFiltros, paramsDaUrl, filtrosHtml, ligarFiltros, tabelaChamados, ligarOrdenacao,
  abrirFormChamado, FILTROS_LIMPOS,
} from './chamados.js';

const campos = [
  { nome: 'nome', rotulo: 'Nome', obrigatorio: true, max: 100 },
  { nome: 'email', rotulo: 'E-mail', tipo: 'email', obrigatorio: true, max: 120, placeholder: 'nome@exemplo.com' },
  { nome: 'telefone', rotulo: 'Telefone', tipo: 'tel', max: 20, placeholder: '47999999999' },
];

export const render = telaCrud({
  titulo: 'Clientes',
  descricao: 'Pessoas ou empresas que abrem chamados.',
  recurso: api.clientes,
  colunas: [
    { rotulo: 'Nº', classe: 'num', html: (c) => c.id },
    { rotulo: 'Nome', classe: 'titulo-celula', html: (c) => `<a href="#/clientes/${c.id}">${esc(c.nome)}</a>` },
    { rotulo: 'E-mail', classe: 'esconder-celular', html: (c) => esc(c.email) },
    { rotulo: 'Telefone', classe: 'esconder-celular', html: (c) => (c.telefone ? esc(c.telefone) : '<span class="texto-suave">—</span>') },
  ],
  campos,
  valoresDe: (c) => ({ nome: c.nome, email: c.email, telefone: c.telefone }),
  textoBusca: (c) => `${c.nome} ${c.email} ${c.telefone ?? ''}`,
  nomeDe: (c) => c.nome,
  acoesExtras: (c) => `<a class="btn btn-sm" href="#/clientes/${c.id}">Chamados</a>`,
  textos: {
    novo: 'Novo cliente',
    editar: 'Editar cliente',
    vazio: 'Nenhum cliente cadastrado',
    vazioDica: 'Cadastre um cliente para poder abrir chamados.',
    criado: 'Cliente cadastrado',
    atualizado: 'Cliente atualizado',
    excluido: 'Cliente excluído',
    unidade: ['cliente', 'clientes'],
  },
});

/** Detalhe: dados do cliente + chamados dele com filtros, ordenação e paginação. */
export async function renderDetalhe(el, { id }, query) {
  const f = lerFiltros(query);
  const [cliente, pagina] = await Promise.all([
    api.clientes.buscar(id),
    api.clientes.chamados(id, { status: f.status, prioridade: f.prioridade, page: f.page, size: f.size, sort: f.sort }),
  ]);
  const mudar = (parcial) => irPara(`/clientes/${id}`, paramsDaUrl({ ...f, ...parcial, clienteId: '' }));
  const temFiltro = f.status || f.prioridade;

  let corpo;
  if (pagina.conteudo.length) {
    corpo = tabelaChamados(pagina.conteudo, { sort: f.sort, mostrarCliente: false });
  } else if (temFiltro) {
    corpo = vazio('Nenhum chamado encontrado', 'Nenhum chamado deste cliente combina com os filtros.',
      '<button class="btn" data-limpar-vazio>Limpar filtros</button>');
  } else {
    corpo = vazio('Este cliente ainda não tem chamados', '',
      '<button class="btn btn-primario" data-novo>+ Abrir chamado</button>');
  }

  el.innerHTML = `
    <a href="#/clientes" class="voltar">← Voltar para clientes</a>
    <div class="cabecalho">
      <div>
        <h1>${esc(cliente.nome)}</h1>
        <p>${esc(cliente.email)}${cliente.telefone ? ` · ${esc(cliente.telefone)}` : ''}</p>
      </div>
      <div class="acoes"><button class="btn btn-primario" data-novo>+ Abrir chamado</button></div>
    </div>
    <div class="cartao">
      <div class="cartao-cabecalho"><h2>Chamados do cliente</h2></div>
      ${filtrosHtml(f)}
      ${corpo}
      ${pagina.totalElementos > 0 ? paginacaoHtml(pagina) : ''}
    </div>`;

  ligarFiltros(el, mudar);
  ligarOrdenacao(el, f.sort, (sort) => mudar({ sort, page: 0 }));
  ligarPaginacao(el, mudar);

  el.addEventListener('click', (e) => {
    if (e.target.closest('[data-novo]')) abrirFormChamado({ clienteId: cliente.id });
    else if (e.target.closest('[data-limpar-vazio]')) mudar(FILTROS_LIMPOS);
  });
}
