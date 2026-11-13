package br.com.sistemas.chamados.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.sistemas.chamados.dto.TecnicoRequest;
import br.com.sistemas.chamados.dto.TecnicoResponse;
import br.com.sistemas.chamados.service.TecnicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/tecnicos")
@Tag(name = "Técnicos", description = "Cadastro de técnicos")
public class TecnicoController {

    private final TecnicoService service;

    public TecnicoController(TecnicoService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastra um técnico (409 se o e-mail já existir)")
    @PostMapping
    public ResponseEntity<TecnicoResponse> criar(@Valid @RequestBody TecnicoRequest dto) {
        TecnicoResponse criado = service.criar(dto);
        return ResponseEntity.created(URI.create("/tecnicos/" + criado.id())).body(criado);
    }

    @Operation(summary = "Lista todos os técnicos")
    @GetMapping
    public List<TecnicoResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Busca um técnico pelo id")
    @GetMapping("/{id}")
    public TecnicoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @Operation(summary = "Atualiza um técnico")
    @PutMapping("/{id}")
    public TecnicoResponse atualizar(@PathVariable Long id,
                                     @Valid @RequestBody TecnicoRequest dto) {
        return service.atualizar(id, dto);
    }

    @Operation(summary = "Exclui um técnico (409 se tiver chamados)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}