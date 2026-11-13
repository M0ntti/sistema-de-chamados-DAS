package br.com.sistemas.chamados.controller;

import java.net.URI;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.AtribuirTecnicoRequest;
import br.com.sistemas.chamados.dto.ChamadoDetalheResponse;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.HistoricoStatusResponse;
import br.com.sistemas.chamados.dto.PaginaResponse;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import br.com.sistemas.chamados.service.ChamadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/chamados")
@Tag(name = "Chamados", description = "Abertura, consulta e ações sobre chamados")
public class ChamadoController {

    private final ChamadoService service;

    public ChamadoController(ChamadoService service) {
        this.service = service;
    }

    @Operation(summary = "Abre um chamado (nasce ABERTO)")
    @PostMapping
    public ResponseEntity<ChamadoResponse> criar(@Valid @RequestBody ChamadoRequest dto) {
        ChamadoResponse criado = service.criar(dto);
        return ResponseEntity.created(URI.create("/chamados/" + criado.id())).body(criado);
    }

    @Operation(summary = "Lista chamados com filtros, paginação e ordenação",
               description = "sort aceita: id, titulo, prioridade, status, dataAbertura, dataFechamento")
    @GetMapping
    public PaginaResponse<ChamadoResponse> listar(
            @RequestParam(required = false) StatusChamado status,
            @RequestParam(required = false) Prioridade prioridade,
            @RequestParam(required = false) Long clienteId,
            @PageableDefault(size = 10, sort = "dataAbertura", direction = Sort.Direction.DESC)
            @ParameterObject Pageable pageable) {
        return service.listar(status, prioridade, clienteId, pageable);
    }

    @Operation(summary = "Detalhe do chamado com a lista de comentários")
    @GetMapping("/{id}")
    public ChamadoDetalheResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @Operation(summary = "Histórico de mudanças de status do chamado")
    @GetMapping("/{id}/historico")
    public List<HistoricoStatusResponse> historico(@PathVariable Long id) {
        return service.historico(id);
    }

    @Operation(summary = "Atualiza um chamado (409 se estiver FECHADO)")
    @PutMapping("/{id}")
    public ChamadoResponse atualizar(@PathVariable Long id,
                                     @Valid @RequestBody ChamadoRequest dto) {
        return service.atualizar(id, dto);
    }

    @Operation(summary = "Atribui um técnico ao chamado (409 se estiver FECHADO)")
    @PatchMapping("/{id}/atribuir")
    public ChamadoResponse atribuir(@PathVariable Long id,
                                    @Valid @RequestBody AtribuirTecnicoRequest dto) {
        return service.atribuirTecnico(id, dto);
    }

    @Operation(summary = "Altera o status seguindo as transições permitidas (409 se inválida)")
    @PatchMapping("/{id}/status")
    public ChamadoResponse alterarStatus(@PathVariable Long id,
                                         @Valid @RequestBody AlterarStatusRequest dto) {
        return service.alterarStatus(id, dto);
    }

    @Operation(summary = "Exclui o chamado junto com comentários e histórico")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
