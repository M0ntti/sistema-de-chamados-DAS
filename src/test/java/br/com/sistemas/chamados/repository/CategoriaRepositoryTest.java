package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import br.com.sistemas.chamados.entity.Categoria;

@DataJpaTest
class CategoriaRepositoryTest {

    @Autowired CategoriaRepository repository;

    @Test
    void duplicidadeIgnoraMaiusculasEMinusculas() {
        repository.save(new Categoria("Hardware", "Equipamentos"));

        assertThat(repository.existsByNomeIgnoreCase("hardware")).isTrue();
        assertThat(repository.existsByNomeIgnoreCase("HARDWARE")).isTrue();
        assertThat(repository.existsByNomeIgnoreCase("Software")).isFalse();
    }

    @Test
    void duplicidadeNaAtualizacaoIgnoraAPropriaCategoria() {
        Categoria hardware = repository.save(new Categoria("Hardware", null));
        Categoria software = repository.save(new Categoria("Software", null));

        assertThat(repository.existsByNomeIgnoreCaseAndIdNot("hardware", hardware.getId())).isFalse();
        assertThat(repository.existsByNomeIgnoreCaseAndIdNot("hardware", software.getId())).isTrue();
    }
}
