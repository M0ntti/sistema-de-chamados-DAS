package br.com.sistemas.chamados.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import br.com.sistemas.chamados.enums.Perfil;
import br.com.sistemas.chamados.enums.Prioridade;
import br.com.sistemas.chamados.enums.StatusChamado;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.TransicaoStatusInvalidaException;

class ChamadoTest {

    private static Chamado novoChamado() {
        return new Chamado("Computador não liga", "Não dá vídeo", Prioridade.ALTA,
            new Cliente("Ana Souza", "ana@exemplo.com", "47999999999"),
            new Categoria("Hardware", null),
            new Tecnico("Maria Silva", "maria@empresa.com", Perfil.TECNICO));
    }

    private static Chamado chamadoEm(StatusChamado alvo) {
        Chamado chamado = novoChamado();
        switch (alvo) {
            case ABERTO -> { }
            case EM_ATENDIMENTO -> chamado.alterarStatus(StatusChamado.EM_ATENDIMENTO);
            case AGUARDANDO_CLIENTE -> {
                chamado.alterarStatus(StatusChamado.EM_ATENDIMENTO);
                chamado.alterarStatus(StatusChamado.AGUARDANDO_CLIENTE);
            }
            case FECHADO -> chamado.alterarStatus(StatusChamado.FECHADO);
        }
        return chamado;
    }

    @Test
    void nasceAbertoComDataAberturaESemDataFechamento() {
        Chamado chamado = novoChamado();

        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.ABERTO);
        assertThat(chamado.getDataAbertura()).isNotNull();
        assertThat(chamado.getDataFechamento()).isNull();
        assertThat(chamado.estaFechado()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "ABERTO,EM_ATENDIMENTO",
        "ABERTO,FECHADO",
        "EM_ATENDIMENTO,AGUARDANDO_CLIENTE",
        "EM_ATENDIMENTO,FECHADO",
        "AGUARDANDO_CLIENTE,EM_ATENDIMENTO",
        "AGUARDANDO_CLIENTE,FECHADO"
    })
    void aceitaTransicoesValidas(StatusChamado origem, StatusChamado destino) {
        Chamado chamado = chamadoEm(origem);

        chamado.alterarStatus(destino);

        assertThat(chamado.getStatus()).isEqualTo(destino);
    }

    @ParameterizedTest
    @CsvSource({
        "ABERTO,ABERTO",
        "ABERTO,AGUARDANDO_CLIENTE",
        "EM_ATENDIMENTO,ABERTO",
        "EM_ATENDIMENTO,EM_ATENDIMENTO",
        "AGUARDANDO_CLIENTE,ABERTO",
        "AGUARDANDO_CLIENTE,AGUARDANDO_CLIENTE"
    })
    void rejeitaTransicoesInvalidas(StatusChamado origem, StatusChamado destino) {
        Chamado chamado = chamadoEm(origem);

        assertThatThrownBy(() -> chamado.alterarStatus(destino))
            .isInstanceOf(TransicaoStatusInvalidaException.class)
            .hasMessage("Transição de status inválida: " + origem + " -> " + destino);
        assertThat(chamado.getStatus()).isEqualTo(origem);
    }

    @Test
    void aoFecharPreencheDataFechamento() {
        Chamado chamado = novoChamado();
        assertThat(chamado.getDataFechamento()).isNull();

        chamado.alterarStatus(StatusChamado.FECHADO);

        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.FECHADO);
        assertThat(chamado.getDataFechamento()).isNotNull();
        assertThat(chamado.estaFechado()).isTrue();
    }

    @Test
    void statusIntermediariosNaoPreenchemDataFechamento() {
        Chamado chamado = chamadoEm(StatusChamado.AGUARDANDO_CLIENTE);

        assertThat(chamado.getDataFechamento()).isNull();
    }

    @ParameterizedTest
    @EnumSource(StatusChamado.class)
    void chamadoFechadoNaoMudaMaisDeStatus(StatusChamado destino) {
        Chamado chamado = chamadoEm(StatusChamado.FECHADO);

        assertThatThrownBy(() -> chamado.alterarStatus(destino))
            .isInstanceOf(ChamadoFechadoException.class);
        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.FECHADO);
    }

    @Test
    void garantirNaoFechadoLancaApenasQuandoFechado() {
        Chamado aberto = novoChamado();
        Chamado fechado = chamadoEm(StatusChamado.FECHADO);

        aberto.garantirNaoFechado(); // não lança
        assertThatThrownBy(fechado::garantirNaoFechado)
            .isInstanceOf(ChamadoFechadoException.class);
    }
}
