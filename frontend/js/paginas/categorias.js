// Tela de Categorias: CRUD em /categorias.
import { api } from '../api.js';
import { esc } from '../ui.js';
import { telaCrud } from '../crud.js';

export const render = telaCrud({
  titulo: 'Categorias',
  descricao: 'Tipos de chamado (ex.: Hardware, Rede, Acesso a sistemas).',
  recurso: api.categorias,
  colunas: [
    { rotulo: 'Nº', classe: 'num', html: (c) => c.id },
    { rotulo: 'Nome', classe: 'titulo-celula', html: (c) => esc(c.nome) },
    { rotulo: 'Descrição', classe: 'esconder-celular', html: (c) => (c.descricao ? esc(c.descricao) : '<span class="texto-suave">—</span>') },
  ],
  campos: [
    { nome: 'nome', rotulo: 'Nome', obrigatorio: true, max: 100 },
    { nome: 'descricao', rotulo: 'Descrição', tipo: 'textarea', max: 255, linhas: 3 },
  ],
  valoresDe: (c) => ({ nome: c.nome, descricao: c.descricao }),
  textoBusca: (c) => `${c.nome} ${c.descricao ?? ''}`,
  nomeDe: (c) => c.nome,
  textos: {
    novo: 'Nova categoria',
    editar: 'Editar categoria',
    vazio: 'Nenhuma categoria cadastrada',
    vazioDica: 'Todo chamado precisa de uma categoria.',
    criado: 'Categoria cadastrada',
    atualizado: 'Categoria atualizada',
    excluido: 'Categoria excluída',
    unidade: ['categoria', 'categorias'],
  },
});
