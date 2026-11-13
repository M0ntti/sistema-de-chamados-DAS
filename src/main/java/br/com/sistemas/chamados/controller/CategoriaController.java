package br.com.sistemas.chamados.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.sistemas.chamados.dto.CategoriaRequest;
import br.com.sistemas.chamados.dto.CategoriaResponse;
import br.com.sistemas.chamados.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/categorias")
@Tag(name = "Categorias", description = "Cadastro de categorias de chamado")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastra uma categoria (409 se o nome já existir)")
    @PostMapping
    public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CategoriaRequest dto) {
        CategoriaResponse criada = service.criar(dto);
        return ResponseEntity.created(URI.create("/categorias/" + criada.id())).body(criada);
    }

    @Operation(summary = "Lista todas as categorias")
    @GetMapping
    public List<CategoriaResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Busca uma categoria pelo id")
    @GetMapping("/{id}")
    public CategoriaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @Operation(summary = "Atualiza uma categoria")
    @PutMapping("/{id}")
    public CategoriaResponse atualizar(@PathVariable Long id,
                                       @Valid @RequestBody CategoriaRequest dto) {
        return service.atualizar(id, dto);
    }

    @Operation(summary = "Exclui uma categoria (409 se tiver chamados)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}