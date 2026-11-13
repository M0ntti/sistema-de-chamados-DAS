package br.com.sistemas.chamados.controller;

import java.net.URI;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.dto.PaginaResponse;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Cadastro de clientes")
public class ClienteController {

    private final ClienteService service;
    private final ChamadoService chamadoService;

    public ClienteController(ClienteService service, ChamadoService chamadoService) {
        this.service = service;
        this.chamadoService = chamadoService;
    }

    @Operation(summary = "Cadastra um cliente (409 se o e-mail já existir)")
    @PostMapping
    public ResponseEntity<ClienteResponse> criar(
        @Valid @RequestBody ClienteRequest dto) {
            ClienteResponse criado = service.criar(dto);
            return ResponseEntity.created (URI.create("/clientes/" + criado.id())).body(criado);
        }

    @Operation(summary = "Lista todos os clientes")
    @GetMapping
    public List<ClienteResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Busca um cliente pelo id")
    @GetMapping("/{id}")
    public ClienteResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @Operation(summary = "Lista os chamados de um cliente com filtros, paginação e ordenação")
    @GetMapping("/{id}/chamados")
    public PaginaResponse<ChamadoResponse> listarChamados(
            @PathVariable Long id,
            @RequestParam(required = false) StatusChamado status,
            @RequestParam(required = false) Prioridade prioridade,
            @PageableDefault(size = 10, sort = "dataAbertura", direction = Sort.Direction.DESC)
            @ParameterObject Pageable pageable) {
        return chamadoService.listarPorCliente(id, status, prioridade, pageable);
    }

    @Operation(summary = "Atualiza um cliente")
    @PutMapping("/{id}")
    public ClienteResponse atualizar(
        @PathVariable Long id,
        @Valid @RequestBody ClienteRequest dto) {
        return service.atualizar(id, dto);
    }

    @Operation(summary = "Exclui um cliente (409 se tiver chamados)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
