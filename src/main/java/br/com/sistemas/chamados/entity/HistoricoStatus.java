package br.com.sistemas.chamados.entity;

import java.time.LocalDateTime;

import br.com.sistemas.chamados.enums.StatusChamado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "historico_status")
public class HistoricoStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chamado_id", nullable = false)
    private Chamado chamado;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", nullable = false, length = 30)
    private StatusChamado statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 30)
    private StatusChamado statusNovo;

    @Column(name = "data_alteracao", nullable = false, updatable = false)
    private LocalDateTime dataAlteracao;

    public HistoricoStatus() {}

    public HistoricoStatus(Chamado chamado, StatusChamado statusAnterior, StatusChamado statusNovo) {
        this.chamado = chamado;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.dataAlteracao = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Chamado getChamado() { return chamado; }
    public StatusChamado getStatusAnterior() { return statusAnterior; }
    public StatusChamado getStatusNovo() { return statusNovo; }
    public LocalDateTime getDataAlteracao() { return dataAlteracao; }
}
