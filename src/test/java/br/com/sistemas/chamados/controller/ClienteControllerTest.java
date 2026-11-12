package br.com.sistemas.chamados.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.dto.PaginaResponse;
import br.com.sistemas.chamados.enums.StatusChamado;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ClienteService;

@WebMvcTest(ClienteController.class)
@EnableSpringDataWebSupport
class ClienteControllerTest {

    private static final String JSON_VALIDO =
        "{\"nome\":\"Ana Souza\",\"email\":\"ana@exemplo.com\",\"telefone\":\"47999999999\"}";

    @Autowired MockMvc mockMvc;
    @MockitoBean ClienteService service;
    @MockitoBean ChamadoService chamadoService;

    @Test
    void criaClienteRetorna201() throws Exception {
        when(service.criar(any(ClienteRequest.class)))
            .thenReturn(new ClienteResponse(1L, "Ana Souza", "ana@exemplo.com", "47999999999"));

        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/clientes/1"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("ana@exemplo.com"));
    }

    @Test
    void nomeVazioRetorna400() throws Exception {
        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\",\"email\":\"ana@exemplo.com\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("Dados inválidos"))
            .andExpect(jsonPath("$.detalhes", hasItem("nome: Nome é obrigatório")));
        verifyNoInteractions(service);
    }

    @Test
    void emailInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Ana\",\"email\":\"nao-e-email\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalhes", hasItem("email: Email inválido")));
        verifyNoInteractions(service);
    }

    @Test
    void emailComMaisDe120CaracteresRetorna400() throws Exception {
        String emailLongo = "a".repeat(60) + "@" + "b".repeat(60) + ".com";

        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Ana\",\"email\":\"" + emailLongo + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalhes", hasItem("email: Email deve ter no máximo 120 caracteres")));
        verifyNoInteractions(service);
    }

    @Test
    void corpoAusenteRetorna400() throws Exception {
        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(service);
    }

    @Test
    void emailDuplicadoRetorna409() throws Exception {
        when(service.criar(any(ClienteRequest.class)))
            .thenThrow(new RegraNegocioException("Já existe cliente com o e-mail ana@exemplo.com"));

        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.erro").value("Já existe cliente com o e-mail ana@exemplo.com"));
    }

    @Test
    void buscaClienteExistente() throws Exception {
        when(service.buscar(1L))
            .thenReturn(new ClienteResponse(1L, "Ana Souza", "ana@exemplo.com", null));

        mockMvc.perform(get("/clientes/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Ana Souza"));
    }

    @Test
    void buscarInexistenteRetorna404() throws Exception {
        when(service.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Cliente 99 não encontrado"));

        mockMvc.perform(get("/clientes/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.erro").value("Cliente 99 não encontrado"));
    }

    @Test
    void listaClientes() throws Exception {
        when(service.listar()).thenReturn(List.of(
            new ClienteResponse(1L, "Ana", "ana@exemplo.com", null),
            new ClienteResponse(2L, "Bruno", "bruno@exemplo.com", null)));

        mockMvc.perform(get("/clientes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void atualizaCliente() throws Exception {
        when(service.atualizar(eq(1L), any(ClienteRequest.class)))
            .thenReturn(new ClienteResponse(1L, "Ana Souza", "ana@exemplo.com", "47999999999"));

        mockMvc.perform(put("/clientes/1").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.telefone").value("47999999999"));
    }

    @Test
    void excluirRetorna204() throws Exception {
        mockMvc.perform(delete("/clientes/1")).andExpect(status().isNoContent());
        verify(service).excluir(1L);
    }

    @Test
    void excluirClienteComChamadosRetorna409ComMensagemClara() throws Exception {
        doThrow(new RegraNegocioException(
                "Não é possível excluir o cliente 1: existem 2 chamado(s) vinculado(s) a ele"))
            .when(service).excluir(1L);

        mockMvc.perform(delete("/clientes/1"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro").value(
                "Não é possível excluir o cliente 1: existem 2 chamado(s) vinculado(s) a ele"));
    }

    @Test
    void listaChamadosDoCliente() throws Exception {
        when(chamadoService.listarPorCliente(eq(1L), any(), any(), any(Pageable.class)))
            .thenReturn(new PaginaResponse<ChamadoResponse>(List.of(), 0, 10, 0, 0, true, true));

        mockMvc.perform(get("/clientes/1/chamados").param("status", "ABERTO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(0));

        verify(chamadoService).listarPorCliente(
            eq(1L), eq(StatusChamado.ABERTO), isNull(), any(Pageable.class));
    }

    @Test
    void chamadosDeClienteInexistenteRetorna404() throws Exception {
        when(chamadoService.listarPorCliente(eq(99L), any(), any(), any(Pageable.class)))
            .thenThrow(new RecursoNaoEncontradoException("Cliente 99 não encontrado"));

        mockMvc.perform(get("/clientes/99/chamados"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.erro").value("Cliente 99 não encontrado"));
    }
}
