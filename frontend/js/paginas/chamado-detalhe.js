// =========================================================
// Detalhe do chamado: GET /chamados/{id} (com comentários) e /historico.
// Ações: alterar status (só transições válidas), atribuir técnico,
// comentar, editar e excluir. Chamado FECHADO fica só leitura.
// =========================================================
import { api } from '../api.js';
import {
  esc, formatarData, etiquetaStatus, etiquetaPrioridade, plural, opcoesHtml, camposHtml,
  aoEnviarForm, confirmar, toast, mostrarErro, irPara, recarregar,
} from '../ui.js';
import {
  STATUS, PERFIS, TRANSICOES, textoAcaoStatus,
} from '../dominio.js';
import { abrirFormChamado } from './chamados.js';

const CHAVE_AUTOR = 'chamados.autor';

const camposComentario = [
  { nome: 'autor', rotulo: 'Autor', obrigatorio: true, max: 100, placeholder: 'Seu nome' },
  { nome: 'texto', rotulo: 'Comentário', tipo: 'textarea', obrigatorio: true, max: 2000, linhas: 3 },
];

// Lembrar o nome do autor é só conveniência: se o navegador bloquear, segue sem.
function autorSalvo() {
  try { return localStorage.getItem(CHAVE_AUTOR) ?? ''; } catch { return ''; }
}
function salvarAutor(autor) {
  try { localStorage.setItem(CHAVE_AUTOR, autor); } catch { /* ignora */ }
}

function comentariosHtml(comentarios) {
  if (!comentarios.length) {
    return '<p class="texto-suave" style="margin-top:0">Nenhum comentário ainda.</p>';
  }
  return `
    <ul class="comentarios">
      ${comentarios.map((c) => `
        <li class="comentario">
          <div class="comentario-topo"><strong>${esc(c.autor)}</strong><span>${formatarData(c.data)}</span></div>
          <p>${esc(c.texto)}</p>
        </li>`).join('')}
    </ul>`;
}

function historicoHtml(chamado, historico) {
  // A abertura não gera registro de histórico na API; mostramos como primeiro item.
  const itens = [
    `<li><strong>Chamado aberto</strong> ${etiquetaStatus('ABERTO')}<span class="quando">${formatarData(chamado.dataAbertura)}</span></li>`,
    ...historico.map((h) => `
      <li>
        ${etiquetaStatus(h.statusAnterior)} → ${etiquetaStatus(h.statusNovo)}
        <span class="quando">${formatarData(h.dataAlteracao)}</span>
      </li>`),
  ];
  return `<ul class="linha-tempo">${itens.join('')}</ul>`;
}

function acoesStatusHtml(chamado) {
  const destinos = TRANSICOES[chamado.status] ?? [];
  if (!destinos.length) {
    return `<div class="aviso-fechado">Chamado fechado em ${formatarData(chamado.dataFechamento)}. É o estado final: não aceita mais alterações, atribuição nem comentários.</div>`;
  }
  return `
    <div class="acoes-status">
      ${destinos.map((destino) => `
        <button class="btn ${destino === 'FECHADO' ? 'btn-perigo' : ''}" data-status="${destino}">
          ${esc(textoAcaoStatus(chamado.status, destino))}
          <span class="texto-suave" style="margin-left:auto">→ ${esc(STATUS[destino])}</span>
        </button>`).join('')}
    </div>`;
}

export async function render(el, { id }) {
  const [chamado, historico, tecnicos] = await Promise.all([
    api.chamados.buscar(id),
    api.chamados.historico(id),
    api.tecnicos.listar(),
  ]);
  const fechado = chamado.status === 'FECHADO';
  document.title = `#${chamado.id} ${chamado.titulo} · Sistema de Chamados`;

  el.innerHTML = `
    <a href="#/chamados" class="voltar">← Voltar para chamados</a>
    <div class="cabecalho">
      <div>
        <h1><span class="num">#${chamado.id}</span> ${esc(chamado.titulo)}</h1>
        <p>${etiquetaStatus(chamado.status)} ${etiquetaPrioridade(chamado.prioridade)} · ${esc(chamado.categoriaNome)}</p>
      </div>
      <div class="acoes">
        <button class="btn" data-editar${fechado ? ' disabled title="Chamado fechado não pode ser editado"' : ''}>Editar</button>
        <button class="btn btn-perigo" data-excluir>Excluir</button>
      </div>
    </div>

    <div class="detalhe-grade">
      <div>
        <section class="cartao">
          <div class="cartao-cabecalho"><h2>Descrição</h2></div>
          <div class="cartao-corpo"><p class="descricao">${esc(chamado.descricao)}</p></div>
        </section>

        <section class="cartao">
          <div class="cartao-cabecalho">
            <h2>Comentários</h2>
            <span class="texto-suave">${plural(chamado.comentarios.length, 'comentário', 'comentários')}</span>
          </div>
          <div class="cartao-corpo">
            ${comentariosHtml(chamado.comentarios)}
            ${fechado
    ? '<p class="texto-suave" style="margin-bottom:0">Chamado fechado: não aceita novos comentários.</p>'
    : `<form novalidate data-comentar style="margin-top:16px">
                  <div class="form-erro" role="alert"></div>
                  ${camposHtml(camposComentario, { autor: autorSalvo() })}
                  <button type="submit" class="btn btn-primario">Comentar</button>
                </form>`}
          </div>
        </section>
      </div>

      <aside>
        <section class="cartao">
          <div class="cartao-cabecalho"><h2>Status</h2>${etiquetaStatus(chamado.status)}</div>
          <div class="cartao-corpo">${acoesStatusHtml(chamado)}</div>
        </section>

        <section class="cartao">
          <div class="cartao-cabecalho"><h2>Técnico responsável</h2></div>
          <div class="cartao-corpo">
            <form class="atribuir" data-atribuir>
              <select name="tecnicoId" aria-label="Técnico responsável"${fechado ? ' disabled' : ''}>
                ${opcoesHtml(tecnicos.map((t) => ({ valor: t.id, rotulo: `${t.nome} (${PERFIS[t.perfil] ?? t.perfil})` })), chamado.tecnicoId)}
              </select>
              <button type="submit" class="btn"${fechado ? ' disabled' : ''}>Atribuir</button>
            </form>
          </div>
        </section>

        <section class="cartao">
          <div class="cartao-cabecalho"><h2>Informações</h2></div>
          <div class="cartao-corpo">
            <dl class="info">
              <dt>Cliente</dt><dd><a href="#/clientes/${chamado.clienteId}">${esc(chamado.clienteNome)}</a></dd>
              <dt>Categoria</dt><dd>${esc(chamado.categoriaNome)}</dd>
              <dt>Técnico</dt><dd>${esc(chamado.tecnicoNome)}</dd>
              <dt>Abertura</dt><dd>${formatarData(chamado.dataAbertura)}</dd>
              <dt>Fechamento</dt><dd>${formatarData(chamado.dataFechamento)}</dd>
            </dl>
          </div>
        </section>

        <section class="cartao">
          <div class="cartao-cabecalho"><h2>Histórico de status</h2></div>
          <div class="cartao-corpo">${historicoHtml(chamado, historico)}</div>
        </section>
      </aside>
    </div>`;

  // ----- Alterar status -----
  el.querySelectorAll('[data-status]').forEach((botao) => {
    botao.addEventListener('click', async () => {
      const destino = botao.dataset.status;
      if (destino === 'FECHADO') {
        const ok = await confirmar({
          titulo: 'Fechar chamado',
          mensagem: 'Depois de fechado, o chamado não aceita mais edição, atribuição, mudança de status nem comentários. Continuar?',
          textoBotao: 'Fechar chamado',
          perigo: true,
        });
        if (!ok) return;
      }
      botao.disabled = true;
      try {
        await api.chamados.alterarStatus(chamado.id, destino);
        toast(`Status alterado para "${STATUS[destino]}"`);
        recarregar();
      } catch (erro) {
        mostrarErro(erro);
        botao.disabled = false;
      }
    });
  });

  // ----- Atribuir técnico -----
  el.querySelector('[data-atribuir]').addEventListener('submit', async (e) => {
    e.preventDefault();
    const tecnicoId = Number(e.target.elements.tecnicoId.value);
    if (tecnicoId === chamado.tecnicoId) {
      toast('Este técnico já é o responsável pelo chamado.', 'erro');
      return;
    }
    try {
      const atualizado = await api.chamados.atribuir(chamado.id, tecnicoId);
      toast(`Chamado atribuído a ${atualizado.tecnicoNome}`);
      recarregar();
    } catch (erro) {
      mostrarErro(erro);
    }
  });

  // ----- Comentar -----
  const formComentario = el.querySelector('[data-comentar]');
  if (formComentario) {
    aoEnviarForm(formComentario, camposComentario, async (dados) => {
      await api.chamados.comentar(chamado.id, dados);
      if (dados.autor) salvarAutor(dados.autor);
      toast('Comentário adicionado');
      recarregar();
    });
  }

  // ----- Editar / excluir -----
  el.querySelector('[data-editar]').addEventListener('click', () => abrirFormChamado({ chamado }));

  el.querySelector('[data-excluir]').addEventListener('click', async () => {
    const ok = await confirmar({
      titulo: 'Excluir chamado',
      mensagem: `Excluir o chamado #${chamado.id}? Os comentários e o histórico também serão apagados.`,
      textoBotao: 'Excluir',
      perigo: true,
    });
    if (!ok) return;
    try {
      await api.chamados.excluir(chamado.id);
      toast(`Chamado #${chamado.id} excluído`);
      irPara('/chamados');
    } catch (erro) {
      mostrarErro(erro);
    }
  });
}
