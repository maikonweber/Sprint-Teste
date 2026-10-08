# People API — Cadastro de Pessoas

API REST para cadastro de pessoas, com autenticação **JWT**, persistência em **PostgreSQL** (migrations com **Flyway**) e previsão de nacionalidade pela API pública [Nationalize.io](https://nationalize.io). Inclui uma interface web em **React + Vite** que consome a API.

## Projeto

- **Backend** Spring Boot organizado em camadas: `controller` → `service` → `repository`, com DTOs (records) separando a API das entidades JPA.
- **Segurança** stateless: `POST /auth/login` emite um JWT (HS256) e um filtro do Spring Security o valida em todas as outras requisições. As senhas ficam armazenadas apenas como hash **BCrypt**.
- **Integração externa** isolada em `NationalizeClient` (WebClient com timeouts). As falhas de comunicação são traduzidas para `503 Service Unavailable`.
- **Erros padronizados** num único `@RestControllerAdvice`, sem stack trace para o cliente.
- **Frontend** React com login, cadastro, listagem, exclusão e consulta de nacionalidade.

## Tecnologias

| Camada | Tecnologias |
|---|---|
| Linguagem / build | Java 21, Maven (wrapper `./mvnw`) |
| Framework | Spring Boot 3.5 (Web, Data JPA, Validation, Security, WebFlux/WebClient) |
| Autenticação | Spring Security + JWT (jjwt 0.12) + BCrypt |
| Banco | PostgreSQL 16, Flyway, Hibernate (`ddl-auto=validate`) |
| Documentação | springdoc-openapi (Swagger UI) |
| Testes | JUnit 5, Mockito, Spring MockMvc, WireMock, H2 (modo PostgreSQL) |
| Frontend | React 19 + Vite 8 |
| Infra | Docker, Docker Compose, nginx (frontend) |

## Estrutura

```text
.
├── src/main/java/com/example/people
│   ├── PeopleApplication.java
│   ├── client/          NationalizeClient, NationalizeResponse, NationalizeProperties
│   ├── config/          SecurityConfig, OpenApiConfig, WebClientConfig, PropertiesConfig,
│   │                    AdminUserInitializer (seed), AdminProperties, CorsProperties
│   ├── controller/      AuthController, PersonController, NationalityController
│   ├── dto/             auth/, person/, nationality/, error/
│   ├── entity/          Person, User, Role
│   ├── exception/       GlobalExceptionHandler, ResourceNotFoundException, BusinessException,
│   │                    DuplicateResourceException, ExternalServiceException
│   ├── repository/      PersonRepository, UserRepository
│   ├── security/        JwtAuthenticationFilter, JwtService, JwtProperties,
│   │                    CustomUserDetailsService, SecurityErrorHandler
│   └── service/         AuthService, PersonService, NationalityService, CountryNameResolver
├── src/main/resources
│   ├── application.yml
│   └── db/migration/    V1__create_people_table.sql, V2__create_users_table.sql
├── src/test/...         testes unitários, de controller (WebMvcTest) e de integração
├── frontend/            React + Vite (Dockerfile + nginx.conf)
├── Dockerfile           imagem do backend (multi-stage, usuário não-root)
├── docker-compose.yml   PostgreSQL + backend + frontend
└── .env.example         variáveis de desenvolvimento
```

## Como executar

Pré-requisitos: **Java 21+**, **Node 20.19+** (ou 22.12+) e **Docker**.

```bash
cp .env.example .env          # valores de desenvolvimento (o .env está no .gitignore)
```

### Opção 1 — tudo via Docker

```bash
docker compose up --build
```

| Serviço | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | localhost:5432 (`postgres` / `postgres`, banco `people`) |

Os dados ficam no volume `postgres-data` e persistem entre reinícios (`docker compose down` mantém o volume; `docker compose down -v` apaga).

### Opção 2 — banco no Docker, aplicação local

```bash
docker compose up -d postgres

./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run

cd frontend
npm install
npm run dev                   # http://localhost:5173
```

O backend lê automaticamente o arquivo `.env` da raiz (`spring.config.import=optional:file:.env[.properties]`), e variáveis de ambiente reais têm precedência sobre ele. O Vite também lê o mesmo `.env` (`envDir: '..'`), mas só expõe ao bundle as variáveis com prefixo `VITE_`.

## Configuração

| Variável | Descrição | Padrão (dev) |
|---|---|---|
| `DB_HOST` | Host do PostgreSQL | `localhost` |
| `DB_PORT` | Porta do PostgreSQL | `5432` |
| `DB_NAME` | Nome do banco | `people` |
| `DB_USERNAME` | Usuário do banco | `postgres` |
| `DB_PASSWORD` | Senha do banco | `postgres` |
| `JWT_SECRET` | Chave HMAC do JWT (**obrigatória**, ≥ 32 caracteres) | ver `.env.example` |
| `JWT_EXPIRATION` | Validade do token (ISO-8601) | `PT1H` |
| `ADMIN_USERNAME` | Usuário admin criado no primeiro start (**obrigatória**) | `admin` |
| `ADMIN_PASSWORD` | Senha do admin, gravada como BCrypt (**obrigatória**, ≥ 8 caracteres) | `admin123` |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas, separadas por vírgula | `http://localhost:5173` |
| `NATIONALIZE_BASE_URL` | URL da API Nationalize | `https://api.nationalize.io` |
| `NATIONALIZE_TIMEOUT` | Timeout da chamada externa | `PT5S` |
| `SWAGGER_ENABLED` | Habilita Swagger UI / OpenAPI | `true` |
| `VITE_API_URL` | URL do backend usada pelo frontend | `http://localhost:8080` |

As propriedades são validadas na inicialização (`@Validated @ConfigurationProperties`). Se faltar `JWT_SECRET` ou `ADMIN_PASSWORD`, a aplicação **não sobe** e informa o motivo, em vez de cair silenciosamente num valor inseguro.

## Credenciais de desenvolvimento

```text
username: admin
password: admin123
```

O usuário é criado por `AdminUserInitializer` apenas se ainda não existir; a senha é persistida como hash BCrypt. Mudar `ADMIN_PASSWORD` depois do primeiro start não altera um usuário já existente.

## Endpoints

Exceto `POST /auth/login` (e a documentação Swagger), **todos exigem** `Authorization: Bearer <token>`.

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| `POST` | `/auth/login` | Autentica e retorna o JWT | 200, 400, 401 |
| `POST` | `/registrarName` | Cadastra uma pessoa | 201 (+ header `Location`), 400, 401, 409 |
| `GET` | `/list` | Lista todas as pessoas (ordenadas por id) | 200, 401 |
| `GET` | `/list/{id}` | Consulta uma pessoa | 200, 400, 401, 404 |
| `DELETE` | `/list/{id}` | Exclui uma pessoa | 204, 400, 401, 404 |
| `GET` | `/findNacionalityByPerson/{id}` | Nacionalidade mais provável pelo nome | 200, 400, 401, 404, 503 |

### Regras de validação

| Campo | Regra |
|---|---|
| `document` | obrigatório, único, apenas dígitos: 11 (CPF) ou 14 (CNPJ) |
| `name`, `surname` | obrigatórios, 2 a 100 caracteres |
| `email` | obrigatório, formato válido, até 254 caracteres, único (comparação case-insensitive; persistido em minúsculas) |
| `{id}` | inteiro positivo; `0`, negativos, texto ou valores fora do range de `Long` retornam `400` |

O documento é validado por formato e tamanho, não por dígito verificador, porque o exemplo do enunciado (`12345678900`) não é um CPF válido.

### Exemplos

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r .token)

# Cadastro
curl -i -X POST http://localhost:8080/registrarName \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"document":"12345678900","name":"Nathaniel","surname":"Silva","email":"nathaniel@example.com"}'

# Listagem / consulta / exclusão
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/list
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/list/1
curl -X DELETE -H "Authorization: Bearer $TOKEN" http://localhost:8080/list/1

# Nacionalidade
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/findNacionalityByPerson/1
```

Resposta de nacionalidade:

```json
{
  "personId": 1,
  "name": "Nathaniel",
  "nationality": "United States",
  "countryCode": "US",
  "probability": 0.357622
}
```

A nacionalidade vem do código ISO de **maior probabilidade** retornado pela Nationalize, convertido para o nome do país com os dados de localização da própria JDK (`Locale`). Quando a API não tem previsão para o nome, ou retorna um código fora da ISO 3166-1, a resposta é `200` com `"nationality": "Unknown"` (e `countryCode`/`probability` nulos quando não há previsão). Indisponibilidade, timeout, limite de requisições (429) ou resposta malformada da Nationalize resultam em `503`.

### Formato de erro

```json
{
  "timestamp": "2026-10-07T17:13:56.941Z",
  "status": 400,
  "error": "Bad Request",
  "message": "E-mail inválido; Nome deve ter entre 2 e 100 caracteres",
  "path": "/registrarName",
  "errors": [
    { "field": "email", "message": "E-mail inválido" },
    { "field": "name", "message": "Nome deve ter entre 2 e 100 caracteres" }
  ]
}
```

O campo `errors` aparece apenas em erros de validação. Erros inesperados retornam `500` com a mensagem genérica `"Erro interno inesperado"`; o detalhe completo fica só no log do servidor.

## Autenticação

1. `POST /auth/login` com `{"username": "...", "password": "..."}`.
2. A resposta traz `{"token": "<JWT>", "tokenType": "Bearer", "expiresIn": 3600}`.
3. Envie o token nas demais chamadas: `Authorization: Bearer <JWT>`.

Detalhes:

- O token é assinado com HS256 e carrega `sub` (username), `iss`, `iat` e `exp`.
- O `JwtAuthenticationFilter` valida assinatura, emissor e expiração, e confirma que o usuário ainda existe e está habilitado.
- Não há sessão HTTP (`SessionCreationPolicy.STATELESS`) nem cookies. Por isso o CSRF está desabilitado.
- Requisições sem token, com token inválido ou com token expirado recebem `401` no mesmo formato de erro padronizado.
- Usuário inexistente e senha errada retornam a mesma mensagem, para não permitir enumeração de usuários.

No frontend, o token fica em `sessionStorage`: sobrevive a um refresh e é descartado ao fechar a aba. Se o backend responder `401`, a sessão é encerrada automaticamente.

## Swagger

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Para testar endpoints protegidos: execute `POST /auth/login`, copie o `token`, clique em **Authorize** e cole o token (sem o prefixo `Bearer`).

## Testes

```bash
./mvnw test                   # Windows: mvnw.cmd test
```

São 76 testes, que **não dependem de internet nem de Docker**:

| Suíte | Tipo | O que cobre |
|---|---|---|
| `PersonServiceTest` | Unitário (Mockito) | criação com normalização, documento/e-mail duplicados, busca, inexistente, listagem, exclusão |
| `NationalityServiceTest` | Unitário (Mockito) | maior probabilidade, código desconhecido, sem previsão, pessoa inexistente (não chama a API), falha externa |
| `CountryNameResolverTest` | Unitário | BR/US/GB/CA, minúsculas, nulos, vazios, códigos inválidos |
| `JwtServiceTest` | Unitário | token válido, adulterado, expirado, outra chave, outro emissor, lixo |
| `NationalizeClientTest` | Cliente HTTP real + WireMock | parsing, encoding de nomes acentuados, 500, 429, timeout, JSON malformado, conexão recusada |
| `PersonControllerTest` | `@WebMvcTest` | cadastro válido/inválido/malformado/duplicado, busca válida, IDs inválidos (`0`, `-1`, `abc`, `1.5`, overflow), 404, exclusão, 500 sem vazamento de detalhes |
| `AuthIntegrationTest` | Integração (`@SpringBootTest`) | login válido, senha inválida, usuário inexistente, payload inválido, endpoints protegidos sem JWT, com JWT válido e com JWT inválido, ausência de sessão, senha seed em BCrypt |
| `PersonApiIntegrationTest` | Integração | fluxo completo de cadastro, listagem, busca e exclusão, conflitos 409, validação de payload e de id, CORS (origem permitida vs. não permitida) |
| `NationalityIntegrationTest` | Integração + WireMock | fluxo completo, 404 sem chamada externa, id inválido, Nationalize fora do ar → 503 |

Os testes de integração sobem a aplicação inteira (segurança, Flyway, JPA) sobre **H2 em modo de compatibilidade PostgreSQL**, executando as **mesmas migrations** de produção. A Nationalize é substituída por um servidor **WireMock** local.

## Decisões técnicas

- **Spring Boot 3.5** (linha 3.x mais recente), conforme o requisito "Spring Boot 3+".
- **Rotas exatamente como especificadas** (`/registrarName`, `/list`, `/findNacionalityByPerson/{id}`), inclusive a grafia "Nacionality", para manter compatibilidade com o enunciado.
- **Records como DTOs**, imutáveis e concisos. As entidades não saem da camada de serviço.
- **Unicidade em duas camadas**: checagem no service, que dá mensagem clara de conflito, e `UNIQUE` no banco, que garante consistência em concorrência. Uma violação concorrente vira `409` via `DataIntegrityViolationException`.
- **Validação do `id`** com `@Positive` em `Long` (method validation nativa do Spring 6.1+). A conversão de tipo fica a cargo do framework, e falhas viram `400` no handler global, sem `Long.parseLong` manual.
- **WebClient** com timeout de conexão e de resposta, bloqueando na borda do service, já que a aplicação é Spring MVC.
- **Conversão ISO → nome via `java.util.Locale`**, sem dependência extra, cobrindo todos os códigos ISO 3166-1, com fallback seguro `Unknown`.
- **`ddl-auto=validate`**: o schema é responsabilidade exclusiva do Flyway, e o Hibernate apenas confere se o mapeamento bate.
- **Segredos fora do código**: `JWT_SECRET` e `ADMIN_PASSWORD` não têm valor padrão no `application.yml`, e a aplicação falha na inicialização se não estiverem configurados.
- **CORS explícito** para `http://localhost:5173` (configurável), com métodos e headers restritos ao que o frontend usa.
- **H2 nos testes** em vez de Testcontainers, para que `./mvnw test` rode em qualquer máquina sem Docker. A aplicação real foi validada contra PostgreSQL 16.

## Melhorias para produção

- **Testcontainers** com PostgreSQL real nos testes de integração, para fidelidade total de dialeto.
- **Refresh tokens** e revogação de tokens (blacklist/rotação), além de chaves assimétricas (RS256/JWKS) se houver múltiplos serviços.
- **Rate limiting / proteção contra brute force** no `/auth/login` (ex.: Bucket4j) e bloqueio temporário de conta.
- Guardar o token em **cookie `HttpOnly` + `SameSite`** em vez de `sessionStorage`, reabilitando CSRF.
- **Paginação e filtros** em `GET /list` (`Pageable`), em vez de retornar todos os registros.
- **Cache** das previsões da Nationalize (Caffeine/Redis) e **circuit breaker/retry** com Resilience4j. A API gratuita tem limite diário.
- Validação de **dígito verificador** de CPF/CNPJ, se o domínio exigir documentos reais.
- **Observabilidade**: Spring Boot Actuator (health/readiness para orquestradores), métricas Micrometer/Prometheus, logs estruturados em JSON com correlation id.
- **Autorização por papel** (`ROLE_ADMIN` vs. `ROLE_USER`) nos endpoints de escrita, e cadastro/gestão de usuários.
- Gerenciamento de segredos via **Vault / AWS Secrets Manager / Kubernetes Secrets**, desabilitar o Swagger em produção (`SWAGGER_ENABLED=false`) e servir tudo atrás de HTTPS.
- **Pipeline CI** (build, testes, análise estática com SonarQube/SpotBugs, scan de dependências e de imagem) e versionamento das imagens Docker.
- Testes de frontend (Vitest + Testing Library) e E2E (Playwright).
