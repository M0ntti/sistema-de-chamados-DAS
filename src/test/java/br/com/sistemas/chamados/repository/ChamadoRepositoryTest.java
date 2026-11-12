package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Comentario;
import br.com.sistemas.chamados.entity.HistoricoStatus;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.enums.Perfil;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import jakarta.persistence.EntityManager;

@DataJpaTest
class ChamadoRepositoryTest {

    @Autowired ChamadoRepository chamadoRepository;
    @Autowired ClienteRepository clienteRepository;
    @Autowired CategoriaRepository categoriaRepository;
    @Autowired TecnicoRepository tecnicoRepository;
    @Autowired ComentarioRepository comentarioRepository;
    @Autowired HistoricoStatusRepository historicoRepository;
    @Autowired EntityManager em;

    Cliente ana;
    Cliente bruno;
    Categoria hardware;
    Tecnico maria;

    @BeforeEach
    void cenario() {
        ana = clienteRepository.save(new Cliente("Ana Souza", "ana@exemplo.com", null));
        bruno = clienteRepository.save(new Cliente("Bruno Lima", "bruno@exemplo.com", null));
        hardware = categoriaRepository.save(new Categoria("Hardware", null));
        maria = tecnicoRepository.save(new Tecnico("Maria Silva", "maria@empresa.com", Perfil.TECNICO));
    }

    private Chamado salvar(String titulo, Prioridade prioridade, Cliente cliente, boolean fechado) {
        Chamado chamado = new Chamado(titulo, "descrição", prioridade, cliente, hardware, maria);
        if (fechado) {
            chamado.alterarStatus(StatusChamado.FECHADO);
        }
        return chamadoRepository.save(chamado);
    }

    private void criarChamados() {
        salvar("A - ana aberto alta", Prioridade.ALTA, ana, false);
        salvar("B - ana aberto baixa", Prioridade.BAIXA, ana, false);
        salvar("C - ana fechado alta", Prioridade.ALTA, ana, true);
        salvar("D - bruno aberto alta", Prioridade.ALTA, bruno, false);
    }

    private List<String> titulos(StatusChamado status, Prioridade prioridade, Long clienteId) {
        return chamadoRepository
            .findAll(ChamadoSpecification.comFiltros(status, prioridade, clienteId),
                     Sort.by("titulo"))
            .stream().map(Chamado::getTitulo).toList();
    }

    @Test
    void persisteChamadoAbertoComDataAberturaERelacionamentos() {
        Chamado salvo = salvar("Computador não liga", Prioridade.ALTA, ana, false);
        em.flush();
        em.clear();

        Chamado lido = chamadoRepository.findById(salvo.getId()).orElseThrow();

        assertThat(lido.getStatus()).isEqualTo(StatusChamado.ABERTO);
        assertThat(lido.getDataAbertura()).isNotNull();
        assertThat(lido.getDataFechamento()).isNull();
        assertThat(lido.getCliente().getNome()).isEqualTo("Ana Souza");
        assertThat(lido.getCategoria().getNome()).isEqualTo("Hardware");
        assertThat(lido.getTecnico().getNome()).isEqualTo("Maria Silva");
    }

    @Test
    void persisteDataFechamentoAoFechar() {
        Chamado salvo = salvar("Fechado", Prioridade.MEDIA, ana, true);
        em.flush();
        em.clear();

        Chamado lido = chamadoRepository.findById(salvo.getId()).orElseThrow();

        assertThat(lido.getStatus()).isEqualTo(StatusChamado.FECHADO);
        assertThat(lido.getDataFechamento()).isNotNull();
    }

    @Test
    void semFiltrosRetornaTodos() {
        criarChamados();

        assertThat(titulos(null, null, null)).hasSize(4);
    }

    @Test
    void filtraPorStatus() {
        criarChamados();

        assertThat(titulos(StatusChamado.ABERTO, null, null))
            .containsExactly("A - ana aberto alta", "B - ana aberto baixa", "D - bruno aberto alta");
        assertThat(titulos(StatusChamado.FECHADO, null, null))
            .containsExactly("C - ana fechado alta");
        assertThat(titulos(StatusChamado.EM_ATENDIMENTO, null, null)).isEmpty();
    }

    @Test
    void filtraPorPrioridade() {
        criarChamados();

        assertThat(titulos(null, Prioridade.ALTA, null))
            .containsExactly("A - ana aberto alta", "C - ana fechado alta", "D - bruno aberto alta");
        assertThat(titulos(null, Prioridade.CRITICA, null)).isEmpty();
    }

    @Test
    void filtraPorCliente() {
        criarChamados();

        assertThat(titulos(null, null, ana.getId())).hasSize(3);
        assertThat(titulos(null, null, bruno.getId())).containsExactly("D - bruno aberto alta");
    }

    @Test
    void combinaFiltros() {
        criarChamados();

        assertThat(titulos(StatusChamado.ABERTO, Prioridade.ALTA, null))
            .containsExactly("A - ana aberto alta", "D - bruno aberto alta");
        assertThat(titulos(StatusChamado.ABERTO, Prioridade.ALTA, ana.getId()))
            .containsExactly("A - ana aberto alta");
        assertThat(titulos(StatusChamado.FECHADO, Prioridade.BAIXA, ana.getId())).isEmpty();
    }

    @Test
    void paginaEOrdena() {
        criarChamados();
        var todos = ChamadoSpecification.comFiltros(null, null, null);

        Page<Chamado> primeira = chamadoRepository.findAll(
            todos, PageRequest.of(0, 3, Sort.by(Sort.Direction.ASC, "titulo")));
        Page<Chamado> segunda = chamadoRepository.findAll(
            todos, PageRequest.of(1, 3, Sort.by(Sort.Direction.ASC, "titulo")));

        assertThat(primeira.getTotalElements()).isEqualTo(4);
        assertThat(primeira.getTotalPages()).isEqualTo(2);
        assertThat(primeira.getContent()).extracting(Chamado::getTitulo)
            .containsExactly("A - ana aberto alta", "B - ana aberto baixa", "C - ana fechado alta");
        assertThat(segunda.getContent()).extracting(Chamado::getTitulo)
            .containsExactly("D - bruno aberto alta");
        assertThat(segunda.isLast()).isTrue();
    }

    @Test
    void ordenaDescendente() {
        criarChamados();

        Page<Chamado> pagina = chamadoRepository.findAll(
            ChamadoSpecification.comFiltros(null, null, null),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "titulo")));

        assertThat(pagina.getContent()).extracting(Chamado::getTitulo)
            .containsExactly("D - bruno aberto alta", "C - ana fechado alta",
                             "B - ana aberto baixa", "A - ana aberto alta");
    }

    @Test
    void paginaComFiltroContaSoOsFiltrados() {
        criarChamados();

        Page<Chamado> pagina = chamadoRepository.findAll(
            ChamadoSpecification.comFiltros(StatusChamado.ABERTO, null, ana.getId()),
            PageRequest.of(0, 1, Sort.by("titulo")));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getTotalPages()).isEqualTo(2);
        assertThat(pagina.getContent()).hasSize(1);
    }

    @Test
    void contaChamadosPorClienteCategoriaETecnico() {
        criarChamados();

        assertThat(chamadoRepository.countByClienteId(ana.getId())).isEqualTo(3);
        assertThat(chamadoRepository.countByClienteId(bruno.getId())).isEqualTo(1);
        assertThat(chamadoRepository.countByCategoriaId(hardware.getId())).isEqualTo(4);
        assertThat(chamadoRepository.countByTecnicoId(maria.getId())).isEqualTo(4);
    }

    @Test
    void comentariosVoltamNaOrdemDeCriacao() {
        Chamado chamado = salvar("Com comentários", Prioridade.MEDIA, ana, false);
        comentarioRepository.save(new Comentario(chamado, "Ana", "primeiro"));
        comentarioRepository.save(new Comentario(chamado, "Maria", "segundo"));
        em.flush();
        em.clear();

        Chamado lido = chamadoRepository.findById(chamado.getId()).orElseThrow();

        assertThat(lido.getComentarios()).extracting(Comentario::getTexto)
            .containsExactly("primeiro", "segundo");
        assertThat(lido.getComentarios().get(0).getDataCriacao()).isNotNull();
    }

    @Test
    void excluirChamadoRemoveComentariosEHistorico() {
        Chamado chamado = salvar("Será excluído", Prioridade.MEDIA, ana, false);
        comentarioRepository.save(new Comentario(chamado, "Ana", "um comentário"));
        historicoRepository.save(
            new HistoricoStatus(chamado, StatusChamado.ABERTO, StatusChamado.EM_ATENDIMENTO));
        em.flush();
        em.clear();
        assertThat(comentarioRepository.count()).isEqualTo(1);
        assertThat(historicoRepository.count()).isEqualTo(1);

        chamadoRepository.delete(chamadoRepository.findById(chamado.getId()).orElseThrow());
        em.flush();

        assertThat(chamadoRepository.count()).isZero();
        assertThat(comentarioRepository.count()).isZero();
        assertThat(historicoRepository.count()).isZero();
    }
}
