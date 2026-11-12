package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.sistemas.chamados.entity.Cliente;

@DataJpaTest
class ClienteRepositoryTest {

    @Autowired ClienteRepository repository;

    @Test
    void salvaEBuscaCliente() {
        Cliente salvo = repository.save(new Cliente("Ana Souza", "ana@exemplo.com", "47999999999"));

        Cliente lido = repository.findById(salvo.getId()).orElseThrow();

        assertThat(lido.getId()).isNotNull();
        assertThat(lido.getNome()).isEqualTo("Ana Souza");
        assertThat(lido.getEmail()).isEqualTo("ana@exemplo.com");
        assertThat(lido.getTelefone()).isEqualTo("47999999999");
    }

    @Test
    void existsByEmailDetectaDuplicidade() {
        repository.save(new Cliente("Ana", "ana@exemplo.com", null));

        assertThat(repository.existsByEmail("ana@exemplo.com")).isTrue();
        assertThat(repository.existsByEmail("outro@exemplo.com")).isFalse();
    }

    @Test
    void existsByEmailAndIdNotIgnoraOProprioCliente() {
        Cliente ana = repository.save(new Cliente("Ana", "ana@exemplo.com", null));
        Cliente bruno = repository.save(new Cliente("Bruno", "bruno@exemplo.com", null));

        assertThat(repository.existsByEmailAndIdNot("ana@exemplo.com", ana.getId())).isFalse();
        assertThat(repository.existsByEmailAndIdNot("ana@exemplo.com", bruno.getId())).isTrue();
    }

    @Test
    void bancoRecusaEmailDuplicado() {
        repository.saveAndFlush(new Cliente("Ana", "ana@exemplo.com", null));

        assertThatThrownBy(() ->
                repository.saveAndFlush(new Cliente("Outra Ana", "ana@exemplo.com", null)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void buscaPorIdInexistenteRetornaVazio() {
        assertThat(repository.findById(999L)).isEmpty();
    }
}
