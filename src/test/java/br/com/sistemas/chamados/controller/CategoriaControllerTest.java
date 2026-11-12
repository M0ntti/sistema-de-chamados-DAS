package br.com.sistemas.chamados.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.CategoriaRequest;
import br.com.sistemas.chamados.dto.CategoriaResponse;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.CategoriaService;

@WebMvcTest(CategoriaController.class)
class CategoriaControllerTest {

    private static final String JSON_VALIDO = "{\"nome\":\"Hardware\",\"descricao\":\"Equipamentos\"}";

    @Autowired MockMvc mockMvc;
    @MockitoBean CategoriaService service;

    @Test
    void criaCategoriaRetorna201() throws Exception {
        when(service.criar(any(CategoriaRequest.class)))
            .thenReturn(new CategoriaResponse(1L, "Hardware", "Equipamentos"));

        mockMvc.perform(post("/categorias").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/categorias/1"))
            .andExpect(jsonPath("$.nome").value("Hardware"));
    }

    @Test
    void nomeVazioRetorna400() throws Exception {
        mockMvc.perform(post("/categorias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalhes", hasItem("nome: Nome é obrigatório")));
        verifyNoInteractions(service);
    }

    @Test
    void nomeDuplicadoRetorna409() throws Exception {
        when(service.criar(any(CategoriaRequest.class)))
            .thenThrow(new RegraNegocioException("Já existe categoria com o nome Hardware"));

        mockMvc.perform(post("/categorias").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro").value("Já existe categoria com o nome Hardware"));
    }

    @Test
    void buscarInexistenteRetorna404() throws Exception {
        when(service.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Categoria 99 não encontrada"));

        mockMvc.perform(get("/categorias/99")).andExpect(status().isNotFound());
    }

    @Test
    void excluirCategoriaComChamadosRetorna409() throws Exception {
        doThrow(new RegraNegocioException(
                "Não é possível excluir a categoria 1: existem 1 chamado(s) vinculado(s) a ela"))
            .when(service).excluir(1L);

        mockMvc.perform(delete("/categorias/1")).andExpect(status().isConflict());
    }
}
