package br.com.sistemas.chamados.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.AtribuirTecnicoRequest;
import br.com.sistemas.chamados.dto.ChamadoDetalheResponse;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.dto.HistoricoStatusResponse;
import br.com.sistemas.chamados.dto.PaginaResponse;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.ParametroInvalidoException;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.TransicaoStatusInvalidaException;
import br.com.sistemas.chamados.service.ChamadoService;

@WebMvcTest(ChamadoController.class)
@EnableSpringDataWebSupport
class ChamadoControllerTest {

    private static final String JSON_VALIDO = """
        {"titulo":"Computador não liga","descricao":"Não dá vídeo","prioridade":"ALTA",
         "clienteId":1,"categoriaId":1,"tecnicoId":1}""";

    @Autowired MockMvc mockMvc;
    @MockitoBean ChamadoService service;

    private static ChamadoResponse chamado(Long id, StatusChamado status, Long tecnicoId) {
        return new ChamadoResponse(id, "Computador não liga", "Não dá vídeo",
            LocalDateTime.of(2026, 11, 5, 10, 0),
            status == StatusChamado.FECHADO ? LocalDateTime.of(2026, 11, 5, 12, 0) : null,
            Prioridade.ALTA, status, 1L, "Ana Souza", 1L, "Hardware", tecnicoId, "Maria Silva");
    }

    private static PaginaResponse<ChamadoResponse> pagina(ChamadoResponse... itens) {
        return new PaginaResponse<>(List.of(itens), 0, 10, itens.length, 1, true, true);
    }

    @Test
    void criaChamadoRetorna201ComLocation() throws Exception {
        when(service.criar(any(ChamadoRequest.class)))
            .thenReturn(chamado(1L, StatusChamado.ABERTO, 1L));

        mockMvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/chamados/1"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("ABERTO"))
            .andExpect(jsonPath("$.tecnicoNome").value("Maria Silva"));
    }

    @Test
    void criarSemTituloRetorna400ComDetalhes() throws Exception {
        String json = """
            {"titulo":"","descricao":"d","prioridade":"ALTA","clienteId":1,"categoriaId":1,"tecnicoId":1}""";

        mockMvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.erro").value("Dados inválidos"))
            .andExpect(jsonPath("$.detalhes", hasItem("titulo: Título é obrigatório")));
        verifyNoInteractions(service);
    }

    @Test
    void criarSemTecnicoRetorna400() throws Exception {
        String json = """
            {"titulo":"t","descricao":"d","prioridade":"ALTA","clienteId":1,"categoriaId":1}""";

        mockMvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalhes", hasItem("tecnicoId: Técnico é obrigatório")));
        verifyNoInteractions(service);
    }

    @Test
    void criarComPrioridadeInvalidaRetorna400() throws Exception {
        String json = """
            {"titulo":"t","descricao":"d","prioridade":"URGENTE","clienteId":1,"categoriaId":1,"tecnicoId":1}""";

        mockMvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(service);
    }

    @Test
    void criarComClienteInexistenteRetorna404() throws Exception {
        when(service.criar(any(ChamadoRequest.class)))
            .thenThrow(new RecursoNaoEncontradoException("Cliente 1 não encontrado"));

        mockMvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.erro").value("Cliente 1 não encontrado"));
    }


    @Test
    void buscarRetornaDetalheComComentarios() throws Exception {
        ComentarioResponse comentario = new ComentarioResponse(
            1L, 1L, "Ana", "Continua sem ligar", LocalDateTime.of(2026, 11, 5, 11, 0));
        when(service.buscar(1L)).thenReturn(new ChamadoDetalheResponse(
            1L, "Computador não liga", "Não dá vídeo",
            LocalDateTime.of(2026, 11, 5, 10, 0), null, Prioridade.ALTA, StatusChamado.ABERTO,
            1L, "Ana Souza", 1L, "Hardware", 1L, "Maria Silva", List.of(comentario)));

        mockMvc.perform(get("/chamados/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titulo").value("Computador não liga"))
            .andExpect(jsonPath("$.comentarios.length()").value(1))
            .andExpect(jsonPath("$.comentarios[0].autor").value("Ana"));
    }

    @Test
    void buscarInexistenteRetorna404() throws Exception {
        when(service.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Chamado 99 não encontrado"));

        mockMvc.perform(get("/chamados/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.erro").value("Chamado 99 não encontrado"));
    }

    @Test
    void idNaoNumericoRetorna400() throws Exception {
        mockMvc.perform(get("/chamados/abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro", containsString("'id'")));
        verifyNoInteractions(service);
    }

    @Test
    void listarSemParametrosUsaPaginacaoPadrao() throws Exception {
        when(service.listar(any(), any(), any(), any(Pageable.class)))
            .thenReturn(pagina(chamado(1L, StatusChamado.ABERTO, 1L)));

        mockMvc.perform(get("/chamados"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.conteudo.length()").value(1))
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.pagina").value(0));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).listar(isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThat(captor.getValue().getSort().getOrderFor("dataAbertura").getDirection())
            .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void listarRepassaFiltrosPaginaEOrdenacao() throws Exception {
        when(service.listar(any(), any(), any(), any(Pageable.class))).thenReturn(pagina());

        mockMvc.perform(get("/chamados")
                .param("status", "ABERTO")
                .param("prioridade", "ALTA")
                .param("clienteId", "5")
                .param("page", "2")
                .param("size", "5")
                .param("sort", "titulo,asc"))
            .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).listar(
            eq(StatusChamado.ABERTO), eq(Prioridade.ALTA), eq(5L), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort().getOrderFor("titulo").getDirection())
            .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void listarComStatusInvalidoRetorna400() throws Exception {
        mockMvc.perform(get("/chamados").param("status", "INEXISTENTE"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro", containsString("status")));
        verifyNoInteractions(service);
    }

    @Test
    void listarComPrioridadeInvalidaRetorna400() throws Exception {
        mockMvc.perform(get("/chamados").param("prioridade", "URGENTE"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro", containsString("prioridade")));
        verifyNoInteractions(service);
    }

    @Test
    void listarComOrdenacaoInvalidaRetorna400() throws Exception {
        when(service.listar(any(), any(), any(), any(Pageable.class)))
            .thenThrow(new ParametroInvalidoException("Campo de ordenação inválido: 'xyz'"));

        mockMvc.perform(get("/chamados").param("sort", "xyz,asc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("Campo de ordenação inválido: 'xyz'"));
    }

    @Test
    void atualizarChamadoFechadoRetorna409() throws Exception {
        when(service.atualizar(eq(1L), any(ChamadoRequest.class)))
            .thenThrow(new ChamadoFechadoException("Chamado 1 está FECHADO e não pode ser alterado"));

        mockMvc.perform(put("/chamados/1").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.erro").value("Chamado 1 está FECHADO e não pode ser alterado"));
    }

    @Test
    void excluirRetorna204() throws Exception {
        mockMvc.perform(delete("/chamados/1")).andExpect(status().isNoContent());
        verify(service).excluir(1L);
    }

    @Test
    void excluirInexistenteRetorna404() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Chamado 99 não encontrado"))
            .when(service).excluir(99L);

        mockMvc.perform(delete("/chamados/99")).andExpect(status().isNotFound());
    }

    @Test
    void atribuirRetorna200ComNovoTecnico() throws Exception {
        when(service.atribuirTecnico(eq(1L), any(AtribuirTecnicoRequest.class)))
            .thenReturn(chamado(1L, StatusChamado.ABERTO, 2L));

        mockMvc.perform(patch("/chamados/1/atribuir")
                .contentType(MediaType.APPLICATION_JSON).content("{\"tecnicoId\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tecnicoId").value(2));
    }

    @Test
    void atribuirSemTecnicoRetorna400() throws Exception {
        mockMvc.perform(patch("/chamados/1/atribuir")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalhes", hasItem("tecnicoId: Técnico é obrigatório")));
        verifyNoInteractions(service);
    }

    @Test
    void atribuirEmChamadoFechadoRetorna409() throws Exception {
        when(service.atribuirTecnico(eq(1L), any(AtribuirTecnicoRequest.class)))
            .thenThrow(new ChamadoFechadoException("Chamado 1 está FECHADO e não pode ser alterado"));

        mockMvc.perform(patch("/chamados/1/atribuir")
                .contentType(MediaType.APPLICATION_JSON).content("{\"tecnicoId\":2}"))
            .andExpect(status().isConflict());
    }

    @Test
    void atribuirTecnicoInexistenteRetorna404() throws Exception {
        when(service.atribuirTecnico(eq(1L), any(AtribuirTecnicoRequest.class)))
            .thenThrow(new RecursoNaoEncontradoException("Técnico 99 não encontrado"));

        mockMvc.perform(patch("/chamados/1/atribuir")
                .contentType(MediaType.APPLICATION_JSON).content("{\"tecnicoId\":99}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.erro").value("Técnico 99 não encontrado"));
    }

    @Test
    void alterarStatusRetorna200() throws Exception {
        when(service.alterarStatus(eq(1L), any(AlterarStatusRequest.class)))
            .thenReturn(chamado(1L, StatusChamado.EM_ATENDIMENTO, 1L));

        mockMvc.perform(patch("/chamados/1/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"EM_ATENDIMENTO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"));
    }

    @Test
    void fecharRetornaDataFechamento() throws Exception {
        when(service.alterarStatus(eq(1L), any(AlterarStatusRequest.class)))
            .thenReturn(chamado(1L, StatusChamado.FECHADO, 1L));

        mockMvc.perform(patch("/chamados/1/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"FECHADO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FECHADO"))
            .andExpect(jsonPath("$.dataFechamento").isNotEmpty());
    }

    @Test
    void transicaoInvalidaRetorna409() throws Exception {
        when(service.alterarStatus(eq(1L), any(AlterarStatusRequest.class)))
            .thenThrow(new TransicaoStatusInvalidaException(
                "Transição de status inválida: ABERTO -> AGUARDANDO_CLIENTE"));

        mockMvc.perform(patch("/chamados/1/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"AGUARDANDO_CLIENTE\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro").value("Transição de status inválida: ABERTO -> AGUARDANDO_CLIENTE"));
    }

    @Test
    void alterarStatusDeChamadoFechadoRetorna409() throws Exception {
        when(service.alterarStatus(eq(1L), any(AlterarStatusRequest.class)))
            .thenThrow(new ChamadoFechadoException("Chamado 1 está FECHADO e não pode ser alterado"));

        mockMvc.perform(patch("/chamados/1/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"EM_ATENDIMENTO\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void statusInexistenteRetorna400() throws Exception {
        mockMvc.perform(patch("/chamados/1/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELADO\"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void statusAusenteRetorna400() throws Exception {
        mockMvc.perform(patch("/chamados/1/status")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalhes", hasItem("status: Status é obrigatório")));
        verifyNoInteractions(service);
    }

    @Test
    void alterarStatusDeChamadoInexistenteRetorna404() throws Exception {
        when(service.alterarStatus(eq(99L), any(AlterarStatusRequest.class)))
            .thenThrow(new RecursoNaoEncontradoException("Chamado 99 não encontrado"));

        mockMvc.perform(patch("/chamados/99/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"FECHADO\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void historicoRetornaMudancasDeStatus() throws Exception {
        when(service.historico(1L)).thenReturn(List.of(
            new HistoricoStatusResponse(1L, StatusChamado.ABERTO, StatusChamado.EM_ATENDIMENTO,
                LocalDateTime.of(2026, 11, 5, 11, 0)),
            new HistoricoStatusResponse(2L, StatusChamado.EM_ATENDIMENTO, StatusChamado.FECHADO,
                LocalDateTime.of(2026, 11, 5, 12, 0))));

        mockMvc.perform(get("/chamados/1/historico"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].statusAnterior").value("ABERTO"))
            .andExpect(jsonPath("$[1].statusNovo").value("FECHADO"));
    }

    @Test
    void historicoDeChamadoInexistenteRetorna404() throws Exception {
        when(service.historico(99L)).thenThrow(new RecursoNaoEncontradoException("Chamado 99 não encontrado"));

        mockMvc.perform(get("/chamados/99/historico")).andExpect(status().isNotFound());
    }
}
