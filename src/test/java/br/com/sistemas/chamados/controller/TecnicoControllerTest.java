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

import br.com.sistemas.chamados.dto.TecnicoRequest;
import br.com.sistemas.chamados.dto.TecnicoResponse;
import br.com.sistemas.chamados.enums.Perfil;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.TecnicoService;

@WebMvcTest(TecnicoController.class)
class TecnicoControllerTest {

    private static final String JSON_VALIDO =
        "{\"nome\":\"Maria Silva\",\"email\":\"maria@empresa.com\",\"perfil\":\"TECNICO\"}";

    @Autowired MockMvc mockMvc;
    @MockitoBean TecnicoService service;

    @Test
    void criaTecnicoRetorna201() throws Exception {
        when(service.criar(any(TecnicoRequest.class)))
            .thenReturn(new TecnicoResponse(1L, "Maria Silva", "maria@empresa.com", Perfil.TECNICO));

        mockMvc.perform(post("/tecnicos").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/tecnicos/1"))
            .andExpect(jsonPath("$.perfil").value("TECNICO"));
    }

    @Test
    void semPerfilRetorna400() throws Exception {
        mockMvc.perform(post("/tecnicos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Maria\",\"email\":\"maria@empresa.com\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalhes", hasItem("perfil: Perfil é obrigatório")));
        verifyNoInteractions(service);
    }

    @Test
    void perfilInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/tecnicos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Maria\",\"email\":\"maria@empresa.com\",\"perfil\":\"GERENTE\"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void emailDuplicadoRetorna409() throws Exception {
        when(service.criar(any(TecnicoRequest.class)))
            .thenThrow(new RegraNegocioException("Já existe técnico com o e-mail maria@empresa.com"));

        mockMvc.perform(post("/tecnicos").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro").value("Já existe técnico com o e-mail maria@empresa.com"));
    }

    @Test
    void buscarInexistenteRetorna404() throws Exception {
        when(service.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Técnico 99 não encontrado"));

        mockMvc.perform(get("/tecnicos/99")).andExpect(status().isNotFound());
    }

    @Test
    void excluirTecnicoComChamadosRetorna409() throws Exception {
        doThrow(new RegraNegocioException(
                "Não é possível excluir o técnico 1: existem 1 chamado(s) vinculado(s) a ele"))
            .when(service).excluir(1L);

        mockMvc.perform(delete("/tecnicos/1")).andExpect(status().isConflict());
    }
}
