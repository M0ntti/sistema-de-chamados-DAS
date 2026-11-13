// =========================================================
// Valores dos enums do back-end e seus rótulos para a tela.
// A chave é exatamente o que a API espera/devolve no JSON.
// =========================================================

export const STATUS = {
  ABERTO: 'Aberto',
  EM_ATENDIMENTO: 'Em atendimento',
  AGUARDANDO_CLIENTE: 'Aguardando cliente',
  FECHADO: 'Fechado',
};

export const PRIORIDADES = {
  BAIXA: 'Baixa',
  MEDIA: 'Média',
  ALTA: 'Alta',
  CRITICA: 'Crítica',
};

export const PERFIS = {
  ADMIN: 'Administrador',
  TECNICO: 'Técnico',
};

// Mesma tabela do enum StatusChamado.proximosPermitidos() no back-end.
// O front só mostra os botões válidos; quem garante a regra continua sendo a API (409).
export const TRANSICOES = {
  ABERTO: ['EM_ATENDIMENTO', 'FECHADO'],
  EM_ATENDIMENTO: ['AGUARDANDO_CLIENTE', 'FECHADO'],
  AGUARDANDO_CLIENTE: ['EM_ATENDIMENTO', 'FECHADO'],
  FECHADO: [],
};

/** Texto do botão que leva o chamado de `atual` para `destino`. */
export function textoAcaoStatus(atual, destino) {
  if (destino === 'FECHADO') return 'Fechar chamado';
  if (destino === 'AGUARDANDO_CLIENTE') return 'Aguardar retorno do cliente';
  if (destino === 'EM_ATENDIMENTO') {
    return atual === 'AGUARDANDO_CLIENTE' ? 'Retomar atendimento' : 'Iniciar atendimento';
  }
  return STATUS[destino];
}

// Campos aceitos no parâmetro sort de GET /chamados (outro campo -> 400).
export const CAMPOS_ORDENACAO = {
  dataAbertura: 'Data de abertura',
  dataFechamento: 'Data de fechamento',
  id: 'Número',
  titulo: 'Título',
  prioridade: 'Prioridade',
  status: 'Status',
};

export const ORDENACAO_PADRAO = 'dataAbertura,desc';
export const TAMANHOS_PAGINA = [5, 10, 20, 50];
