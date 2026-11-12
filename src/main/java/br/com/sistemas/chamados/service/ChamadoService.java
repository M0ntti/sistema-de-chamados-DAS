package br.com.sistemas.chamados.service;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.AtribuirTecnicoRequest;
import br.com.sistemas.chamados.dto.ChamadoDetalheResponse;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.HistoricoStatusResponse;
import br.com.sistemas.chamados.dto.PaginaResponse;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.HistoricoStatus;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import br.com.sistemas.chamados.exception.ParametroInvalidoException;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ChamadoSpecification;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.repository.HistoricoStatusRepository;
import br.com.sistemas.chamados.repository.TecnicoRepository;

@Service
public class ChamadoService {

    private static final Set<String> CAMPOS_ORDENAVEIS =
        Set.of("id", "titulo", "prioridade", "status", "dataAbertura", "dataFechamento");

    private final ChamadoRepository repository;
    private final ClienteRepository clienteRepository;
    private final CategoriaRepository categoriaRepository;
    private final TecnicoRepository tecnicoRepository;
    private final HistoricoStatusRepository historicoRepository;

    public ChamadoService(ChamadoRepository repository,
                          ClienteRepository clienteRepository,
                          CategoriaRepository categoriaRepository,
                          TecnicoRepository tecnicoRepository,
                          HistoricoStatusRepository historicoRepository) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.categoriaRepository = categoriaRepository;
        this.tecnicoRepository = tecnicoRepository;
        this.historicoRepository = historicoRepository;
    }

    @Transactional
    public ChamadoResponse criar(ChamadoRequest dto) {
        Chamado chamado = new Chamado(
            dto.titulo(), dto.descricao(), dto.prioridade(),
            buscarCliente(dto.clienteId()),
            buscarCategoria(dto.categoriaId()),
            buscarTecnico(dto.tecnicoId()));
        return ChamadoResponse.de(repository.save(chamado));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ChamadoResponse> listar(
            StatusChamado status, Prioridade prioridade, Long clienteId, Pageable pageable) {
        validarOrdenacao(pageable);
        Page<Chamado> pagina = repository.findAll(
            ChamadoSpecification.comFiltros(status, prioridade, clienteId), pageable);
        return PaginaResponse.de(pagina, ChamadoResponse::de);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ChamadoResponse> listarPorCliente(
            Long clienteId, StatusChamado status, Prioridade prioridade, Pageable pageable) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new RecursoNaoEncontradoException("Cliente " + clienteId + " não encontrado");
        }
        return listar(status, prioridade, clienteId, pageable);
    }

    @Transactional(readOnly = true)
    public ChamadoDetalheResponse buscar(Long id) {
        return ChamadoDetalheResponse.de(buscarEntity(id));
    }

    @Transactional(readOnly = true)
    public List<HistoricoStatusResponse> historico(Long id) {
        return buscarEntity(id).getHistorico().stream().map(HistoricoStatusResponse::de).toList();
    }

    @Transactional
    public ChamadoResponse atualizar(Long id, ChamadoRequest dto) {
        Chamado chamado = buscarEntity(id);
        chamado.garantirNaoFechado();
        chamado.setTitulo(dto.titulo());
        chamado.setDescricao(dto.descricao());
        chamado.setPrioridade(dto.prioridade());
        chamado.setCliente(buscarCliente(dto.clienteId()));
        chamado.setCategoria(buscarCategoria(dto.categoriaId()));
        chamado.setTecnico(buscarTecnico(dto.tecnicoId()));
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public ChamadoResponse atribuirTecnico(Long id, AtribuirTecnicoRequest dto) {
        Chamado chamado = buscarEntity(id);
        chamado.garantirNaoFechado();
        chamado.setTecnico(buscarTecnico(dto.tecnicoId()));
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public ChamadoResponse alterarStatus(Long id, AlterarStatusRequest dto) {
        Chamado chamado = buscarEntity(id);
        StatusChamado anterior = chamado.getStatus();
        chamado.alterarStatus(dto.status());
        historicoRepository.save(new HistoricoStatus(chamado, anterior, dto.status()));
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(buscarEntity(id));
    }

    private void validarOrdenacao(Pageable pageable) {
        for (Sort.Order ordem : pageable.getSort()) {
            if (!CAMPOS_ORDENAVEIS.contains(ordem.getProperty())) {
                throw new ParametroInvalidoException(
                    "Campo de ordenação inválido: '" + ordem.getProperty() + "'. Campos permitidos: "
                    + String.join(", ", new TreeSet<>(CAMPOS_ORDENAVEIS)));
            }
        }
    }

    private Chamado buscarEntity(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Chamado " + id + " não encontrado"));
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Cliente " + id + " não encontrado"));
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Categoria " + id + " não encontrada"));
    }

    private Tecnico buscarTecnico(Long id) {
        return tecnicoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Técnico " + id + " não encontrado"));
    }
}
