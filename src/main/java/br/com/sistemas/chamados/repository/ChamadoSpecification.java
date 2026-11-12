package br.com.sistemas.chamados.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import jakarta.persistence.criteria.Predicate;

public final class ChamadoSpecification {

    private ChamadoSpecification() {}

    public static Specification<Chamado> comFiltros(
            StatusChamado status, Prioridade prioridade, Long clienteId) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (status != null) {
                predicados.add(cb.equal(root.get("status"), status));
            }
            if (prioridade != null) {
                predicados.add(cb.equal(root.get("prioridade"), prioridade));
            }
            if (clienteId != null) {
                predicados.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (predicados.isEmpty()) {
                return cb.conjunction(); // sem filtros: traz tudo
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}
