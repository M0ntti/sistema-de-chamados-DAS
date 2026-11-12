package br.com.sistemas.chamados.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.service.ComentarioService;

@WebMvcTest(ComentarioController.class)
class ComentarioControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ComentarioService service;

    private static ComentarioResponse comentario(Long id, String autor, String texto) {
        return new ComentarioResponse(id, 1L, autor, texto, LocalDateTime.of(2026, 11, 5, 11, 0));
    }

    @Test
    void criaComentarioRetorna201() throws Exception {
        when(service.criar(eq(1L), any(ComentarioRequest.class)))
            .thenReturn(comentario(1L, "Maria", "Estou verificando"));

        mockMvc.perform(post("/chamados/1/comentarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"autor\":\"Maria\",\"texto\":\"Estou verificando\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.chamadoId").value(1))
            .andExpect(jsonPath("$.autor").value("Maria"))
            .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    void textoVazioRetorna400() throws Exception {
        mockMvc.perform(post("/chamados/1/comentarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"autor\":\"Maria\",\"texto\":\"  \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalhes", hasItem("texto: Texto é obrigatório")));
        verifyNoInteractions(service);
    }

    @Test
    void autorAusenteRetorna400() throws Exception {
        mockMvc.perform(post("/chamados/1/comentarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"Oi\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalhes", hasItem("autor: Autor é obrigatório")));
        verifyNoInteractions(service);
    }

    @Test
    void comentarEmChamadoInexistenteRetorna404() throws Exception {
        when(service.criar(eq(99L), any(ComentarioRequest.class)))
            .thenThrow(new RecursoNaoEncontradoException("Chamado 99 não encontrado"));

        mockMvc.perform(post("/chamados/99/comentarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"autor\":\"Maria\",\"texto\":\"Oi\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void comentarEmChamadoFechadoRetorna409() throws Exception {
        when(service.criar(eq(1L), any(ComentarioRequest.class)))
            .thenThrow(new ChamadoFechadoException("Chamado 1 está FECHADO e não pode ser alterado"));

        mockMvc.perform(post("/chamados/1/comentarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"autor\":\"Maria\",\"texto\":\"Oi\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void listaComentarios() throws Exception {
        when(service.listar(1L)).thenReturn(List.of(
            comentario(1L, "Ana", "Não liga"), comentario(2L, "Maria", "Vou verificar")));

        mockMvc.perform(get("/chamados/1/comentarios"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].autor").value("Ana"))
            .andExpect(jsonPath("$[1].texto").value("Vou verificar"));
    }

    @Test
    void listarDeChamadoInexistenteRetorna404() throws Exception {
        when(service.listar(99L)).thenThrow(new RecursoNaoEncontradoException("Chamado 99 não encontrado"));

        mockMvc.perform(get("/chamados/99/comentarios")).andExpect(status().isNotFound());
    }
}
