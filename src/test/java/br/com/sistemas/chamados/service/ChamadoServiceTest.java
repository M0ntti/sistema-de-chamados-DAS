package br.com.sistemas.chamados.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.AtribuirTecnicoRequest;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.PaginaResponse;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.HistoricoStatus;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.enums.Perfil;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.ParametroInvalidoException;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.TransicaoStatusInvalidaException;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.repository.HistoricoStatusRepository;
import br.com.sistemas.chamados.repository.TecnicoRepository;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {

    @Mock ChamadoRepository repository;
    @Mock ClienteRepository clienteRepository;
    @Mock CategoriaRepository categoriaRepository;
    @Mock TecnicoRepository tecnicoRepository;
    @Mock HistoricoStatusRepository historicoRepository;

    @InjectMocks ChamadoService service;

    private static Chamado chamadoComId(Long id) {
        Chamado chamado = new Chamado("Computador não liga", "Não dá vídeo", Prioridade.ALTA,
            new Cliente("Ana Souza", "ana@exemplo.com", null),
            new Categoria("Hardware", null),
            new Tecnico("Maria Silva", "maria@empresa.com", Perfil.TECNICO));
        ReflectionTestUtils.setField(chamado, "id", id);
        return chamado;
    }

    @Test
    void listarAceitaOrdenacaoPorCampoPermitido() {
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "dataAbertura"));
        when(repository.findAll(ArgumentMatchers.<Specification<Chamado>>any(), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(chamadoComId(1L)), pageable, 1));

        PaginaResponse<ChamadoResponse> resposta =
            service.listar(StatusChamado.ABERTO, Prioridade.ALTA, 1L, pageable);

        assertThat(resposta.conteudo()).hasSize(1);
        assertThat(resposta.totalElementos()).isEqualTo(1);
        assertThat(resposta.tamanho()).isEqualTo(5);
    }

    @Test
    void listarRejeitaOrdenacaoPorCampoInexistente() {
        Pageable pageable = PageRequest.of(0, 5, Sort.by("campoQueNaoExiste"));

        assertThatThrownBy(() -> service.listar(null, null, null, pageable))
            .isInstanceOf(ParametroInvalidoException.class)
            .hasMessageContaining("campoQueNaoExiste");
        verifyNoInteractions(repository);
    }

    @Test
    void listarPorClienteInexistenteLanca404() {
        when(clienteRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() ->
                service.listarPorCliente(99L, null, null, PageRequest.of(0, 10)))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessage("Cliente 99 não encontrado");
        verifyNoInteractions(repository);
    }

    @Test
    void atribuirTrocaOTecnico() {
        Chamado chamado = chamadoComId(1L);
        Tecnico novo = new Tecnico("Joao", "joao@empresa.com", Perfil.TECNICO);
        when(repository.findById(1L)).thenReturn(Optional.of(chamado));
        when(tecnicoRepository.findById(2L)).thenReturn(Optional.of(novo));

        service.atribuirTecnico(1L, new AtribuirTecnicoRequest(2L));

        assertThat(chamado.getTecnico()).isSameAs(novo);
    }

    @Test
    void atribuirEmChamadoFechadoLanca409SemNemBuscarOTecnico() {
        Chamado chamado = chamadoComId(1L);
        chamado.alterarStatus(StatusChamado.FECHADO);
        when(repository.findById(1L)).thenReturn(Optional.of(chamado));

        assertThatThrownBy(() -> service.atribuirTecnico(1L, new AtribuirTecnicoRequest(2L)))
            .isInstanceOf(ChamadoFechadoException.class);
        verifyNoInteractions(tecnicoRepository);
    }

    @Test
    void atribuirEmChamadoInexistenteLanca404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atribuirTecnico(99L, new AtribuirTecnicoRequest(2L)))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessage("Chamado 99 não encontrado");
    }

    @Test
    void atribuirTecnicoInexistenteLanca404() {
        when(repository.findById(1L)).thenReturn(Optional.of(chamadoComId(1L)));
        when(tecnicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atribuirTecnico(1L, new AtribuirTecnicoRequest(99L)))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessage("Técnico 99 não encontrado");
    }

    @Test
    void alterarStatusAtualizaEGravaHistorico() {
        Chamado chamado = chamadoComId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(chamado));

        ChamadoResponse resposta =
            service.alterarStatus(1L, new AlterarStatusRequest(StatusChamado.EM_ATENDIMENTO));

        assertThat(resposta.status()).isEqualTo(StatusChamado.EM_ATENDIMENTO);
        ArgumentCaptor<HistoricoStatus> captor = ArgumentCaptor.forClass(HistoricoStatus.class);
        verify(historicoRepository).save(captor.capture());
        assertThat(captor.getValue().getStatusAnterior()).isEqualTo(StatusChamado.ABERTO);
        assertThat(captor.getValue().getStatusNovo()).isEqualTo(StatusChamado.EM_ATENDIMENTO);
        assertThat(captor.getValue().getDataAlteracao()).isNotNull();
    }

    @Test
    void fecharPreencheDataFechamentoNaResposta() {
        when(repository.findById(1L)).thenReturn(Optional.of(chamadoComId(1L)));

        ChamadoResponse resposta =
            service.alterarStatus(1L, new AlterarStatusRequest(StatusChamado.FECHADO));

        assertThat(resposta.status()).isEqualTo(StatusChamado.FECHADO);
        assertThat(resposta.dataFechamento()).isNotNull();
    }

    @Test
    void transicaoInvalidaNaoGravaHistorico() {
        when(repository.findById(1L)).thenReturn(Optional.of(chamadoComId(1L)));

        assertThatThrownBy(() ->
                service.alterarStatus(1L, new AlterarStatusRequest(StatusChamado.AGUARDANDO_CLIENTE)))
            .isInstanceOf(TransicaoStatusInvalidaException.class);
        verify(historicoRepository, never()).save(any(HistoricoStatus.class));
    }

    @Test
    void alterarStatusDeChamadoFechadoLanca409() {
        Chamado chamado = chamadoComId(1L);
        chamado.alterarStatus(StatusChamado.FECHADO);
        when(repository.findById(1L)).thenReturn(Optional.of(chamado));

        assertThatThrownBy(() ->
                service.alterarStatus(1L, new AlterarStatusRequest(StatusChamado.EM_ATENDIMENTO)))
            .isInstanceOf(ChamadoFechadoException.class);
        verify(historicoRepository, never()).save(any(HistoricoStatus.class));
    }

    @Test
    void atualizarChamadoFechadoLanca409() {
        Chamado chamado = chamadoComId(1L);
        chamado.alterarStatus(StatusChamado.FECHADO);
        when(repository.findById(1L)).thenReturn(Optional.of(chamado));
        ChamadoRequest dto = new ChamadoRequest("Novo", "Novo", Prioridade.BAIXA, 1L, 1L, 1L);

        assertThatThrownBy(() -> service.atualizar(1L, dto))
            .isInstanceOf(ChamadoFechadoException.class);
        verifyNoInteractions(clienteRepository, categoriaRepository, tecnicoRepository);
    }
}
