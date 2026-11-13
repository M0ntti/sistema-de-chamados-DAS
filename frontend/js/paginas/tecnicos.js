// Tela de Técnicos: CRUD em /tecnicos.
import { api } from '../api.js';
import { esc } from '../ui.js';
import { PERFIS } from '../dominio.js';
import { telaCrud } from '../crud.js';

export const render = telaCrud({
  titulo: 'Técnicos',
  descricao: 'Equipe que atende os chamados.',
  recurso: api.tecnicos,
  colunas: [
    { rotulo: 'Nº', classe: 'num', html: (t) => t.id },
    { rotulo: 'Nome', classe: 'titulo-celula', html: (t) => esc(t.nome) },
    { rotulo: 'E-mail', classe: 'esconder-celular', html: (t) => esc(t.email) },
    { rotulo: 'Perfil', html: (t) => esc(PERFIS[t.perfil] ?? t.perfil) },
  ],
  campos: [
    { nome: 'nome', rotulo: 'Nome', obrigatorio: true, max: 100 },
    { nome: 'email', rotulo: 'E-mail', tipo: 'email', obrigatorio: true, max: 120, placeholder: 'nome@empresa.com' },
    { nome: 'perfil', rotulo: 'Perfil', tipo: 'select', obrigatorio: true, opcoes: PERFIS },
  ],
  valoresDe: (t) => ({ nome: t.nome, email: t.email, perfil: t.perfil }),
  textoBusca: (t) => `${t.nome} ${t.email} ${PERFIS[t.perfil] ?? ''}`,
  nomeDe: (t) => t.nome,
  textos: {
    novo: 'Novo técnico',
    editar: 'Editar técnico',
    vazio: 'Nenhum técnico cadastrado',
    vazioDica: 'Todo chamado precisa de um técnico responsável.',
    criado: 'Técnico cadastrado',
    atualizado: 'Técnico atualizado',
    excluido: 'Técnico excluído',
    unidade: ['técnico', 'técnicos'],
  },
});
