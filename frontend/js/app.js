// =========================================================
// Ponto de entrada do front: rotas com # (ex.: #/chamados/3),
// menu lateral e comportamentos globais.
// =========================================================
import { BASE_URL } from './api.js';
import { carregando, erroBloco, vazio, recarregar } from './ui.js';
import * as inicio from './paginas/inicio.js';
import * as chamados from './paginas/chamados.js';
import * as chamadoDetalhe from './paginas/chamado-detalhe.js';
import * as clientes from './paginas/clientes.js';
import * as categorias from './paginas/categorias.js';
import * as tecnicos from './paginas/tecnicos.js';

// Cada rota: padrão do caminho, item do menu que fica ativo e a função que desenha a tela.
// O grupo (\d+) do padrão vira params.id.
const rotas = [
  { padrao: /^\/$/, menu: 'inicio', titulo: 'Início', render: inicio.render },
  { padrao: /^\/chamados$/, menu: 'chamados', titulo: 'Chamados', render: chamados.render },
  { padrao: /^\/chamados\/(\d+)$/, menu: 'chamados', titulo: 'Chamado', render: chamadoDetalhe.render },
  { padrao: /^\/clientes$/, menu: 'clientes', titulo: 'Clientes', render: clientes.render },
  { padrao: /^\/clientes\/(\d+)$/, menu: 'clientes', titulo: 'Cliente', render: clientes.renderDetalhe },
  { padrao: /^\/categorias$/, menu: 'categorias', titulo: 'Categorias', render: categorias.render },
  { padrao: /^\/tecnicos$/, menu: 'tecnicos', titulo: 'Técnicos', render: tecnicos.render },
];

const conteudo = document.getElementById('conteudo');
let contador = 0;
let ultimoCaminho = null;

function lerHash() {
  const hash = window.location.hash.slice(1) || '/';
  const [caminho, qs = ''] = hash.split('?');
  return { caminho, query: Object.fromEntries(new URLSearchParams(qs)) };
}

function marcarMenu(menu) {
  document.querySelectorAll('.nav a[data-menu]').forEach((a) => {
    a.classList.toggle('ativo', a.dataset.menu === menu);
  });
}

async function navegar() {
  const { caminho, query } = lerHash();
  const id = ++contador;

  let rota = null;
  let params = {};
  for (const r of rotas) {
    const encontrado = caminho.match(r.padrao);
    if (encontrado) {
      rota = r;
      params = { id: encontrado[1] };
      break;
    }
  }

  document.body.classList.remove('menu-aberto');
  marcarMenu(rota?.menu);

  if (!rota) {
    ultimoCaminho = null;
    conteudo.innerHTML = vazio('Página não encontrada', `Não existe tela para "${caminho}".`,
      '<a class="btn" href="#/">Ir para o início</a>');
    return;
  }
  document.title = `${rota.titulo} · Sistema de Chamados`;

  // Mesma tela (mudou filtro/página ou recarregou após salvar): mantém o conteúdo atual
  // na tela até os dados novos chegarem, sem piscar e sem voltar ao topo.
  const mesmaTela = caminho === ultimoCaminho;
  ultimoCaminho = caminho;
  const tela = document.createElement('div');
  if (!mesmaTela) {
    tela.innerHTML = carregando();
    conteudo.replaceChildren(tela);
    window.scrollTo(0, 0);
  }

  try {
    await rota.render(tela, params, query);
  } catch (erro) {
    console.error(erro);
    tela.innerHTML = erroBloco(erro);
  }

  // Se o usuário já navegou para outro lugar enquanto carregava, descarta este resultado.
  if (mesmaTela && id === contador) conteudo.replaceChildren(tela);
}

// ---------- Comportamentos globais ----------

// Linha de tabela com data-href é clicável inteira.
document.addEventListener('click', (e) => {
  const linha = e.target.closest('tr[data-href]');
  if (linha && !e.target.closest('a, button, input, select')) window.location.hash = linha.dataset.href;
  if (e.target.closest('[data-recarregar]')) recarregar();
});

// Menu no celular.
const botaoMenu = document.getElementById('btn-menu');
botaoMenu.addEventListener('click', () => {
  const aberto = document.body.classList.toggle('menu-aberto');
  botaoMenu.setAttribute('aria-expanded', String(aberto));
});
document.getElementById('menu-fundo').addEventListener('click', () => {
  document.body.classList.remove('menu-aberto');
  botaoMenu.setAttribute('aria-expanded', 'false');
});

// Links das ferramentas apontam para a API (útil quando o front roda em outra porta).
document.getElementById('link-swagger').href = `${BASE_URL}/swagger-ui.html`;
document.getElementById('link-h2').href = `${BASE_URL}/h2-console`;

window.addEventListener('hashchange', navegar);
navegar();
