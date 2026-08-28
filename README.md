# Cobranças API

Microserviço backend para gestão de cobranças, com suporte a **PIX** e **cartão de crédito**, regras de concorrência por usuário, processamento de webhooks PIX, validação de checkout e reprocessamento de cobranças.

O projeto foi desenvolvido como parte de um desafio técnico de backend, com foco em **Java 17, Spring Boot, arquitetura em camadas, regras de negócio, concorrência, integração com serviços externos e testes automatizados**.

---

## 📋 Visão geral

A API implementa um fluxo mínimo de cobrança com suporte às seguintes operações:

* Criação de cobrança
* Consulta de cobrança por ID
* Processamento de webhook PIX
* Validação de checkout de cartão de crédito
* Lock distribuído por usuário
* Versionamento e reprocessamento de cobranças
* Integrações externas simuladas por mocks

A API utiliza a base:

```text
/api/v1/cobrancas
```

### Principais características

* Java 17
* Spring Boot 
* Spring Data JPA
* Hibernate
* H2 Database
* Maven
* JUnit 5
* Mockito
* Arquitetura em camadas
* Strategy Pattern para criação por método de pagamento
* Controle de concorrência por usuário
* Tratamento centralizado de exceções
* Integrações externas simuladas

---

## 🛠️ Stack

| Tecnologia       | Utilização              |
| ---------------- | ----------------------- |
| Java 17          | Linguagem principal     |
| Spring Boot      | Framework principal     |
| Spring Web       | Construção da API REST  |
| Spring Validation | Validação dos requests  |
| Spring Data JPA  | Persistência            |
| Hibernate        | ORM                     |
| H2               | Banco de dados          |
| Maven            | Gerenciamento e build   |
| JUnit 5          | Testes unitários        |
| Mockito          | Mocks e testes isolados |

---

## 📁 Estrutura do projeto

A aplicação está organizada por responsabilidades:

```text
src/
└── main/
    └── java/
        └── ...
            ├── controller
            │   └── Endpoints REST
            │
            ├── service
            │   └── Regras de negócio
            │
            ├── service/strategy
            │   └── Estratégias de criação por método
            │
            ├── repository
            │   └── Acesso ao banco de dados
            │
            ├── domain
            │   ├── Entidades
            │   └── Enums
            │
            ├── dto
            │   ├── Requests
            │   └── Responses
            │
            ├── integration
            │   └── Clientes de integrações externas
            │
            ├── lock
            │   └── Controle de concorrência
            │
            ├── exception
            │   └── Tratamento de exceções
            │
            └── mapper
                └── Conversão entre entidade e DTO
```

---

# 💳 Domínio

A entidade principal do sistema é `Cobranca`.

### Campos

| Campo              | Descrição                            |
| ------------------ | ------------------------------------ |
| `id`               | Identificador da cobrança            |
| `idUsuario`        | Identificador do usuário responsável |
| `nomeSolicitante`  | Nome do solicitante                  |
| `tipo`             | Tipo da cobrança                     |
| `metodo`           | Método de pagamento                  |
| `status`           | Status atual da cobrança             |
| `valorSolicitacao` | Valor solicitado                     |
| `valorPago`        | Valor efetivamente pago              |
| `txid`             | Identificador da transação PIX       |
| `copiaECola`       | Código PIX copia e cola              |
| `transactionId`    | Identificador da transação de cartão |
| `acsUrl`           | URL utilizada no fluxo 3DS           |
| `threeDsPayload`   | Payload relacionado ao 3DS           |
| `dataCriacao`      | Data de criação                      |
| `dataExpiracao`    | Data de expiração                    |
| `dataFinalizada`   | Data de finalização                  |

---

## 🔖 Enums

### `CobrancaTipoEnum`

Representa o tipo da cobrança:

```text
RECARGA
RECARGA_TERCEIROS
ENVIO_CARTAO
```

### `CobrancaMetodoEnum`

Representa o método de pagamento:

```text
PIX
CARTAO_CREDITO
```

### `CobrancaStatusEnum`

Representa o estado atual da cobrança:

| Código | Status                  |
| -----: | ----------------------- |
|    `2` | `SOLICITADA`            |
|    `3` | `EXPIRADA`              |
|    `4` | `ERRO_APROVACAO_PEDIDO` |
|    `5` | `FINALIZADA`            |
|    `6` | `EM_REPROCESSAMENTO`    |
|    `9` | `ERRO_ANALISE_PENDENTE` |

---

# ⚙️ Regras de negócio

## 1. Lock por usuário

A criação de uma cobrança utiliza um lock por usuário para evitar a criação concorrente de múltiplas cobranças.

A chave utilizada é:

```text
cobrancas:{idUsuario}
```

### Configuração

* **TTL:** 5 segundos
* **Escopo:** usuário
* **Liberação:** sempre executada no bloco `finally`

Caso o lock não esteja disponível, a operação deve retornar um erro de negócio:

```text
Geracao de cobranca em andamento.
```

A utilização do `finally` garante que o lock seja liberado mesmo quando ocorrer uma exceção durante o processamento.

---

## 2. Timezone oficial

Todas as operações relacionadas a datas de negócio utilizam:

```text
America/Sao_Paulo
```

Isso é especialmente importante para:

* criação da cobrança;
* expiração;
* finalização;
* processamento de webhook PIX;
* comparação de datas.

---

## 3. Criação de cobrança

Ao criar uma cobrança, o sistema:

1. Obtém o usuário autenticado através do `UserContext`.
2. Recupera:

    * `idUsuario`;
    * `givenName`;
    * `familyName`.
3. Monta o nome do solicitante:

```text
givenName familyName
```

4. Define o status inicial como:

```text
SOLICITADA
```

5. Utiliza os seguintes valores padrão quando não informados:

```text
metodo = PIX
tipo = RECARGA
```

6. Adquire o lock do usuário.
7. Seleciona a estratégia correspondente ao método de pagamento.
8. Persiste a cobrança.
9. Libera o lock no bloco `finally`.

### Estratégias

#### PIX

A estratégia PIX é responsável por preencher:

* `txid`;
* `copiaECola`;
* `dataExpiracao`.

#### Cartão de crédito

A estratégia de cartão é responsável por preencher:

* `transactionId`;
* `acsUrl`, quando disponível;
* `threeDsPayload`, quando disponível.

A utilização do **Strategy Pattern** permite adicionar novos métodos de pagamento sem concentrar todas as regras em uma única classe.

---

# 🔔 4. Webhook PIX

O endpoint de webhook recebe notificações relacionadas aos pagamentos PIX.

### Comportamento

Se o campo `pix` for:

* `null`;
* vazio;

o sistema simplesmente ignora a notificação e retorna:

```text
200 OK
```

Para cada item recebido:

1. Valida a existência do `txid`.
2. Busca a cobrança mais recente associada ao `txid`.
3. Caso a cobrança não exista, ignora a notificação.
4. Caso a cobrança já esteja `FINALIZADA`, ignora a notificação.
5. Caso a cobrança esteja pendente, cria uma nova versão.
6. Define o novo status como:

```text
FINALIZADA
```

7. Atualiza `valorPago`.
8. Define `dataFinalizada` utilizando o timezone:

```text
America/Sao_Paulo
```

O processamento é, portanto, preparado para receber notificações repetidas sem finalizar novamente uma cobrança que já foi processada.

---

# 🔎 5. Consulta da cobrança

Ao consultar uma cobrança por ID:

1. O sistema verifica se a cobrança existe.
2. Caso não exista, retorna `404 Not Found`.
3. Caso exista uma versão filha/reprocessada, retorna a versão mais recente.

Para cobranças PIX que estejam em um dos seguintes estados:

```text
SOLICITADA
EXPIRADA
ERRO_APROVACAO_PEDIDO
EM_REPROCESSAMENTO
ERRO_ANALISE_PENDENTE
```

o sistema pode consultar o status em um serviço externo.

Caso o status externo tenha sido alterado:

1. A cobrança atual é preservada.
2. Uma nova versão é criada.
3. A nova versão referencia a anterior.
4. O novo status é persistido.

Essa abordagem mantém o histórico das alterações da cobrança.

---

# 💳 6. Validação de cartão

Para validar um checkout de cartão:

1. O sistema busca a cobrança utilizando `transactionId`.
2. Caso não exista, retorna `404 Not Found`.
3. Chama o cliente externo de validação.
4. Atualiza os dados de autorização.
5. Persiste as alterações.
6. Retorna `200 OK`.

Os dados utilizados no processo incluem:

* `cavv`;
* `xid`;
* `eci`.

---

# 🌐 Endpoints

Base URL:

```text
/api/v1/cobrancas
```

---

## 1. Criar cobrança

### `POST /api/v1/cobrancas`

Cria uma nova cobrança.

### Request

```http
POST /api/v1/cobrancas
Content-Type: application/json
```

```json
{
  "valor": 25.50,
  "tipo": "RECARGA",
  "metodo": "PIX"
}
```

### Response

**201 Created**

```json
{
  "id": 123,
  "txid": "abc123",
  "copiaECola": "000201...",
  "dataExpiracao": "2026-04-15T12:00:00"
}
```

---

## 2. Criar cobrança utilizando valores padrão

Os campos `tipo` e `metodo` podem ser omitidos.

### Request

```http
POST /api/v1/cobrancas
Content-Type: application/json
```

```json
{
  "valor": 15.00
}
```

Nesse caso, o sistema utiliza:

```text
tipo   = RECARGA
metodo = PIX
```

---

## 3. Buscar cobrança

### `GET /api/v1/cobrancas/{id}`

Consulta uma cobrança pelo seu identificador.

### Request

```http
GET /api/v1/cobrancas/123
```

### Response

**200 OK**

```json
{
  "id": 123,
  "txid": "abc123",
  "idUsuario": "user-1",
  "tipo": "RECARGA",
  "metodo": "PIX",
  "status": "SOLICITADA",
  "valorSolicitacao": 25.50,
  "valorPago": null,
  "dataCriacao": "2026-04-15T10:00:00",
  "dataExpiracao": "2026-04-15T12:00:00",
  "dataFinalizada": null
}
```

### Cobrança inexistente

**404 Not Found**

---

# 4. Webhook PIX

### `POST /api/v1/cobrancas/webhook/pix`

Recebe notificações de pagamento PIX.

### Request

```http
POST /api/v1/cobrancas/webhook/pix
Content-Type: application/json
```

```json
{
  "pix": [
    {
      "txid": "abc123",
      "horario": "2026-04-15T13:02:30Z",
      "valor": 25.50
    }
  ]
}
```

### Response

**200 OK**

O endpoint retorna `200 OK` mesmo quando a notificação é ignorada, por exemplo, quando:

* `pix` é nulo;
* `pix` está vazio;
* o `txid` não possui cobrança correspondente;
* a cobrança já está finalizada.

---

# 5. Validar checkout de cartão

### `POST /api/v1/cobrancas/{transactionId}/validate`

Valida os dados do checkout de cartão de crédito.

### Request

```http
POST /api/v1/cobrancas/txn-001/validate
Content-Type: application/json
```

```json
{
  "cavv": "AAABBB",
  "xid": "XYZ",
  "eci": "05"
}
```

### Response

**200 OK**

---

# ❌ Tratamento de erros

## Regras de negócio

### Lock indisponível

Quando já existe uma operação de criação de cobrança para o usuário:

```text
Geracao de cobranca em andamento.
```

### Erro inesperado durante a criação

Caso ocorra uma exceção inesperada:

```text
Erro ao criar cobranca.
```

---

## Status HTTP

| HTTP Status       | Situação                                              |
| ----------------- | ----------------------------------------------------- |
| `200 OK`          | Consulta, webhook ou validação processada com sucesso |
| `201 Created`     | Cobrança criada com sucesso                           |
| `400 Bad Request` | Erro de regra de negócio                              |
| `404 Not Found`   | Cobrança não encontrada                               |

---

# 🔌 Integrações externas

Como o projeto é independente de serviços externos reais, as integrações são representadas por implementações mockadas.

Os principais clientes são:

### `PagamentoGatewayClient`

Simula a integração com o gateway responsável pelo processamento de pagamentos.

### `PixWebhookProvider`

Representa o provedor responsável pelo recebimento/processamento de eventos PIX.

### `CheckoutValidationClient`

Simula o serviço de validação de checkout de cartão.

### `StatusConsultaExternaClient`

Simula a consulta do status de uma cobrança PIX em um serviço externo.

Essa separação permite que as regras de negócio sejam desenvolvidas e testadas sem depender de serviços externos reais.

---

# 🧪 Testes

O projeto possui testes unitários e de integração utilizando **JUnit 5** e **Mockito**.

## Testes unitários obrigatórios

### Criação

* Criação de cobrança PIX com sucesso.
* Lock indisponível.
* Exceção inesperada deve ser convertida para erro de negócio.

### Webhook PIX

* Finalização de cobrança pendente.
* Ignorar cobrança já finalizada.

### Cartão

* Validação de checkout atualizando uma cobrança existente.

### Concorrência

* `LockExecutor` deve garantir o `unlock` dentro do bloco `finally`.

---

## Testes de integração

### Fluxo de criação e consulta

```text
POST /cobrancas
       │
       ▼
Criação da cobrança
       │
       ▼
GET /cobrancas/{id}
       │
       ▼
Consulta da cobrança criada
```

### Fluxo do webhook PIX

```text
POST /webhook/pix
       │
       ▼
Localização da cobrança
       │
       ▼
Criação da nova versão
       │
       ▼
Status = FINALIZADA
```

---

# 🚀 Como executar

## Pré-requisitos

Certifique-se de possuir instalado:

* JDK 17 ou superior
* Maven 3.9 ou superior

---

## Executar a aplicação

Utilizando o Maven Wrapper:

```bash
./mvnw spring-boot:run
```

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

---

## Executar os testes

Linux/macOS:

```bash
./mvnw test
```

Windows:

```powershell
.\mvnw.cmd test
```

---

# 🗄️ H2 Console

Durante a execução da aplicação, o console do H2 pode ser acessado em:

```text
http://localhost:8080/h2-console
```

### Credenciais

| Configuração | Valor                |
| ------------ | -------------------- |
| JDBC URL     | `jdbc:h2:mem:testdb` |
| Usuário      | `sa`                 |
| Senha        | *(vazio)*            |

---

# 🏗️ Arquitetura

A aplicação utiliza uma arquitetura em camadas, separando responsabilidades entre:

```text
Controller
    │
    ▼
Service
    │
    ├── Strategy
    │
    ├── Integration
    │
    └── Lock
    │
    ▼
Repository
    │
    ▼
Database
```

### Responsabilidades

**Controller**

Responsável por:

* receber requisições HTTP;
* validar requests;
* retornar respostas HTTP.

**Service**

Responsável pelas:

* regras de negócio;
* orquestração do fluxo;
* controle das transações.

**Strategy**

Responsável por encapsular as regras específicas de cada método de pagamento.

**Repository**

Responsável pelo acesso e persistência dos dados.

**Integration**

Responsável pela comunicação com serviços externos.

**Lock**

Responsável pelo controle de concorrência entre operações do mesmo usuário.

**Mapper**

Responsável pela conversão entre entidades e DTOs.

---

# 🔄 Versionamento de cobranças

O projeto suporta versionamento de cobranças para preservar o histórico de alterações.

Uma cobrança pode possuir uma versão anterior relacionada a ela:

```text
Cobrança original
       │
       ▼
Reprocessamento
       │
       ▼
Nova versão
       │
       ▼
Status atualizado
```

Essa abordagem evita sobrescrever completamente o estado anterior e permite manter uma relação entre as diferentes versões da cobrança.

---

# 🎯 Objetivos técnicos

O projeto foi estruturado para demonstrar conhecimentos relacionados a:

* Desenvolvimento backend com Java 17;
* Spring Boot;
* APIs REST;
* Arquitetura em camadas;
* Separação de responsabilidades;
* Design Patterns;
* Strategy Pattern;
* Persistência com JPA/Hibernate;
* Controle de concorrência;
* Locks distribuídos;
* Processamento de webhooks;
* Idempotência no processamento de eventos;
* Versionamento/reprocessamento de entidades;
* Integração com serviços externos;
* Tratamento de exceções;
* Testes unitários;
* Testes de integração;
* Cobertura de código.

---

# 📌 Observações

Este projeto foi desenvolvido com o objetivo de ser uma implementação **simples, isolada e extensível** de um fluxo de cobranças.

As integrações externas são simuladas para permitir a execução local e facilitar os testes automatizados.

Em um ambiente produtivo, componentes como:

* gateway de pagamento;
* provedor PIX;
* mecanismo de lock distribuído;
* observabilidade;
* autenticação/autorização;
* banco de dados;
* mensageria;

poderiam ser substituídos ou complementados por implementações específicas da infraestrutura da aplicação.

---

# 📄 Licença

Este projeto é destinado a fins **educacionais e técnicos**, especialmente para avaliação em processos seletivos.
