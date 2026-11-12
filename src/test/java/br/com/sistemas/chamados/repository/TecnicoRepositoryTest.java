package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.enums.Perfil;

@DataJpaTest
class TecnicoRepositoryTest {

    @Autowired TecnicoRepository repository;

    @Test
    void salvaPerfilComoEnum() {
        Tecnico salvo = repository.save(new Tecnico("Maria", "maria@empresa.com", Perfil.ADMIN));

        assertThat(repository.findById(salvo.getId()).orElseThrow().getPerfil())
            .isEqualTo(Perfil.ADMIN);
    }

    @Test
    void detectaEmailDuplicado() {
        Tecnico maria = repository.save(new Tecnico("Maria", "maria@empresa.com", Perfil.TECNICO));
        Tecnico joao = repository.save(new Tecnico("João", "joao@empresa.com", Perfil.TECNICO));

        assertThat(repository.existsByEmail("maria@empresa.com")).isTrue();
        assertThat(repository.existsByEmail("outro@empresa.com")).isFalse();
        assertThat(repository.existsByEmailAndIdNot("maria@empresa.com", maria.getId())).isFalse();
        assertThat(repository.existsByEmailAndIdNot("maria@empresa.com", joao.getId())).isTrue();
    }
}
