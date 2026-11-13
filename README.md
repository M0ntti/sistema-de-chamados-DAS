# Sistema de Chamados

API REST + front-end para abertura e acompanhamento de chamados de suporte.

Trabalho da disciplina **DAS – Design e Arquitetura de Sistemas** (UNIVILLE – Engenharia de Software, Prof. Sestito).

## Funcionalidades

- Cadastro de **clientes**, **categorias** e **técnicos** (CRUD completo).
- Abertura de **chamados** com prioridade, cliente, categoria e técnico responsável.
- Ciclo de vida do chamado com transições de status controladas e **histórico** de cada mudança.
- **Comentários** no chamado.
- Listagem com **filtros** (status, prioridade, cliente), **paginação** e **ordenação**.
- Documentação interativa da API com **Swagger UI**.
- **Front-end** em HTML, CSS e JavaScript puro, servido pela própria aplicação.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Back-end | Java 21, Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA, Bean Validation |
| Banco | H2 em memória (`jdbc:h2:mem:chamadosdb`) |
| Documentação | springdoc-openapi 3 (Swagger UI) |
| Front-end | HTML + CSS + JavaScript (ES Modules), sem framework e sem build |
| Testes | JUnit 5, Mockito, `@DataJpaTest`, `@WebMvcTest` |
| Build | Maven (Maven Wrapper incluso) |

## Como iniciar o projeto

### Pré-requisitos

- **JDK 21** instalado (`java -version` deve mostrar 21).
- Não precisa instalar Maven: o projeto usa o Maven Wrapper (`mvnw`).
- Não precisa instalar Node.js: o front-end não tem etapa de build.

### 1. Clonar o repositório

```powershell
git clone https://github.com/M0ntti/sistema-de-chamados-DAS.git
cd sistema-de-chamados-DAS
```

### 2. Subir a aplicação

Rode **na pasta raiz do projeto** (onde ficam o `pom.xml` e a pasta `frontend/`):

Windows (PowerShell):

```powershell
.\mvnw spring-boot:run
```

macOS / Linux:

```bash
./mvnw spring-boot:run
```

Espere aparecer no terminal a linha `Started ChamadosApplication`. Um único comando sobe a API **e** o front-end.

### 3. Acessar

| O quê | Endereço |
|---|---|
| **Front-end** | http://localhost:8080/ |
| **Swagger UI** (documentação e testes da API) | http://localhost:8080/swagger-ui.html |
| Especificação OpenAPI (JSON) | http://localhost:8080/v3/api-docs |
| Console do H2 | http://localhost:8080/h2-console |

Para o console do H2: JDBC URL `jdbc:h2:mem:chamadosdb`, usuário `sa`, senha em branco.

> O banco é **em memória**: os dados somem quando a aplicação é reiniciada. Isso é esperado nesta fase do projeto.

### 4. Primeiro uso

O banco começa vazio. A tela **Início** mostra um passo a passo:

1. Cadastre um **cliente**.
2. Cadastre uma **categoria**.
3. Cadastre um **técnico**.
4. Abra um **chamado** e, no detalhe dele, altere o status, atribua outro técnico e adicione comentários.

### Parar a aplicação

No terminal onde ela está rodando, pressione `Ctrl + C`.

## Rodar os testes

```powershell
.\mvnw test
```

## Front-end

Fica na pasta [`frontend/`](frontend/). O Spring Boot serve essa pasta na raiz (`http://localhost:8080/`) por causa desta linha no `application.properties`:

```properties
spring.web.resources.static-locations=classpath:/static/,file:frontend/
```

Por isso a aplicação precisa ser iniciada na pasta raiz do projeto.

### Telas

| Rota no navegador | Tela |
|---|---|
| `#/` | Início: contagem de chamados por status e prioridade, cadastros e últimos chamados |
| `#/chamados` | Lista de chamados com filtros, ordenação (clique no cabeçalho da coluna) e paginação |
| `#/chamados/{id}` | Detalhe: descrição, comentários, histórico, alterar status, atribuir técnico, editar e excluir |
| `#/clientes` | CRUD de clientes |
| `#/clientes/{id}` | Chamados do cliente, com filtros e paginação |
| `#/categorias` | CRUD de categorias |
| `#/tecnicos` | CRUD de técnicos |

O detalhe do chamado mostra **só os botões de status permitidos** para o status atual. Chamado `FECHADO` fica somente leitura.

### Organização do código

```
frontend/
├── index.html              layout: menu lateral + área de conteúdo
├── css/style.css           estilos (tema claro/escuro automático, responsivo)
└── js/
    ├── api.js              cliente HTTP: único arquivo que chama a API
    ├── dominio.js          enums (status, prioridade, perfil) e tabela de transições
    ├── ui.js               utilitários: modal, formulário, avisos, paginação, datas
    ├── crud.js             tela genérica de cadastro (usada por clientes, categorias e técnicos)
    ├── app.js              rotas (#/...) e menu
    └── paginas/            uma tela por arquivo
        ├── inicio.js
        ├── chamados.js
        ├── chamado-detalhe.js
        ├── clientes.js
        ├── categorias.js
        └── tecnicos.js
```

Como os erros aparecem na tela: o `api.js` lê o `ErroResposta` da API (`erro` + `detalhes`). Nos formulários, cada item `"campo: mensagem"` aparece embaixo do campo correspondente; os demais erros (ex.: e-mail duplicado, chamado fechado) aparecem no topo do formulário ou num aviso vermelho.

### Rodar o front em outra porta (opcional)

Também dá para abrir o front pelo **Live Server** do VS Code (porta 5500). Nesse caso o `api.js` chama `http://localhost:8080` e o back-end libera essa origem via CORS (`config/CorsConfig.java`). A API precisa estar rodando do mesmo jeito.

Abrir o `index.html` com dois cliques (endereço `file://`) **não funciona**, porque o navegador bloqueia módulos JavaScript fora de um servidor.

## API

URL base: `http://localhost:8080`. A lista completa, com exemplos e botão "Try it out", está no **Swagger UI**.

| Método | Rota | Descrição |
|---|---|---|
| `POST` `GET` | `/clientes` | Cria (201) / lista clientes |
| `GET` `PUT` `DELETE` | `/clientes/{id}` | Busca / atualiza / exclui (204) |
| `GET` | `/clientes/{id}/chamados` | Chamados do cliente (filtros `status`, `prioridade`, paginação e ordenação) |
| `POST` `GET` | `/categorias` | Cria / lista categorias |
| `GET` `PUT` `DELETE` | `/categorias/{id}` | Busca / atualiza / exclui |
| `POST` `GET` | `/tecnicos` | Cria / lista técnicos |
| `GET` `PUT` `DELETE` | `/tecnicos/{id}` | Busca / atualiza / exclui |
| `POST` | `/chamados` | Abre chamado (nasce `ABERTO`) |
| `GET` | `/chamados` | Lista paginada com filtros |
| `GET` | `/chamados/{id}` | Detalhe com comentários |
| `PUT` | `/chamados/{id}` | Atualiza (bloqueado se `FECHADO`) |
| `PATCH` | `/chamados/{id}/atribuir` | `{ "tecnicoId": 2 }` |
| `PATCH` | `/chamados/{id}/status` | `{ "status": "EM_ATENDIMENTO" }` |
| `GET` | `/chamados/{id}/historico` | Mudanças de status |
| `POST` `GET` | `/chamados/{id}/comentarios` | `{ "autor": "...", "texto": "..." }` |
| `DELETE` | `/chamados/{id}` | Exclui com comentários e histórico (204) |

### Listagem de chamados

```
GET /chamados?status=ABERTO&prioridade=ALTA&clienteId=1&page=0&size=10&sort=dataAbertura,desc
```

- Todos os filtros são opcionais e combináveis.
- Padrão: `page=0`, `size=10`, `sort=dataAbertura,desc`.
- `sort` aceita: `id`, `titulo`, `prioridade`, `status`, `dataAbertura`, `dataFechamento` (outro campo devolve 400).

A resposta é um envelope de página:

```json
{ "conteudo": [], "pagina": 0, "tamanho": 10, "totalElementos": 0,
  "totalPaginas": 0, "primeira": true, "ultima": true }
```

### Formato de erro

```json
{ "momento": "2026-11-05T10:00:00", "status": 400, "erro": "Dados inválidos",
  "detalhes": ["email: Email inválido"] }
```

| Status | Quando |
|---|---|
| 400 | Validação, parâmetro ou JSON inválido, campo de ordenação inválido |
| 404 | Recurso não encontrado |
| 409 | Regra de negócio: e-mail/nome duplicado, chamado fechado, transição de status inválida, exclusão de registro ligado a chamados |

## Regras de negócio

- Todo chamado nasce `ABERTO`, com `dataAbertura` preenchida automaticamente.
- Transições de status permitidas:

  | De | Para |
  |---|---|
  | `ABERTO` | `EM_ATENDIMENTO`, `FECHADO` |
  | `EM_ATENDIMENTO` | `AGUARDANDO_CLIENTE`, `FECHADO` |
  | `AGUARDANDO_CLIENTE` | `EM_ATENDIMENTO`, `FECHADO` |
  | `FECHADO` | nenhuma (estado final) |

- Ao fechar, `dataFechamento` é preenchida.
- Chamado `FECHADO` não aceita edição, atribuição, mudança de status nem comentário (409).
- Cada mudança de status gera um registro no histórico.
- Excluir um chamado apaga os comentários e o histórico dele.
- Cliente, categoria ou técnico ligado a algum chamado não pode ser excluído (409).

## Arquitetura do back-end

Arquitetura em camadas, no pacote `br.com.sistemas.chamados`:

```
controller  ->  service  ->  repository  ->  banco (H2)
     |             |
    dto        entity / enums
     |
exception (GlobalExceptionHandler)        config (Swagger e CORS)
```

- **controller**: rotas HTTP e códigos de resposta.
- **dto**: contrato de entrada e saída da API (`record`s).
- **service**: regras de negócio e transações (`@Transactional`).
- **repository**: acesso a dados com Spring Data JPA.
- **entity / enums**: modelo persistido e regras de domínio (ex.: `Chamado.alterarStatus`).
- **exception**: exceções de domínio e conversão em respostas HTTP padronizadas.
- **config**: configuração do Swagger (`OpenApiConfig`) e do CORS (`CorsConfig`).

## Problemas comuns

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| `Port 8080 was already in use` | Outra instância da aplicação já está rodando | Feche o outro terminal ou encerre o processo Java |
| Front abre em branco ou dá 404 em `/` | Aplicação iniciada fora da pasta raiz | Rode `.\mvnw spring-boot:run` na pasta que tem o `pom.xml` |
| Front mostra "Não foi possível conectar à API" | Back-end parado | Suba a aplicação e recarregue a página |
| Dados sumiram | H2 em memória é recriado a cada reinício | Comportamento esperado |
| `JAVA_HOME not found` ao rodar `mvnw` | JDK não configurado | Instale o JDK 21 e configure a variável `JAVA_HOME` |
