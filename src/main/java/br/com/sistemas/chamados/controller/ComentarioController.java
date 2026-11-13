package br.com.sistemas.chamados.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.service.ComentarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/chamados/{chamadoId}/comentarios")
@Tag(name = "Comentários", description = "Comentários de um chamado")
public class ComentarioController {

    private final ComentarioService service;

    public ComentarioController(ComentarioService service) {
        this.service = service;
    }

    @Operation(summary = "Adiciona um comentário (409 se o chamado estiver FECHADO)")
    @PostMapping
    public ResponseEntity<ComentarioResponse> criar(@PathVariable Long chamadoId,
                                                    @Valid @RequestBody ComentarioRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(chamadoId, dto));
    }

    @Operation(summary = "Lista os comentários do chamado")
    @GetMapping
    public List<ComentarioResponse> listar(@PathVariable Long chamadoId) {
        return service.listar(chamadoId);
    }
}
