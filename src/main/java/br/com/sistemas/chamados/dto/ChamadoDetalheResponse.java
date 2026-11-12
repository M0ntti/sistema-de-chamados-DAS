package br.com.sistemas.chamados.dto;

import java.time.LocalDateTime;
import java.util.List;

import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;

public record ChamadoDetalheResponse(
    Long id,
    String titulo,
    String descricao,
    LocalDateTime dataAbertura,
    LocalDateTime dataFechamento,
    Prioridade prioridade,
    StatusChamado status,
    Long clienteId,
    String clienteNome,
    Long categoriaId,
    String categoriaNome,
    Long tecnicoId,
    String tecnicoNome,
    List<ComentarioResponse> comentarios
) {
    public static ChamadoDetalheResponse de(Chamado c) {
        return new ChamadoDetalheResponse(
            c.getId(), c.getTitulo(), c.getDescricao(),
            c.getDataAbertura(), c.getDataFechamento(),
            c.getPrioridade(), c.getStatus(),
            c.getCliente().getId(), c.getCliente().getNome(),
            c.getCategoria().getId(), c.getCategoria().getNome(),
            c.getTecnico().getId(), c.getTecnico().getNome(),
            c.getComentarios().stream().map(ComentarioResponse::de).toList());
    }
}
