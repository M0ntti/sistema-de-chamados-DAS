// =========================================================
// Funções de interface reaproveitadas por todas as telas.
// =========================================================
import { ErroApi } from './api.js';
import { STATUS, PRIORIDADES, TAMANHOS_PAGINA } from './dominio.js';

// ---------- Texto e formatação ----------

/** Escapa HTML. Use SEMPRE que colocar dado vindo da API dentro de innerHTML. */
export function esc(valor) {
  return String(valor ?? '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  }[c]));
}

/** "2026-11-05T10:00:00.123456" (LocalDateTime) -> "05/11/2026 10:00". */
export function formatarData(iso) {
  if (!iso) return '—';
  const data = new Date(String(iso).replace(/(\.\d{3})\d+/, '$1'));
  if (Number.isNaN(data.getTime())) return esc(iso);
  return data.toLocaleString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

export function plural(qtd, singular, pluralTexto) {
  return `${qtd} ${qtd === 1 ? singular : pluralTexto}`;
}

export function etiquetaStatus(status) {
  return `<span class="etiqueta st-${esc(status)}">${esc(STATUS[status] ?? status)}</span>`;
}

export function etiquetaPrioridade(prioridade) {
  return `<span class="etiqueta pr-${esc(prioridade)}">${esc(PRIORIDADES[prioridade] ?? prioridade)}</span>`;
}

// ---------- Estados da tela ----------

export function carregando(texto = 'Carregando...') {
  return `<div class="estado"><div class="girando"></div><div>${esc(texto)}</div></div>`;
}

/** Estado vazio. `acaoHtml` é HTML já pronto (ex.: um botão). */
export function vazio(titulo, texto = '', acaoHtml = '') {
  return `<div class="estado"><strong>${esc(titulo)}</strong>${texto ? `<div>${esc(texto)}</div>` : ''}${acaoHtml}</div>`;
}

export function erroBloco(erro) {
  const detalhes = erro?.detalhes?.length
    ? `<ul>${erro.detalhes.map((d) => `<li>${esc(d)}</li>`).join('')}</ul>` : '';
  return `<div class="estado estado-erro">
      <strong>Não foi possível carregar esta tela</strong>
      <div>${esc(erro?.message ?? 'Erro inesperado.')}</div>${detalhes}
      <button class="btn" data-recarregar>Tentar novamente</button>
    </div>`;
}

// ---------- Avisos (toasts) ----------

export function toast(mensagem, tipo = 'sucesso', detalhes = []) {
  const caixa = document.getElementById('toasts');
  const item = document.createElement('div');
  item.className = `toast toast-${tipo}`;
  item.setAttribute('role', tipo === 'erro' ? 'alert' : 'status');
  item.innerHTML = esc(mensagem)
    + (detalhes.length ? `<ul>${detalhes.map((d) => `<li>${esc(d)}</li>`).join('')}</ul>` : '');
  caixa.append(item);
  setTimeout(() => item.remove(), tipo === 'erro' ? 6000 : 3500);
}

/** Mostra um erro da API (ErroApi) ou qualquer outro erro como aviso vermelho. */
export function mostrarErro(erro) {
  console.error(erro);
  toast(erro?.message ?? 'Erro inesperado.', 'erro', erro?.detalhes ?? []);
}

// ---------- Navegação (rotas com #) ----------

export function montarHash(caminho, params = {}) {
  const qs = new URLSearchParams();
  for (const [chave, valor] of Object.entries(params)) {
    if (valor !== undefined && valor !== null && valor !== '') qs.append(chave, valor);
  }
  const texto = qs.toString();
  return `#${caminho}${texto ? `?${texto}` : ''}`;
}

export function irPara(caminho, params = {}) {
  const destino = montarHash(caminho, params);
  if (window.location.hash === destino) recarregar();
  else window.location.hash = destino;
}

/** Redesenha a tela atual buscando os dados de novo na API. */
export function recarregar() {
  window.dispatchEvent(new HashChangeEvent('hashchange'));
}

// ---------- Modal ----------

/** Abre um <dialog>. `corpo` é HTML; qualquer elemento com data-fechar fecha o modal. */
export function abrirModal({ titulo, corpo }) {
  const dialog = document.createElement('dialog');
  dialog.className = 'modal';
  dialog.innerHTML = `
    <div class="modal-cabecalho">
      <h2>${esc(titulo)}</h2>
      <button type="button" class="modal-fechar" aria-label="Fechar" data-fechar>&times;</button>
    </div>
    ${corpo}`;
  document.body.append(dialog);

  // Fecha ao clicar fora (no fundo escuro), mas não ao arrastar uma seleção de texto para fora.
  let cliqueComecouFora = false;
  dialog.addEventListener('mousedown', (e) => { cliqueComecouFora = e.target === dialog; });
  dialog.addEventListener('click', (e) => {
    if (e.target.closest('[data-fechar]') || (e.target === dialog && cliqueComecouFora)) dialog.close();
  });
  dialog.addEventListener('close', () => dialog.remove());
  dialog.showModal();
  return dialog;
}

/** Pergunta de confirmação. Resolve true (confirmou) ou false. */
export function confirmar({ titulo, mensagem, textoBotao = 'Confirmar', perigo = false }) {
  return new Promise((resolve) => {
    let confirmou = false;
    const dialog = abrirModal({
      titulo,
      corpo: `
        <div class="modal-corpo"><p style="margin-top:0">${esc(mensagem)}</p></div>
        <div class="modal-rodape">
          <button type="button" class="btn" data-fechar>Cancelar</button>
          <button type="button" class="btn ${perigo ? 'btn-perigo-cheio' : 'btn-primario'}" data-ok>${esc(textoBotao)}</button>
        </div>`,
    });
    dialog.querySelector('[data-ok]').addEventListener('click', () => { confirmou = true; dialog.close(); });
    dialog.querySelector('[data-ok]').focus();
    dialog.addEventListener('close', () => resolve(confirmou));
  });
}

// ---------- Formulários ----------
// Um campo é descrito por um objeto:
// { nome, rotulo, tipo: 'text'|'email'|'tel'|'textarea'|'select', obrigatorio, max,
//   opcoes: { VALOR: 'Rótulo' } ou [{ valor, rotulo }], numero (converte para Number), ajuda, placeholder }
// Um array de campos dentro da lista desenha esses campos lado a lado.

function listaOpcoes(opcoes) {
  if (Array.isArray(opcoes)) return opcoes;
  return Object.entries(opcoes ?? {}).map(([valor, rotulo]) => ({ valor, rotulo }));
}

export function opcoesHtml(opcoes, selecionado) {
  return listaOpcoes(opcoes).map((o) => `<option value="${esc(o.valor)}"${String(o.valor) === String(selecionado ?? '') ? ' selected' : ''}>${esc(o.rotulo)}</option>`).join('');
}

export function campoHtml(campo, valor) {
  const id = `campo-${campo.nome}`;
  const v = valor ?? '';
  const max = campo.max ? ` maxlength="${campo.max}"` : '';
  const ph = campo.placeholder ? ` placeholder="${esc(campo.placeholder)}"` : '';
  let controle;
  if (campo.tipo === 'select') {
    const primeira = campo.vazio === false ? '' : `<option value="">${esc(campo.vazio ?? 'Selecione...')}</option>`;
    controle = `<select id="${id}" name="${campo.nome}">${primeira}${opcoesHtml(campo.opcoes, v)}</select>`;
  } else if (campo.tipo === 'textarea') {
    controle = `<textarea id="${id}" name="${campo.nome}" rows="${campo.linhas ?? 4}"${max}${ph}>${esc(v)}</textarea>`;
  } else {
    controle = `<input id="${id}" name="${campo.nome}" type="${campo.tipo ?? 'text'}" value="${esc(v)}"${max}${ph}>`;
  }
  return `
    <div class="campo">
      <label for="${id}">${esc(campo.rotulo)}${campo.obrigatorio ? ' <span class="obrigatorio">*</span>' : ''}</label>
      ${controle}
      ${campo.ajuda ? `<small class="campo-ajuda">${esc(campo.ajuda)}</small>` : ''}
      <small class="campo-erro" data-erro-de="${campo.nome}"></small>
    </div>`;
}

export function camposHtml(campos, valores = {}) {
  return campos.map((c) => (Array.isArray(c)
    ? `<div class="linha-form">${c.map((sub) => campoHtml(sub, valores[sub.nome])).join('')}</div>`
    : campoHtml(c, valores[c.nome]))).join('');
}

/** Lê os campos do form. Vazio vira null (assim o @NotBlank/@NotNull da API responde). */
export function lerCampos(form, campos) {
  const dados = {};
  for (const campo of campos.flat()) {
    const bruto = form.elements[campo.nome].value.trim();
    dados[campo.nome] = bruto === '' ? null : (campo.numero ? Number(bruto) : bruto);
  }
  return dados;
}

export function limparErros(form) {
  form.querySelectorAll('.campo-erro').forEach((el) => { el.textContent = ''; });
  form.querySelectorAll('.campo.com-erro').forEach((el) => el.classList.remove('com-erro'));
  const geral = form.querySelector('.form-erro');
  if (geral) geral.innerHTML = '';
}

/**
 * Distribui o ErroResposta da API no formulário:
 * cada "campo: mensagem" dos detalhes vai para baixo do campo correspondente;
 * o resto (ex.: "Já existe cliente com o e-mail ...") aparece no topo do form.
 */
export function mostrarErrosForm(form, erro) {
  limparErros(form);
  const naoEncaixados = [];
  for (const detalhe of erro.detalhes ?? []) {
    const pos = detalhe.indexOf(': ');
    const nome = pos > 0 ? detalhe.slice(0, pos) : '';
    const alvo = nome && form.querySelector(`[data-erro-de="${CSS.escape(nome)}"]`);
    if (alvo) {
      alvo.textContent = detalhe.slice(pos + 2);
      alvo.closest('.campo').classList.add('com-erro');
    } else {
      naoEncaixados.push(detalhe);
    }
  }
  const geral = form.querySelector('.form-erro');
  const algumCampo = form.querySelector('.campo.com-erro');
  const titulo = algumCampo && !naoEncaixados.length ? 'Confira os campos destacados.' : erro.message;
  const lista = naoEncaixados.length ? `<ul>${naoEncaixados.map((d) => `<li>${esc(d)}</li>`).join('')}</ul>` : '';
  if (geral) geral.innerHTML = esc(titulo) + lista;
  (form.querySelector('.campo.com-erro input, .campo.com-erro select, .campo.com-erro textarea'))?.focus();
}

/**
 * Liga o envio de um <form> à API: desabilita o botão enquanto envia
 * e mostra os erros de validação/regra de negócio nos campos.
 */
export function aoEnviarForm(form, campos, enviar) {
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const botao = form.querySelector('[type="submit"]');
    const textoOriginal = botao.textContent;
    limparErros(form);
    botao.disabled = true;
    botao.textContent = 'Salvando...';
    try {
      await enviar(lerCampos(form, campos));
    } catch (erro) {
      if (erro instanceof ErroApi && form.isConnected) mostrarErrosForm(form, erro);
      else mostrarErro(erro);
    } finally {
      botao.disabled = false;
      botao.textContent = textoOriginal;
    }
  });
}

/** Abre um formulário em modal. `aoEnviar(dados)` chama a API; se der certo o modal fecha. */
export function abrirFormulario({ titulo, campos, valores = {}, textoBotao = 'Salvar', aoEnviar }) {
  const dialog = abrirModal({
    titulo,
    corpo: `
      <form novalidate>
        <div class="modal-corpo">
          <div class="form-erro" role="alert"></div>
          ${camposHtml(campos, valores)}
        </div>
        <div class="modal-rodape">
          <button type="button" class="btn" data-fechar>Cancelar</button>
          <button type="submit" class="btn btn-primario">${esc(textoBotao)}</button>
        </div>
      </form>`,
  });
  const form = dialog.querySelector('form');
  aoEnviarForm(form, campos, async (dados) => {
    await aoEnviar(dados);
    dialog.close();
  });
  form.querySelector('input, select, textarea')?.focus();
  return dialog;
}

// ---------- Paginação ----------

/** Rodapé de paginação para a resposta { conteudo, pagina, tamanho, totalElementos, totalPaginas, primeira, ultima }. */
export function paginacaoHtml(pagina) {
  const total = Math.max(pagina.totalPaginas, 1);
  return `
    <div class="paginacao">
      <span>${plural(pagina.totalElementos, 'chamado', 'chamados')} · página ${pagina.pagina + 1} de ${total}</span>
      <div class="controles">
        <label>Por página
          <select data-tamanho>${TAMANHOS_PAGINA.map((t) => `<option value="${t}"${t === pagina.tamanho ? ' selected' : ''}>${t}</option>`).join('')}</select>
        </label>
        <button class="btn btn-sm" data-pagina="${pagina.pagina - 1}"${pagina.primeira ? ' disabled' : ''}>← Anterior</button>
        <button class="btn btn-sm" data-pagina="${pagina.pagina + 1}"${pagina.ultima ? ' disabled' : ''}>Próxima →</button>
      </div>
    </div>`;
}

/** Liga os botões/select da paginação. `mudar({ page, size })` recebe o que mudou. */
export function ligarPaginacao(el, mudar) {
  el.querySelectorAll('[data-pagina]').forEach((botao) => {
    botao.addEventListener('click', () => mudar({ page: Number(botao.dataset.pagina) }));
  });
  el.querySelector('[data-tamanho]')?.addEventListener('change', (e) => {
    mudar({ page: 0, size: Number(e.target.value) });
  });
}

/** Lê "campo,direcao" do parâmetro sort. */
export function lerOrdenacao(sort) {
  const [campo, direcao = 'asc'] = String(sort).split(',');
  return { campo, direcao };
}
