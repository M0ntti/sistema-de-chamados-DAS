package br.com.sistemas.chamados.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.TecnicoRequest;
import br.com.sistemas.chamados.dto.TecnicoResponse;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.repository.TecnicoRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;

@Service
public class TecnicoService {

    private final TecnicoRepository repository;
    private final ChamadoRepository chamadoRepository;

    public TecnicoService(TecnicoRepository repository) {
        this.repository = repository;
        this.chamadoRepository = chamadoRepository;
    }

    @Transactional
    public TecnicoResponse criar(TecnicoRequest dto) {
        if (repository.existsByEmail(dto.email())) {
            throw new RegraNegocioException("Já existe técnico com o e-mail " + dto.email());
        }
        Tecnico salvo = repository.save(new Tecnico(dto.nome(), dto.email(), dto.perfil()));
        return TecnicoResponse.de(salvo);
    }

    @Transactional(readOnly = true)
    public List<TecnicoResponse> listar() {
        return repository.findAll().stream().map(TecnicoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public TecnicoResponse buscar(Long id) {
        return TecnicoResponse.de(buscarEntity(id));
    }

    @Transactional
    public TecnicoResponse atualizar(Long id, TecnicoRequest dto) {
        Tecnico tecnico = buscarEntity(id);
        if (repository.existsByEmailAndIdNot(dto.email(), id)) {
            throw new RegraNegocioException("Já existe técnico com o e-mail " + dto.email());
        }
        tecnico.setNome(dto.nome());
        tecnico.setEmail(dto.email());
        tecnico.setPerfil(dto.perfil());
        return TecnicoResponse.de(tecnico);
    }

    @Transactional
    public void excluir(Long id) {
        Tecnico tecnico = buscarEntity(id);
        long vinculados = chamadoRepository.countByTecnicoId(id);
        if (vinculados > 0) {
            throw new RegraNegocioException(
                "Não é possível excluir o técnico " + id + ": existem " + vinculados
                + " chamado(s) vinculado(s) a ele");
        }
        repository.delete(tecnico);
    }

    private Tecnico buscarEntity(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Técnico " + id + " não encontrado"));
    }
}