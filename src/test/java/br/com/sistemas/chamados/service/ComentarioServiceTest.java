package br.com.sistemas.chamados.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Comentario;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.enums.Perfil;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ComentarioRepository;

@ExtendWith(MockitoExtension.class)
class ComentarioServiceTest {

    @Mock ComentarioRepository repository;
    @Mock ChamadoRepository chamadoRepository;

    @InjectMocks ComentarioService service;

    private static Chamado chamadoComId(Long id) {
        Chamado chamado = new Chamado("Computador não liga", "Não dá vídeo", Prioridade.ALTA,
            new Cliente("Ana Souza", "ana@exemplo.com", null),
            new Categoria("Hardware", null),
            new Tecnico("Maria Silva", "maria@empresa.com", Perfil.TECNICO));
        ReflectionTestUtils.setField(chamado, "id", id);
        return chamado;
    }

    @Test
    void criaComentarioComDataAutomatica() {
        when(chamadoRepository.findById(1L)).thenReturn(Optional.of(chamadoComId(1L)));
        when(repository.save(any(Comentario.class))).thenAnswer(inv -> inv.getArgument(0));

        ComentarioResponse resposta =
            service.criar(1L, new ComentarioRequest("Maria", "Estou verificando"));

        assertThat(resposta.autor()).isEqualTo("Maria");
        assertThat(resposta.texto()).isEqualTo("Estou verificando");
        assertThat(resposta.chamadoId()).isEqualTo(1L);
        assertThat(resposta.data()).isNotNull();
    }

    @Test
    void comentarEmChamadoInexistenteLanca404() {
        when(chamadoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(99L, new ComentarioRequest("Maria", "Oi")))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessage("Chamado 99 não encontrado");
        verify(repository, never()).save(any(Comentario.class));
    }

    @Test
    void comentarEmChamadoFechadoLanca409() {
        Chamado chamado = chamadoComId(1L);
        chamado.alterarStatus(StatusChamado.FECHADO);
        when(chamadoRepository.findById(1L)).thenReturn(Optional.of(chamado));

        assertThatThrownBy(() -> service.criar(1L, new ComentarioRequest("Maria", "Oi")))
            .isInstanceOf(ChamadoFechadoException.class);
        verify(repository, never()).save(any(Comentario.class));
    }

    @Test
    void listarDeChamadoInexistenteLanca404() {
        when(chamadoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar(99L))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
