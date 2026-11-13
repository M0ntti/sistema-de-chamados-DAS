// =========================================================
// Tela de cadastro genérica (lista + busca + novo + editar + excluir).
// Clientes, Categorias e Técnicos usam esta mesma tela, mudando só a configuração.
// =========================================================
import {
  esc, vazio, plural, abrirFormulario, confirmar, toast, mostrarErro, recarregar,
} from './ui.js';

/**
 * cfg = {
 *   titulo, descricao,
 *   recurso: objeto do api.js (listar/criar/atualizar/excluir),
 *   colunas: [{ rotulo, html: (item) => string, classe }],
 *   campos: lista de campos do formulário (ver ui.js),
 *   valoresDe: (item) => valores para preencher o formulário de edição,
 *   textoBusca: (item) => texto usado pela busca,
 *   nomeDe: (item) => nome exibido na confirmação de exclusão,
 *   acoesExtras: (item) => HTML de botões extras na linha (opcional),
 *   textos: { novo, editar, vazio, vazioDica, criado, atualizado, excluido, unidade: [singular, plural] }
 * }
 */
export function telaCrud(cfg) {
  const t = cfg.textos;

  return async function render(el) {
    const itens = await cfg.recurso.listar();

    el.innerHTML = `
      <div class="cabecalho">
        <div>
          <h1>${esc(cfg.titulo)}</h1>
          <p>${esc(cfg.descricao)}</p>
        </div>
        <div class="acoes"><button class="btn btn-primario" data-novo>+ ${esc(t.novo)}</button></div>
      </div>
      <div class="cartao">
        <div class="cartao-cabecalho">
          <input type="search" class="busca" placeholder="Buscar..." aria-label="Buscar" data-busca>
          <span class="texto-suave" data-contagem></span>
        </div>
        <div data-lista></div>
      </div>`;

    const lista = el.querySelector('[data-lista]');
    const busca = el.querySelector('[data-busca]');
    const contagem = el.querySelector('[data-contagem]');

    function desenhar() {
      const termo = busca.value.trim().toLowerCase();
      const filtrados = termo ? itens.filter((i) => cfg.textoBusca(i).toLowerCase().includes(termo)) : itens;
      contagem.textContent = plural(filtrados.length, ...t.unidade);

      if (itens.length === 0) {
        lista.innerHTML = vazio(t.vazio, t.vazioDica, `<button class="btn btn-primario" data-novo>+ ${esc(t.novo)}</button>`);
        return;
      }
      if (filtrados.length === 0) {
        lista.innerHTML = vazio('Nada encontrado', `Nenhum resultado para "${termo}".`);
        return;
      }
      lista.innerHTML = `
        <div class="tabela-wrap">
          <table>
            <thead><tr>
              ${cfg.colunas.map((c) => `<th class="${c.classe ?? ''}">${esc(c.rotulo)}</th>`).join('')}
              <th><span class="esconder-celular">Ações</span></th>
            </tr></thead>
            <tbody>
              ${filtrados.map((item) => `
                <tr>
                  ${cfg.colunas.map((c) => `<td class="${c.classe ?? ''}">${c.html(item)}</td>`).join('')}
                  <td class="acoes">
                    ${cfg.acoesExtras ? cfg.acoesExtras(item) : ''}
                    <button class="btn btn-sm" data-editar="${item.id}">Editar</button>
                    <button class="btn btn-sm btn-perigo" data-excluir="${item.id}">Excluir</button>
                  </td>
                </tr>`).join('')}
            </tbody>
          </table>
        </div>`;
    }

    function novo() {
      abrirFormulario({
        titulo: t.novo,
        campos: cfg.campos,
        textoBotao: 'Cadastrar',
        aoEnviar: async (dados) => {
          await cfg.recurso.criar(dados);
          toast(t.criado);
          recarregar();
        },
      });
    }

    function editar(item) {
      abrirFormulario({
        titulo: t.editar,
        campos: cfg.campos,
        valores: cfg.valoresDe(item),
        aoEnviar: async (dados) => {
          await cfg.recurso.atualizar(item.id, dados);
          toast(t.atualizado);
          recarregar();
        },
      });
    }

    async function excluir(item) {
      const ok = await confirmar({
        titulo: 'Confirmar exclusão',
        mensagem: `Excluir "${cfg.nomeDe(item)}"? Esta ação não pode ser desfeita.`,
        textoBotao: 'Excluir',
        perigo: true,
      });
      if (!ok) return;
      try {
        await cfg.recurso.excluir(item.id);
        toast(t.excluido);
        recarregar();
      } catch (erro) {
        // Ex.: 409 quando o registro está ligado a algum chamado.
        mostrarErro(erro);
      }
    }

    desenhar();
    busca.addEventListener('input', desenhar);

    el.addEventListener('click', (e) => {
      if (e.target.closest('[data-novo]')) return novo();
      const botaoEditar = e.target.closest('[data-editar]');
      if (botaoEditar) return editar(itens.find((i) => String(i.id) === botaoEditar.dataset.editar));
      const botaoExcluir = e.target.closest('[data-excluir]');
      if (botaoExcluir) return excluir(itens.find((i) => String(i.id) === botaoExcluir.dataset.excluir));
      return undefined;
    });
  };
}
