// =========================================================
// Cliente HTTP: é o ÚNICO arquivo do front que conversa com a API.
// As telas chamam, por exemplo, api.clientes.listar() e recebem os dados prontos
// ou um ErroApi com a mensagem que veio do GlobalExceptionHandler.
// =========================================================

// Servido pela própria API (porta 8080) usa caminhos relativos;
// aberto em outra porta (ex.: Live Server 5500) aponta para a API e depende do CORS.
export const BASE_URL = window.location.port === '8080' ? '' : 'http://localhost:8080';

/** Erro de chamada à API, já no formato do ErroResposta (erro + detalhes). */
export class ErroApi extends Error {
  constructor(mensagem, status, detalhes = []) {
    super(mensagem);
    this.name = 'ErroApi';
    this.status = status;
    this.detalhes = detalhes;
  }
}

const MENSAGENS_PADRAO = {
  400: 'Requisição inválida.',
  404: 'Recurso não encontrado.',
  405: 'Método HTTP não permitido nesta rota.',
  409: 'Operação não permitida pelas regras do sistema.',
  500: 'Erro interno no servidor. Veja o terminal onde a API está rodando.',
};

async function requisicao(metodo, caminho, corpo) {
  const opcoes = { method: metodo, headers: { Accept: 'application/json' } };
  if (corpo !== undefined) {
    opcoes.headers['Content-Type'] = 'application/json';
    opcoes.body = JSON.stringify(corpo);
  }

  let resposta;
  try {
    resposta = await fetch(BASE_URL + caminho, opcoes);
  } catch {
    throw new ErroApi(
      'Não foi possível conectar à API. Confira se o back-end está rodando em http://localhost:8080.', 0);
  }

  if (resposta.status === 204) return null;

  const texto = await resposta.text();
  let dados = null;
  if (texto) {
    try { dados = JSON.parse(texto); } catch { dados = null; }
  }

  if (!resposta.ok) {
    // Formato padrão da API: { momento, status, erro, detalhes }
    if (dados && typeof dados.erro === 'string') {
      throw new ErroApi(dados.erro, dados.status ?? resposta.status, dados.detalhes ?? []);
    }
    throw new ErroApi(MENSAGENS_PADRAO[resposta.status] ?? `Erro HTTP ${resposta.status}`, resposta.status);
  }
  return dados;
}

/** Monta "?a=1&b=2" ignorando valores vazios. */
function query(params = {}) {
  const qs = new URLSearchParams();
  for (const [chave, valor] of Object.entries(params)) {
    if (valor !== undefined && valor !== null && valor !== '') qs.append(chave, valor);
  }
  const texto = qs.toString();
  return texto ? `?${texto}` : '';
}

/** CRUD padrão (POST/GET/GET id/PUT/DELETE) de um recurso. */
function crud(base) {
  return {
    listar: () => requisicao('GET', base),
    buscar: (id) => requisicao('GET', `${base}/${id}`),
    criar: (dados) => requisicao('POST', base, dados),
    atualizar: (id, dados) => requisicao('PUT', `${base}/${id}`, dados),
    excluir: (id) => requisicao('DELETE', `${base}/${id}`),
  };
}

export const api = {
  clientes: {
    ...crud('/clientes'),
    // filtros: { status, prioridade, page, size, sort }
    chamados: (id, filtros) => requisicao('GET', `/clientes/${id}/chamados${query(filtros)}`),
  },

  categorias: crud('/categorias'),

  tecnicos: crud('/tecnicos'),

  chamados: {
    ...crud('/chamados'),
    // filtros: { status, prioridade, clienteId, page, size, sort } -> resposta paginada
    listar: (filtros) => requisicao('GET', `/chamados${query(filtros)}`),
    atribuir: (id, tecnicoId) => requisicao('PATCH', `/chamados/${id}/atribuir`, { tecnicoId }),
    alterarStatus: (id, status) => requisicao('PATCH', `/chamados/${id}/status`, { status }),
    historico: (id) => requisicao('GET', `/chamados/${id}/historico`),
    comentarios: (id) => requisicao('GET', `/chamados/${id}/comentarios`),
    comentar: (id, dados) => requisicao('POST', `/chamados/${id}/comentarios`, dados),
  },
};
