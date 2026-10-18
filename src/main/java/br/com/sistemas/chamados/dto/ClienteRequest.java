package br.com.sistemas.chamados.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
    @NotBlank(message = "Nome é obrigatorio")
    @Size(max = 100, message = "Nome deve ter no maáximo 100 caracteres")
    String nome,

    @NotBlank(message = "Email é obrigatorio")
    @Email(message = "Email invalido")
    String email,

    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    String telefone

){}