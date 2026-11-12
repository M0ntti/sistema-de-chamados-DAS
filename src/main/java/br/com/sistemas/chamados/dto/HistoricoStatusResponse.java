package br.com.sistemas.chamados.dto;

import java.time.LocalDateTime;

import br.com.sistemas.chamados.entity.HistoricoStatus;
import br.com.sistemas.chamados.enums.StatusChamado;

public record HistoricoStatusResponse(
    Long id,
    StatusChamado statusAnterior,
    StatusChamado statusNovo,
    LocalDateTime dataAlteracao
) {
    public static HistoricoStatusResponse de(HistoricoStatus h) {
        return new HistoricoStatusResponse(
            h.getId(), h.getStatusAnterior(), h.getStatusNovo(), h.getDataAlteracao());
    }
}
